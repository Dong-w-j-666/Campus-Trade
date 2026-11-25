package com.campustrade.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GoodsDetail(
        long id,
        String title,
        String description,
        BigDecimal price,
        String conditionLevel,
        long categoryId,
        String categoryName,
        String campus,
        String status,
        int viewCount,
        List<String> images,
        SellerView seller,
        LocalDateTime createdAt
) {
    public record SellerView(
            long id,
            String name,
            BigDecimal creditScore,
            String campus,
            String avatarUrl
    ) {
    }
}
