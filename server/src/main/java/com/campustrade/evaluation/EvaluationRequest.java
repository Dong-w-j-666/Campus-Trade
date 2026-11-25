package com.campustrade.evaluation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EvaluationRequest(
        @NotNull(message = "请选择需要评价的订单")
        Long orderId,

        @NotNull(message = "请选择评分")
        @Min(value = 1, message = "评分不能低于 1 星")
        @Max(value = 5, message = "评分不能高于 5 星")
        Integer score,

        @Size(max = 500, message = "评价内容不能超过 500 字")
        String content,

        @Size(max = 5, message = "最多选择 5 个评价标签")
        List<@Valid @Size(max = 30, message = "单个评价标签不能超过 30 字") String> tags
) {
}
