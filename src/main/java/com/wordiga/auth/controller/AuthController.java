package com.wordiga.auth.controller;

import com.wordiga.auth.dto.AccessTokenResponse;
import com.wordiga.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @Value("${jwt.cookie-secure:true}")
    private boolean cookieSecure;

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(authService.refresh(refreshToken));
    }

    @DeleteMapping("/session")
    public ResponseEntity<Void> logout() {
        ResponseCookie expired = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
