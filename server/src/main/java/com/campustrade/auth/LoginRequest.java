package com.campustrade.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "请输入学号或手机号") String account,
        @NotBlank(message = "请输入密码") String password
) {
}
