package com.careerfit.api.controller;

import com.careerfit.api.dto.AnalysisRequest;
import com.careerfit.api.dto.AnalysisResponse;
import com.careerfit.api.dto.AnalysisSummaryResponse;
import com.careerfit.api.service.AnalysisService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse create(@Valid @RequestBody AnalysisRequest request) {
        return analysisService.create(request);
    }

    @GetMapping
    public List<AnalysisSummaryResponse> findAll() {
        return analysisService.findAll();
    }

    @GetMapping("/{id}")
    public AnalysisResponse findById(@PathVariable Long id) {
        return analysisService.findById(id);
    }
}
