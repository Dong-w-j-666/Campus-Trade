package com.campustrade.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "请输入学号")
        @Pattern(regexp = "\\d{8}", message = "学号应为 8 位数字")
        String studentNo,

        @NotBlank(message = "请输入姓名")
        @Size(max = 50, message = "姓名不能超过 50 个字符")
        String realName,

        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "1\\d{10}", message = "请输入 11 位手机号")
        String phone,

        @NotBlank(message = "请输入密码")
        @Size(min = 8, max = 32, message = "密码长度应为 8 至 32 位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
        String password,

        @NotBlank(message = "请选择校区")
        @Size(max = 50, message = "校区名称过长")
        String campus
) {
}
