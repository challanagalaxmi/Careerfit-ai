package com.careerfit.api.dto;

import java.time.Instant;

public record AnalysisSummaryResponse(
        Long id,
        Integer matchScore,
        String jobDescriptionPreview,
        Instant createdAt
) {
}
