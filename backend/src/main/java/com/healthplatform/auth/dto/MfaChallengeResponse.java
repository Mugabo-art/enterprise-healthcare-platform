package com.healthplatform.auth.dto;

import java.util.UUID;

public record MfaChallengeResponse(boolean mfaRequired, UUID challengeId) {}
