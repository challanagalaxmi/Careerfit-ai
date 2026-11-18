package com.careerfit.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerfit.api.config.CorsConfig;
import com.careerfit.api.dto.AnalysisResponse;
import com.careerfit.api.dto.AnalysisSummaryResponse;
import com.careerfit.api.dto.SkillGapItem;
import com.careerfit.api.exception.GlobalExceptionHandler;
import com.careerfit.api.service.AnalysisService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalysisController.class)
@Import({CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origin=http://localhost:5173")
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalysisService analysisService;

    @Test
    void postAnalysisReturnsCreatedResponseContract() throws Exception {
        when(analysisService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resumeText": "Resume with Java",
                                  "jobDescription": "Job needs Java and Docker"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.matchScore").value(82))
                .andExpect(jsonPath("$.matchingSkills[0]").value("Java"))
                .andExpect(jsonPath("$.missingSkills[0]").value("Docker"))
                .andExpect(jsonPath("$.skillGapRoadmap[0].priority").value("HIGH"));
    }

    @Test
    void postAnalysisValidatesRequiredFields() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resumeText": "",
                                  "jobDescription": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please check the submitted fields."))
                .andExpect(jsonPath("$.details.resumeText", containsString("required")))
                .andExpect(jsonPath("$.details.jobDescription", containsString("required")));
    }

    @Test
    void getAnalysesReturnsHistoryContract() throws Exception {
        when(analysisService.findAll()).thenReturn(List.of(new AnalysisSummaryResponse(
                1L,
                82,
                "Job needs Java and Docker",
                Instant.parse("2026-09-06T12:00:00Z")
        )));

        mockMvc.perform(get("/api/analyses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].matchScore").value(82))
                .andExpect(jsonPath("$[0].jobDescriptionPreview").value("Job needs Java and Docker"));
    }

    @Test
    void getAnalysisByIdReturnsCompleteContract() throws Exception {
        when(analysisService.findById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/analyses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.recommendedKeywords[0]").value("REST API"));
    }

    @Test
    void corsAllowsFrontendOriginForApiRoutes() throws Exception {
        mockMvc.perform(options("/api/analyses")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
    }

    private AnalysisResponse response() {
        return new AnalysisResponse(
                1L,
                82,
                List.of("Java"),
                List.of("Docker"),
                List.of("Add measurable achievements."),
                List.of("REST API"),
                List.of(new SkillGapItem("Docker", "HIGH", "Required by the job.")),
                Instant.parse("2026-09-06T12:00:00Z")
        );
    }
}
