package com.careerfit.api.dto;

import java.util.List;

public record GeminiAnalysisResult(
        Integer matchScore,
        List<String> matchingSkills,
        List<String> missingSkills,
        List<String> suggestions,
        List<String> recommendedKeywords,
        List<SkillGapItem> skillGapRoadmap
) {
}
