package com.campustrade.evaluation;

import com.campustrade.common.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class EvaluationService {

    private static final Set<String> ALLOWED_TAGS = Set.of(
            "描述相符",
            "交易爽快",
            "守约",
            "沟通顺畅",
            "礼貌友好",
            "打包仔细",
            "响应及时"
    );

    private static final String EVALUATION_SELECT = """
            SELECT e.evaluation_id, e.order_id, e.evaluator_id, e.target_id,
                   e.score, e.content, e.created_at,
                   evaluator.real_name AS evaluator_name,
                   target.real_name AS target_name,
                   target.credit_score AS target_credit_score,
                   COALESCE(
                     (SELECT GROUP_CONCAT(et.tag_name ORDER BY et.tag_name SEPARATOR ',')
                      FROM t_evaluation_tag et
                      WHERE et.evaluation_id = e.evaluation_id),
                     ''
                   ) AS tag_names
            FROM t_evaluation e
            JOIN t_user evaluator ON evaluator.user_id = e.evaluator_id
            JOIN t_user target ON target.user_id = e.target_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public EvaluationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public EvaluationView submit(long evaluatorId, EvaluationRequest request) {
        List<String> tags = normalizeTags(request.tags());
        EvaluationTarget order = lockEvaluationTarget(request.orderId());

        if (!"FINISHED".equals(order.status())) {
            throw ApiException.conflict("只有已完成的订单才能评价");
        }

        long targetId;
        if (order.buyerId() == evaluatorId) {
            targetId = order.sellerId();
        } else if (order.sellerId() == evaluatorId) {
            targetId = order.buyerId();
        } else {
            throw ApiException.forbidden("你不是该订单的交易方");
        }

        Integer duplicateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_evaluation WHERE order_id = ? AND evaluator_id = ?",
                Integer.class,
                order.orderId(),
                evaluatorId
        );
        if (duplicateCount != null && duplicateCount > 0) {
            throw ApiException.conflict("这笔订单已经评价过了");
        }

        long evaluationId;
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO t_evaluation
                      (order_id, evaluator_id, target_id, score, content)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    order.orderId(),
                    evaluatorId,
                    targetId,
                    request.score(),
                    trimToNull(request.content())
            );
            Long generatedId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            if (generatedId == null) {
                throw new IllegalStateException("Unable to read generated evaluation id");
            }
            evaluationId = generatedId;
        } catch (DuplicateKeyException exception) {
            throw ApiException.conflict("这笔订单已经评价过了");
        }

        for (String tag : tags) {
            jdbcTemplate.update(
                    "INSERT INTO t_evaluation_tag (evaluation_id, tag_name) VALUES (?, ?)",
                    evaluationId,
                    tag
            );
        }

        int creditDelta = (request.score() - 3) * 5;
        jdbcTemplate.update(
                """
                UPDATE t_user
                SET credit_score = GREATEST(0.00, LEAST(100.00, credit_score + ?))
                WHERE user_id = ?
                """,
                BigDecimal.valueOf(creditDelta),
                targetId
        );
        return findEvaluation(evaluationId);
    }

    public List<EvaluationView> listForOrder(long orderId, long requesterId) {
        OrderParticipants order = findParticipants(orderId);
        if (order.buyerId() != requesterId && order.sellerId() != requesterId) {
            throw ApiException.forbidden("无权查看该订单的评价");
        }
        return findEvaluations(" WHERE e.order_id = ? ORDER BY e.created_at, e.evaluation_id", orderId);
    }

    public ReputationView reputation(long userId) {
        UserReputation summary;
        try {
            summary = jdbcTemplate.queryForObject(
                    """
                    SELECT u.user_id, u.real_name, u.campus, u.credit_score,
                           COALESCE(AVG(e.score), 0) AS average_score,
                           COUNT(e.evaluation_id) AS evaluation_count
                    FROM t_user u
                    LEFT JOIN t_evaluation e ON e.target_id = u.user_id
                    WHERE u.user_id = ?
                    GROUP BY u.user_id, u.real_name, u.campus, u.credit_score
                    """,
                    (resultSet, rowNum) -> new UserReputation(
                            resultSet.getLong("user_id"),
                            resultSet.getString("real_name"),
                            resultSet.getString("campus"),
                            resultSet.getBigDecimal("credit_score"),
                            resultSet.getBigDecimal("average_score").setScale(1, RoundingMode.HALF_UP),
                            resultSet.getLong("evaluation_count")
                    ),
                    userId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw ApiException.notFound("用户不存在");
        }

        List<EvaluationView> recent = findEvaluations(
                " WHERE e.target_id = ? ORDER BY e.created_at DESC, e.evaluation_id DESC LIMIT 20",
                userId
        );
        return new ReputationView(
                summary.userId(),
                summary.name(),
                summary.campus(),
                summary.creditScore(),
                creditLevel(summary.creditScore()),
                summary.averageScore(),
                summary.evaluationCount(),
                recent
        );
    }

    static String creditLevel(BigDecimal creditScore) {
        if (creditScore.compareTo(new BigDecimal("90")) >= 0) {
            return "优秀";
        }
        if (creditScore.compareTo(new BigDecimal("70")) >= 0) {
            return "良好";
        }
        return "一般";
    }

    private EvaluationTarget lockEvaluationTarget(long orderId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT o.order_id, o.buyer_id, g.seller_id, o.status
                    FROM t_order o
                    JOIN t_goods g ON g.goods_id = o.goods_id
                    WHERE o.order_id = ?
                    FOR UPDATE
                    """,
                    (resultSet, rowNum) -> new EvaluationTarget(
                            resultSet.getLong("order_id"),
                            resultSet.getLong("buyer_id"),
                            resultSet.getLong("seller_id"),
                            resultSet.getString("status")
                    ),
                    orderId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw ApiException.notFound("订单不存在");
        }
    }

    private OrderParticipants findParticipants(long orderId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT o.order_id, o.buyer_id, g.seller_id
                    FROM t_order o
                    JOIN t_goods g ON g.goods_id = o.goods_id
                    WHERE o.order_id = ?
                    """,
                    (resultSet, rowNum) -> new OrderParticipants(
                            resultSet.getLong("order_id"),
                            resultSet.getLong("buyer_id"),
                            resultSet.getLong("seller_id")
                    ),
                    orderId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw ApiException.notFound("订单不存在");
        }
    }

    private EvaluationView findEvaluation(long evaluationId) {
        List<EvaluationView> evaluations = findEvaluations(
                " WHERE e.evaluation_id = ?",
                evaluationId
        );
        if (evaluations.isEmpty()) {
            throw ApiException.notFound("评价记录不存在");
        }
        return evaluations.getFirst();
    }

    private List<EvaluationView> findEvaluations(String condition, Object... parameters) {
        return jdbcTemplate.query(
                EVALUATION_SELECT + condition,
                (resultSet, rowNum) -> new EvaluationView(
                        resultSet.getLong("evaluation_id"),
                        resultSet.getLong("order_id"),
                        resultSet.getLong("evaluator_id"),
                        resultSet.getString("evaluator_name"),
                        resultSet.getLong("target_id"),
                        resultSet.getString("target_name"),
                        resultSet.getInt("score"),
                        resultSet.getString("content"),
                        parseTags(resultSet.getString("tag_names")),
                        resultSet.getBigDecimal("target_credit_score"),
                        creditLevel(resultSet.getBigDecimal("target_credit_score")),
                        resultSet.getTimestamp("created_at").toLocalDateTime()
                ),
                parameters
        );
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            String value = tag.trim();
            if (!ALLOWED_TAGS.contains(value)) {
                throw ApiException.badRequest("包含不支持的评价标签");
            }
            normalized.add(value);
        }
        return List.copyOf(normalized);
    }

    private List<String> parseTags(String values) {
        if (values == null || values.isBlank()) {
            return List.of();
        }
        return Arrays.stream(values.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record EvaluationTarget(
            long orderId,
            long buyerId,
            long sellerId,
            String status
    ) {
    }

    private record OrderParticipants(long orderId, long buyerId, long sellerId) {
    }

    private record UserReputation(
            long userId,
            String name,
            String campus,
            BigDecimal creditScore,
            BigDecimal averageScore,
            long evaluationCount
    ) {
    }
}
