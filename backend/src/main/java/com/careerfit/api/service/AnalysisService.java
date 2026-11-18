package com.careerfit.api.service;

import com.careerfit.api.dto.AnalysisRequest;
import com.careerfit.api.dto.AnalysisResponse;
import com.careerfit.api.dto.AnalysisSummaryResponse;
import com.careerfit.api.dto.GeminiAnalysisResult;
import com.careerfit.api.dto.SkillGapItem;
import com.careerfit.api.entity.Analysis;
import com.careerfit.api.exception.AnalysisNotFoundException;
import com.careerfit.api.repository.AnalysisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<SkillGapItem>> ROADMAP_LIST = new TypeReference<>() {
    };

    private final AnalysisRepository analysisRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public AnalysisService(AnalysisRepository analysisRepository, GeminiService geminiService, ObjectMapper objectMapper) {
        this.analysisRepository = analysisRepository;
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalysisResponse create(AnalysisRequest request) {
        GeminiAnalysisResult aiResult = geminiService.analyze(request.resumeText(), request.jobDescription());

        Analysis analysis = new Analysis();
        analysis.setResumeText(request.resumeText());
        analysis.setJobDescription(request.jobDescription());
        analysis.setMatchScore(aiResult.matchScore());
        analysis.setMatchingSkills(toJson(orEmpty(aiResult.matchingSkills())));
        analysis.setMissingSkills(toJson(orEmpty(aiResult.missingSkills())));
        analysis.setSuggestions(toJson(orEmpty(aiResult.suggestions())));
        analysis.setRecommendedKeywords(toJson(orEmpty(aiResult.recommendedKeywords())));
        analysis.setSkillGapRoadmap(toJson(orEmpty(aiResult.skillGapRoadmap())));

        return toResponse(analysisRepository.save(analysis));
    }

    @Transactional(readOnly = true)
    public List<AnalysisSummaryResponse> findAll() {
        return analysisRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalysisResponse findById(Long id) {
        return analysisRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new AnalysisNotFoundException(id));
    }

    private AnalysisResponse toResponse(Analysis analysis) {
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getMatchScore(),
                fromJson(analysis.getMatchingSkills(), STRING_LIST),
                fromJson(analysis.getMissingSkills(), STRING_LIST),
                fromJson(analysis.getSuggestions(), STRING_LIST),
                fromJson(analysis.getRecommendedKeywords(), STRING_LIST),
                fromJson(analysis.getSkillGapRoadmap(), ROADMAP_LIST),
                analysis.getCreatedAt()
        );
    }

    private AnalysisSummaryResponse toSummary(Analysis analysis) {
        return new AnalysisSummaryResponse(
                analysis.getId(),
                analysis.getMatchScore(),
                preview(analysis.getJobDescription()),
                analysis.getCreatedAt()
        );
    }

    private String preview(String value) {
        String compact = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 120 ? compact : compact.substring(0, 117) + "...";
    }

    private <T> String toJson(T value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize analysis data.", exception);
        }
    }

    private <T> T fromJson(String value, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read stored analysis data.", exception);
        }
    }

    private <T> List<T> orEmpty(List<T> value) {
        return value == null ? List.of() : value;
    }
}
