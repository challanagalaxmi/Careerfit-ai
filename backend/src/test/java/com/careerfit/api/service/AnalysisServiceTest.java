package com.careerfit.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.careerfit.api.dto.AnalysisRequest;
import com.careerfit.api.dto.AnalysisResponse;
import com.careerfit.api.dto.AnalysisSummaryResponse;
import com.careerfit.api.dto.GeminiAnalysisResult;
import com.careerfit.api.dto.SkillGapItem;
import com.careerfit.api.entity.Analysis;
import com.careerfit.api.repository.AnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private GeminiService geminiService;

    @Test
    void createStoresGeminiResultAndReturnsApiContract() {
        AnalysisService service = new AnalysisService(analysisRepository, geminiService, new ObjectMapper());
        when(geminiService.analyze("Resume with Java", "Job needs Java and Docker"))
                .thenReturn(new GeminiAnalysisResult(
                        82,
                        List.of("Java"),
                        List.of("Docker"),
                        List.of("Add measurable achievements."),
                        List.of("REST API"),
                        List.of(new SkillGapItem("Docker", "HIGH", "Required by the job."))
                ));
        when(analysisRepository.save(any(Analysis.class))).thenAnswer(invocation -> {
            Analysis saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(Instant.parse("2026-09-06T12:00:00Z"));
            return saved;
        });

        AnalysisResponse response = service.create(new AnalysisRequest("Resume with Java", "Job needs Java and Docker"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.matchScore()).isEqualTo(82);
        assertThat(response.matchingSkills()).containsExactly("Java");
        assertThat(response.missingSkills()).containsExactly("Docker");
        assertThat(response.recommendedKeywords()).containsExactly("REST API");
        assertThat(response.skillGapRoadmap()).hasSize(1);
    }

    @Test
    void findAllReturnsNewestSummariesWithJobPreview() {
        AnalysisService service = new AnalysisService(analysisRepository, geminiService, new ObjectMapper());
        Analysis analysis = analysis();
        analysis.setJobDescription("This role needs Java, Spring Boot, MySQL, REST APIs, Hibernate, and practical debugging experience.");
        when(analysisRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(analysis));

        List<AnalysisSummaryResponse> response = service.findAll();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(1L);
        assertThat(response.get(0).matchScore()).isEqualTo(82);
        assertThat(response.get(0).jobDescriptionPreview()).contains("Java");
    }

    @Test
    void findByIdReturnsCompleteStoredAnalysis() {
        AnalysisService service = new AnalysisService(analysisRepository, geminiService, new ObjectMapper());
        when(analysisRepository.findById(1L)).thenReturn(Optional.of(analysis()));

        AnalysisResponse response = service.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.suggestions()).containsExactly("Add metrics.");
        assertThat(response.skillGapRoadmap().get(0).skill()).isEqualTo("Docker");
    }

    private Analysis analysis() {
        Analysis analysis = new Analysis();
        analysis.setId(1L);
        analysis.setResumeText("Resume text");
        analysis.setJobDescription("Job description");
        analysis.setMatchScore(82);
        analysis.setMatchingSkills("[\"Java\"]");
        analysis.setMissingSkills("[\"Docker\"]");
        analysis.setSuggestions("[\"Add metrics.\"]");
        analysis.setRecommendedKeywords("[\"REST API\"]");
        analysis.setSkillGapRoadmap("[{\"skill\":\"Docker\",\"priority\":\"HIGH\",\"reason\":\"Required by the role.\"}]");
        analysis.setCreatedAt(Instant.parse("2026-09-06T12:00:00Z"));
        return analysis;
    }
}
