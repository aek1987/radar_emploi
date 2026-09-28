package com.radaremploi.backend.model;

import java.util.List;

public record JobsResponse(
        String updated_at,
        String keyword,
        int min_score,
        int total_analysees,
        List<JobOffer> offres
) {
    public static JobsResponse empty(String keyword, int minScore) {
        return new JobsResponse(null, keyword, minScore, 0, List.of());
    }
}
