package com.campustrade.catalog;

public record CategoryView(
        long id,
        Long parentId,
        String name,
        int sortNo
) {
}
