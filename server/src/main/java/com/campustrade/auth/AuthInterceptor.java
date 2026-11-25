package com.campustrade.auth;

import com.campustrade.common.ApiException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_COOKIE = "CT_SESSION";

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        readToken(request).flatMap(tokenService::verify)
                .ifPresent(userId -> request.setAttribute(CurrentUser.REQUEST_ATTRIBUTE, userId));

        if (handler instanceof HandlerMethod method) {
            boolean loginRequired = method.hasMethodAnnotation(LoginRequired.class)
                    || method.getBeanType().isAnnotationPresent(LoginRequired.class);
            if (loginRequired && request.getAttribute(CurrentUser.REQUEST_ATTRIBUTE) == null) {
                throw ApiException.unauthorized();
            }
        }
        return true;
    }

    private java.util.Optional<String> readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return java.util.Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE.equals(cookie.getName())) {
                return java.util.Optional.ofNullable(cookie.getValue());
            }
        }
        return java.util.Optional.empty();
    }
}
