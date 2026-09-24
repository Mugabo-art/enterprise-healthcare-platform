package com.healthplatform.auth.dto;

/** challengeId is a short-lived signed token (not a user id) — see JwtService#generateMfaChallengeToken. */
public record MfaChallengeResponse(boolean mfaRequired, String challengeId) {}
