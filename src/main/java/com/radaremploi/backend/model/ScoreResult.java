package com.radaremploi.backend.model;

import java.util.List;

public record ScoreResult(
        int score,
        List<String> competencesManquantes,
        List<String> pointsForts
) {
    public static ScoreResult empty() {
        return new ScoreResult(0, List.of(), List.of());
    }
}
