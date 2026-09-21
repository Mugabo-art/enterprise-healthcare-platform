package com.healthplatform.analytics.dto;

import java.time.LocalDate;

public record DailyCount(LocalDate date, long count) {}
