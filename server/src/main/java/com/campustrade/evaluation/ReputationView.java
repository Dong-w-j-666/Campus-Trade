package com.campustrade.evaluation;

import java.math.BigDecimal;
import java.util.List;

public record ReputationView(
        long userId,
        String name,
        String campus,
        BigDecimal creditScore,
        String creditLevel,
        BigDecimal averageScore,
        long evaluationCount,
        List<EvaluationView> recentEvaluations
) {
}
