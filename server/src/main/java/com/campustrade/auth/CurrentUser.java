package com.campustrade.auth;

import com.campustrade.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;

public final class CurrentUser {

    public static final String REQUEST_ATTRIBUTE = CurrentUser.class.getName();

    private CurrentUser() {
    }

    public static Long id(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ATTRIBUTE);
        if (value instanceof Long id) {
            return id;
        }
        throw ApiException.unauthorized();
    }
}
