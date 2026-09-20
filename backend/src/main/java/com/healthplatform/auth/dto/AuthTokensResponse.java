package com.healthplatform.auth.dto;

public record AuthTokensResponse(String accessToken, String refreshToken, long expiresIn) {}
