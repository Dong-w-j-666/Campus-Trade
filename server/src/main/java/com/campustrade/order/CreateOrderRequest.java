package com.campustrade.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull(message = "请选择商品") Long goodsId,
        @Size(max = 255, message = "备注不能超过 255 个字符") String remark
) {
}
