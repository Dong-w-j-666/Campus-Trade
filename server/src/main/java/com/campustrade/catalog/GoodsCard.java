package com.campustrade.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GoodsCard(
        long id,
        String title,
        BigDecimal price,
        String conditionLevel,
        String campus,
        String status,
        int viewCount,
        String imageUrl,
        long sellerId,
        String sellerName,
        String categoryName,
        LocalDateTime createdAt
) {
}
