package com.healthplatform.auth.controller;

import com.healthplatform.auth.dto.*;
import com.healthplatform.auth.model.User;
import com.healthplatform.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registration, login, token refresh, MFA")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Self-register a PATIENT account; any other role requires an authenticated ADMIN")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal User caller // null for anonymous requests
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, caller));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate; returns tokens or an MFA challenge")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Object result = authService.login(request);
        if (result instanceof MfaChallengeResponse) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "Complete login by verifying a TOTP code")
    public ResponseEntity<AuthTokensResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyMfaAndIssueTokens(request.challengeId(), request.code()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new token pair")
    public ResponseEntity<AuthTokensResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke all refresh tokens for the current user")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user) {
        authService.logout(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
