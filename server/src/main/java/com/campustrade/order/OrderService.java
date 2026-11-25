package com.campustrade.order;

import com.campustrade.common.ApiException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OrderService {

    private static final DateTimeFormatter NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String ORDER_SELECT = """
            SELECT o.order_id, o.order_no, o.goods_id, o.buyer_id, o.amount, o.status,
                   o.trade_campus, o.remark, o.created_at, o.paid_at, o.finished_at,
                   g.title AS goods_title, g.seller_id,
                   (SELECT gi.image_url
                    FROM t_goods_image gi
                    WHERE gi.goods_id = g.goods_id
                    ORDER BY gi.sort_no
                    LIMIT 1) AS goods_image,
                   buyer.real_name AS buyer_name,
                   seller.real_name AS seller_name,
                   p.refund_status, p.settlement_status
            FROM t_order o
            JOIN t_goods g ON g.goods_id = o.goods_id
            JOIN t_user buyer ON buyer.user_id = o.buyer_id
            JOIN t_user seller ON seller.user_id = g.seller_id
            LEFT JOIN t_payment p ON p.order_id = o.order_id
            """;

    private static final RowMapper<OrderView> ORDER_MAPPER = (resultSet, rowNum) -> new OrderView(
            resultSet.getLong("order_id"),
            resultSet.getString("order_no"),
            resultSet.getLong("goods_id"),
            resultSet.getString("goods_title"),
            resultSet.getString("goods_image"),
            resultSet.getLong("buyer_id"),
            resultSet.getString("buyer_name"),
            resultSet.getLong("seller_id"),
            resultSet.getString("seller_name"),
            resultSet.getBigDecimal("amount"),
            resultSet.getString("status"),
            resultSet.getString("trade_campus"),
            resultSet.getString("remark"),
            resultSet.getTimestamp("created_at").toLocalDateTime(),
            toLocalDateTime(resultSet.getTimestamp("paid_at")),
            toLocalDateTime(resultSet.getTimestamp("finished_at")),
            resultSet.getString("refund_status"),
            resultSet.getString("settlement_status")
    );

    private final JdbcTemplate jdbcTemplate;

    public OrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public OrderView create(long buyerId, CreateOrderRequest request) {
        OrderGoods goods;
        try {
            goods = jdbcTemplate.queryForObject(
                    """
                    SELECT goods_id, seller_id, price, status, campus
                    FROM t_goods
                    WHERE goods_id = ?
                    FOR UPDATE
                    """,
                    (resultSet, rowNum) -> new OrderGoods(
                            resultSet.getLong("goods_id"),
                            resultSet.getLong("seller_id"),
                            resultSet.getBigDecimal("price"),
                            resultSet.getString("status"),
                            resultSet.getString("campus")
                    ),
                    request.goodsId()
            );
        } catch (EmptyResultDataAccessException exception) {
            throw ApiException.notFound("商品不存在");
        }

        if (goods.sellerId() == buyerId) {
            throw ApiException.badRequest("不能购买自己发布的商品");
        }
        if (!"ON_SALE".equals(goods.status())) {
            throw ApiException.conflict("商品已被预订、售出或下架");
        }

        String orderNo = nextNumber("CT");
        jdbcTemplate.update(
                """
                INSERT INTO t_order
                  (order_no, goods_id, buyer_id, amount, status, trade_campus, remark)
                VALUES (?, ?, ?, ?, 'UNPAID', ?, ?)
                """,
                orderNo,
                goods.id(),
                buyerId,
                goods.price(),
                goods.campus(),
                trimToNull(request.remark())
        );
        Long orderId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (orderId == null) {
            throw new IllegalStateException("Unable to read generated order id");
        }
        jdbcTemplate.update(
                "UPDATE t_goods SET status = 'LOCKED' WHERE goods_id = ? AND status = 'ON_SALE'",
                goods.id()
        );
        return findAccessible(orderId, buyerId);
    }

    public List<OrderView> list(long userId, String role) {
        String normalizedRole = role == null ? "buyer" : role.toLowerCase();
        String condition = switch (normalizedRole) {
            case "seller" -> " WHERE g.seller_id = ?";
            case "buyer" -> " WHERE o.buyer_id = ?";
            default -> throw ApiException.badRequest("订单角色只能是 buyer 或 seller");
        };
        return jdbcTemplate.query(
                ORDER_SELECT + condition + " ORDER BY o.created_at DESC, o.order_id DESC",
                ORDER_MAPPER,
                userId
        );
    }

    public OrderView findAccessible(long orderId, long userId) {
        OrderView order = findById(orderId);
        if (order.buyerId() != userId && order.sellerId() != userId) {
            throw ApiException.forbidden("无权查看该订单");
        }
        return order;
    }

    @Transactional
    public OrderView pay(long orderId, long buyerId) {
        OrderState state = lockState(orderId);
        if (state.buyerId() != buyerId) {
            throw ApiException.forbidden("只有买家可以支付该订单");
        }
        if (!"UNPAID".equals(state.status())) {
            throw ApiException.conflict("该订单当前不能支付");
        }

        jdbcTemplate.update(
                """
                INSERT INTO t_payment
                  (trade_no, order_id, amount, pay_method, status, refund_amount, refund_status,
                   settlement_amount, settlement_status, paid_at)
                VALUES (?, ?, ?, 'SANDBOX', 'SUCCESS', 0.00, 'NONE', 0.00, 'PENDING', NOW())
                """,
                nextNumber("SB"),
                orderId,
                state.amount()
        );
        jdbcTemplate.update(
                "UPDATE t_order SET status = 'PAID', paid_at = NOW() WHERE order_id = ?",
                orderId
        );
        return findById(orderId);
    }

    @Transactional
    public OrderView cancel(long orderId, long buyerId) {
        OrderState state = lockState(orderId);
        if (state.buyerId() != buyerId) {
            throw ApiException.forbidden("只有买家可以取消订单");
        }
        if (!"UNPAID".equals(state.status())) {
            throw ApiException.conflict("只有待付款订单可以取消");
        }
        jdbcTemplate.update("UPDATE t_order SET status = 'CANCELLED' WHERE order_id = ?", orderId);
        jdbcTemplate.update(
                "UPDATE t_goods SET status = 'ON_SALE' WHERE goods_id = ? AND status = 'LOCKED'",
                state.goodsId()
        );
        return findById(orderId);
    }

    @Transactional
    public OrderView refund(long orderId, long buyerId) {
        OrderState state = lockState(orderId);
        if (state.buyerId() != buyerId) {
            throw ApiException.forbidden("只有买家可以申请退款");
        }
        if (!"PAID".equals(state.status())) {
            throw ApiException.conflict("只有已付款且未完成的订单可以退款");
        }

        jdbcTemplate.update(
                """
                UPDATE t_payment
                SET status = 'REFUNDED', refund_amount = amount, refund_status = 'REFUNDED',
                    settlement_amount = 0.00, settlement_status = 'PENDING', refunded_at = NOW()
                WHERE order_id = ?
                """,
                orderId
        );
        jdbcTemplate.update("UPDATE t_order SET status = 'CANCELLED' WHERE order_id = ?", orderId);
        jdbcTemplate.update(
                "UPDATE t_goods SET status = 'ON_SALE' WHERE goods_id = ? AND status = 'LOCKED'",
                state.goodsId()
        );
        return findById(orderId);
    }

    @Transactional
    public OrderView complete(long orderId, long buyerId) {
        OrderState state = lockState(orderId);
        if (state.buyerId() != buyerId) {
            throw ApiException.forbidden("只有买家可以确认收货");
        }
        if (!"PAID".equals(state.status())) {
            throw ApiException.conflict("该订单当前不能确认收货");
        }
        BigDecimal settlement = state.amount()
                .multiply(new BigDecimal("0.98"))
                .setScale(2, RoundingMode.HALF_UP);
        jdbcTemplate.update(
                """
                UPDATE t_order
                SET status = 'FINISHED', finished_at = NOW()
                WHERE order_id = ?
                """,
                orderId
        );
        jdbcTemplate.update(
                """
                UPDATE t_payment
                SET settlement_amount = ?, settlement_status = 'SETTLED', settled_at = NOW()
                WHERE order_id = ? AND status = 'SUCCESS'
                """,
                settlement,
                orderId
        );
        jdbcTemplate.update(
                "UPDATE t_goods SET status = 'SOLD' WHERE goods_id = ?",
                state.goodsId()
        );
        return findById(orderId);
    }

    private OrderView findById(long orderId) {
        try {
            return jdbcTemplate.queryForObject(
                    ORDER_SELECT + " WHERE o.order_id = ?",
                    ORDER_MAPPER,
                    orderId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw ApiException.notFound("订单不存在");
        }
    }

    private OrderState lockState(long orderId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT o.order_id, o.goods_id, o.buyer_id, o.amount, o.status
                    FROM t_order o
                    WHERE o.order_id = ?
                    FOR UPDATE
                    """,
                    (resultSet, rowNum) -> new OrderState(
                            resultSet.getLong("order_id"),
                            resultSet.getLong("goods_id"),
                            resultSet.getLong("buyer_id"),
                            resultSet.getBigDecimal("amount"),
                            resultSet.getString("status")
                    ),
                    orderId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw ApiException.notFound("订单不存在");
        }
    }

    private String nextNumber(String prefix) {
        return prefix
                + LocalDateTime.now().format(NUMBER_TIME)
                + String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record OrderGoods(
            long id,
            long sellerId,
            BigDecimal price,
            String status,
            String campus
    ) {
    }

    private record OrderState(
            long id,
            long goodsId,
            long buyerId,
            BigDecimal amount,
            String status
    ) {
    }
}
