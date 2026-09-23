package com.healthplatform.lab.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record LabResultCreateRequest(
        @NotEmpty Map<String, Object> resultData
) {}
