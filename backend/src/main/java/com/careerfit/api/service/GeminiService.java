package com.careerfit.api.service;

import com.careerfit.api.dto.GeminiAnalysisResult;
import com.careerfit.api.exception.GeminiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GeminiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.api.base-url}") String baseUrl,
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public GeminiAnalysisResult analyze(String resumeText, String jobDescription) {
        if (!StringUtils.hasText(apiKey)) {
            throw new GeminiServiceException("Gemini API key is not configured. Set GEMINI_API_KEY and restart the backend.");
        }

        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", buildPrompt(resumeText, jobDescription)))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            return parseResponse(response);
        } catch (RestClientException exception) {
            throw new GeminiServiceException("Gemini could not analyze the resume right now. Please try again.", exception);
        }
    }

    private GeminiAnalysisResult parseResponse(JsonNode response) {
        JsonNode textNode = response == null
                ? null
                : response.at("/candidates/0/content/parts/0/text");

        if (textNode == null || textNode.isMissingNode() || !StringUtils.hasText(textNode.asText())) {
            throw new GeminiServiceException("Gemini returned an empty analysis response.");
        }

        String jsonText = stripCodeFence(textNode.asText());

        try {
            GeminiAnalysisResult result = objectMapper.readValue(jsonText, GeminiAnalysisResult.class);
            validateResult(result);
            return result;
        } catch (Exception exception) {
            throw new GeminiServiceException("Gemini returned malformed JSON. Please run the analysis again.", exception);
        }
    }

    private void validateResult(GeminiAnalysisResult result) {
        if (result == null || result.matchScore() == null || result.matchScore() < 0 || result.matchScore() > 100) {
            throw new GeminiServiceException("Gemini returned an invalid match score.");
        }
    }

    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private String buildPrompt(String resumeText, String jobDescription) {
        return """
                Compare the resume against the job description.

                Rules:
                - Do not invent skills from the resume.
                - matchingSkills must include only skills explicitly present in both the resume and job description.
                - missingSkills must include important skills required by the job description but absent from the resume.
                - suggestions must be practical resume improvement suggestions.
                - recommendedKeywords must be useful ATS keywords based only on the job description.
                - skillGapRoadmap must be concise and actionable.
                - Return ONLY valid JSON matching this schema:
                  {
                    "matchScore": 0,
                    "matchingSkills": ["string"],
                    "missingSkills": ["string"],
                    "suggestions": ["string"],
                    "recommendedKeywords": ["string"],
                    "skillGapRoadmap": [
                      {
                        "skill": "string",
                        "priority": "HIGH | MEDIUM | LOW",
                        "reason": "string"
                      }
                    ]
                  }

                Resume:
                %s

                Job Description:
                %s
                """.formatted(resumeText, jobDescription);
    }
}
