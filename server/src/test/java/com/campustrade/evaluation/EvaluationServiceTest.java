package com.campustrade.evaluation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationServiceTest {

    @Test
    void creditLevelCoversBoundaryValues() {
        assertThat(EvaluationService.creditLevel(new BigDecimal("0.00"))).isEqualTo("一般");
        assertThat(EvaluationService.creditLevel(new BigDecimal("69.99"))).isEqualTo("一般");
        assertThat(EvaluationService.creditLevel(new BigDecimal("70.00"))).isEqualTo("良好");
        assertThat(EvaluationService.creditLevel(new BigDecimal("89.99"))).isEqualTo("良好");
        assertThat(EvaluationService.creditLevel(new BigDecimal("90.00"))).isEqualTo("优秀");
        assertThat(EvaluationService.creditLevel(new BigDecimal("100.00"))).isEqualTo("优秀");
    }
}
