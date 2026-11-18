package com.careerfit.api.dto;

import java.time.Instant;
import java.util.List;

public record AnalysisResponse(
        Long id,
        Integer matchScore,
        List<String> matchingSkills,
        List<String> missingSkills,
        List<String> suggestions,
        List<String> recommendedKeywords,
        List<SkillGapItem> skillGapRoadmap,
        Instant createdAt
) {
}
