package com.campustrade.message;

import com.campustrade.common.ApiException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class MessageService {

    private static final Pattern MAINLAND_MOBILE =
            Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)");

    private static final String MESSAGE_SELECT = """
            SELECT m.message_id, m.order_id, m.sender_id, sender.real_name AS sender_name,
                   m.receiver_id, receiver.real_name AS receiver_name, m.msg_type,
                   m.content, m.is_read, m.created_at
            FROM t_message m
            JOIN t_user sender ON sender.user_id = m.sender_id
            JOIN t_user receiver ON receiver.user_id = m.receiver_id
            """;

    private final RowMapper<MessageView> messageMapper = (resultSet, rowNum) -> new MessageView(
            resultSet.getLong("message_id"),
            resultSet.getObject("order_id", Long.class),
            resultSet.getLong("sender_id"),
            resultSet.getString("sender_name"),
            resultSet.getLong("receiver_id"),
            resultSet.getString("receiver_name"),
            resultSet.getString("msg_type"),
            resultSet.getString("content"),
            resultSet.getBoolean("is_read"),
            resultSet.getTimestamp("created_at").toLocalDateTime(),
            false
    );

    private final JdbcTemplate jdbcTemplate;

    public MessageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ConversationView> conversations(long userId) {
        return jdbcTemplate.query(
                """
                SELECT o.order_id, o.order_no, o.status,
                       g.goods_id, g.title AS goods_title,
                       (SELECT gi.image_url
                        FROM t_goods_image gi
                        WHERE gi.goods_id = g.goods_id
                        ORDER BY gi.sort_no
                        LIMIT 1) AS goods_image,
                       CASE WHEN o.buyer_id = ? THEN seller.user_id ELSE buyer.user_id END
                         AS counterpart_id,
                       CASE WHEN o.buyer_id = ? THEN seller.real_name ELSE buyer.real_name END
                         AS counterpart_name,
                       last_message.content AS last_message,
                       last_message.sender_id AS last_sender_id,
                       last_message.created_at AS last_message_at,
                       (SELECT COUNT(*)
                        FROM t_message unread_message
                        WHERE unread_message.order_id = o.order_id
                          AND unread_message.receiver_id = ?
                          AND unread_message.is_read = 0) AS unread_count
                FROM t_order o
                JOIN t_goods g ON g.goods_id = o.goods_id
                JOIN t_user buyer ON buyer.user_id = o.buyer_id
                JOIN t_user seller ON seller.user_id = g.seller_id
                LEFT JOIN t_message last_message
                  ON last_message.message_id = (
                    SELECT MAX(recent_message.message_id)
                    FROM t_message recent_message
                    WHERE recent_message.order_id = o.order_id
                  )
                WHERE o.buyer_id = ? OR seller.user_id = ?
                ORDER BY COALESCE(last_message.created_at, o.created_at) DESC, o.order_id DESC
                """,
                (resultSet, rowNum) -> new ConversationView(
                        resultSet.getLong("order_id"),
                        resultSet.getString("order_no"),
                        resultSet.getLong("goods_id"),
                        resultSet.getString("goods_title"),
                        resultSet.getString("goods_image"),
                        resultSet.getLong("counterpart_id"),
                        resultSet.getString("counterpart_name"),
                        resultSet.getString("status"),
                        resultSet.getString("last_message"),
                        resultSet.getObject("last_sender_id", Long.class),
                        resultSet.getTimestamp("last_message_at") == null
                                ? null
                                : resultSet.getTimestamp("last_message_at").toLocalDateTime(),
                        resultSet.getLong("unread_count")
                ),
                userId,
                userId,
                userId,
                userId,
                userId
        );
    }

    public List<MessageView> listForOrder(long orderId, long userId) {
        requireParticipant(orderId, userId);
        List<MessageView> messages = new ArrayList<>(jdbcTemplate.query(
                MESSAGE_SELECT + """
                 WHERE m.order_id = ?
                 ORDER BY m.message_id DESC
                 LIMIT 200
                 """,
                messageMapper,
                orderId
        ));
        Collections.reverse(messages);
        return messages.stream()
                .map(message -> withMine(message, userId))
                .toList();
    }

    @Transactional
    public MessageView send(long orderId, long userId, SendMessageRequest request) {
        OrderParticipant participant = requireParticipant(orderId, userId);
        long receiverId = participant.buyerId() == userId
                ? participant.sellerId()
                : participant.buyerId();
        String content = maskPhoneNumbers(normalizeContent(request.content()));
        if (content.isBlank()) {
            throw ApiException.badRequest("消息不能为空");
        }

        jdbcTemplate.update(
                """
                INSERT INTO t_message
                  (sender_id, receiver_id, order_id, msg_type, content, is_read)
                VALUES (?, ?, ?, 'CHAT', ?, 0)
                """,
                userId,
                receiverId,
                orderId,
                content
        );
        Long messageId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (messageId == null) {
            throw new IllegalStateException("Unable to read generated message id");
        }
        MessageView message = findMessage(messageId);
        return withMine(message, userId);
    }

    @Transactional
    public void markOrderRead(long orderId, long userId) {
        requireParticipant(orderId, userId);
        jdbcTemplate.update(
                """
                UPDATE t_message
                SET is_read = 1
                WHERE order_id = ? AND receiver_id = ? AND is_read = 0
                """,
                orderId,
                userId
        );
    }

    public List<MessageView> notifications(long userId) {
        List<MessageView> messages = jdbcTemplate.query(
                MESSAGE_SELECT + """
                 WHERE m.receiver_id = ? AND m.msg_type = 'SYSTEM'
                 ORDER BY m.message_id DESC
                 LIMIT 100
                 """,
                messageMapper,
                userId
        );
        return messages.stream()
                .map(message -> withMine(message, userId))
                .toList();
    }

    @Transactional
    public void markNotificationsRead(long userId) {
        jdbcTemplate.update(
                """
                UPDATE t_message
                SET is_read = 1
                WHERE receiver_id = ? AND msg_type = 'SYSTEM' AND is_read = 0
                """,
                userId
        );
    }

    public UnreadCountView unreadCount(long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_message WHERE receiver_id = ? AND is_read = 0",
                Long.class,
                userId
        );
        return new UnreadCountView(count == null ? 0 : count);
    }

    private MessageView findMessage(long messageId) {
        try {
            return jdbcTemplate.queryForObject(
                    MESSAGE_SELECT + " WHERE m.message_id = ?",
                    messageMapper,
                    messageId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw ApiException.notFound("消息不存在");
        }
    }

    private OrderParticipant requireParticipant(long orderId, long userId) {
        OrderParticipant participant;
        try {
            participant = jdbcTemplate.queryForObject(
                    """
                    SELECT o.order_id, o.buyer_id, g.seller_id
                    FROM t_order o
                    JOIN t_goods g ON g.goods_id = o.goods_id
                    WHERE o.order_id = ?
                    """,
                    (resultSet, rowNum) -> new OrderParticipant(
                            resultSet.getLong("order_id"),
                            resultSet.getLong("buyer_id"),
                            resultSet.getLong("seller_id")
                    ),
                    orderId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw ApiException.notFound("订单不存在");
        }
        if (participant.buyerId() != userId && participant.sellerId() != userId) {
            throw ApiException.forbidden("只有订单买卖双方可以查看或发送消息");
        }
        return participant;
    }

    private MessageView withMine(MessageView message, long userId) {
        return new MessageView(
                message.id(),
                message.orderId(),
                message.senderId(),
                message.senderName(),
                message.receiverId(),
                message.receiverName(),
                message.type(),
                message.content(),
                message.read(),
                message.createdAt(),
                message.senderId() == userId
        );
    }

    static String normalizeContent(String content) {
        return content == null
                ? ""
                : content.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    static String maskPhoneNumbers(String content) {
        return MAINLAND_MOBILE.matcher(content).replaceAll(match ->
                match.group().substring(0, 3) + "****" + match.group().substring(7)
        );
    }

    private record OrderParticipant(long orderId, long buyerId, long sellerId) {
    }
}
