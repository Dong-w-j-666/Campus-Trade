package com.campustrade.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderView(
        long id,
        String orderNo,
        long goodsId,
        String goodsTitle,
        String goodsImage,
        long buyerId,
        String buyerName,
        long sellerId,
        String sellerName,
        BigDecimal amount,
        String status,
        String tradeCampus,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime finishedAt,
        String refundStatus,
        String settlementStatus,
        boolean buyerEvaluated,
        boolean sellerEvaluated
) {
}
