package com.campustrade.evaluation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EvaluationView(
        long id,
        long orderId,
        long evaluatorId,
        String evaluatorName,
        long targetId,
        String targetName,
        int score,
        String content,
        List<String> tags,
        BigDecimal targetCreditScore,
        String targetCreditLevel,
        LocalDateTime createdAt
) {
}
