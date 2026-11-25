package com.campustrade.auth;

import java.math.BigDecimal;

public record UserView(
        long id,
        String studentNo,
        String realName,
        String phone,
        BigDecimal creditScore,
        String roleCode,
        String campus,
        String avatarUrl
) {
}
