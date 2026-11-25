package com.campustrade.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PublishGoodsRequest(
        @NotBlank(message = "请输入商品标题")
        @Size(max = 60, message = "标题不能超过 60 个字符")
        String title,

        @Size(max = 5000, message = "商品描述不能超过 5000 个字符")
        String description,

        @NotNull(message = "请输入价格")
        @DecimalMin(value = "0.01", message = "价格必须大于 0")
        @Digits(integer = 8, fraction = 2, message = "价格最多保留两位小数")
        BigDecimal price,

        @NotBlank(message = "请选择成色")
        @Size(max = 20, message = "成色名称过长")
        String conditionLevel,

        @NotNull(message = "请选择分类")
        Long categoryId,

        @NotBlank(message = "请选择交易校区")
        @Size(max = 50, message = "校区名称过长")
        String campus,

        @Size(max = 9, message = "最多添加 9 张图片")
        List<@Size(max = 255, message = "图片地址过长") String> imageUrls
) {
}
