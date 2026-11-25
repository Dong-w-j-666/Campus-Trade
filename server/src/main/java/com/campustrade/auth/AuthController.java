package com.campustrade.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final boolean secureCookie;
    private final long tokenHours;

    public AuthController(
            AuthService authService,
            TokenService tokenService,
            @Value("${app.auth.secure-cookie:false}") boolean secureCookie,
            @Value("${app.auth.token-hours:168}") long tokenHours
    ) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.secureCookie = secureCookie;
        this.tokenHours = tokenHours;
    }

    @PostMapping("/register")
    public ResponseEntity<UserView> register(@Valid @RequestBody RegisterRequest request) {
        UserView user = authService.register(request);
        return withSession(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserView> login(@Valid @RequestBody LoginRequest request) {
        UserView user = authService.authenticate(request);
        return withSession(user);
    }

    @GetMapping("/me")
    public ResponseEntity<UserView> me(HttpServletRequest request) {
        Object userId = request.getAttribute(CurrentUser.REQUEST_ATTRIBUTE);
        if (!(userId instanceof Long id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(authService.findUser(id));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from(AuthInterceptor.SESSION_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseEntity<UserView> withSession(UserView user) {
        ResponseCookie cookie = ResponseCookie.from(
                        AuthInterceptor.SESSION_COOKIE,
                        tokenService.issue(user.id())
                )
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(tokenHours))
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(user);
    }
}
