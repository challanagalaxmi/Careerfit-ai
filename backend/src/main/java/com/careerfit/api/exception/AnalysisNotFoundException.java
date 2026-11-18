package com.careerfit.api.exception;

public class AnalysisNotFoundException extends RuntimeException {

    public AnalysisNotFoundException(Long id) {
        super("Analysis " + id + " was not found.");
    }
}
