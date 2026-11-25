package com.campustrade.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "消息不能为空")
        @Size(max = 500, message = "消息不能超过 500 个字符")
        String content
) {
}
