package com.mtheory7.occlupanidservice.entites;

import java.util.List;

public record PredictionResponse(
        String topClass,
        double confidence,
        long inferenceTimeMs,
        List<CandidatePrediction> topCandidates
) {
public record CandidatePrediction(String className, double probability) {}
}