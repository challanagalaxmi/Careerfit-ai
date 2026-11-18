package com.careerfit.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalysisRequest(
        @NotBlank(message = "Resume text is required")
        @Size(max = 20000, message = "Resume text must be 20,000 characters or fewer")
        String resumeText,

        @NotBlank(message = "Job description is required")
        @Size(max = 20000, message = "Job description must be 20,000 characters or fewer")
        String jobDescription
) {
}
