package com.radaremploi.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppProperties {

    @Value("${app.google-api-key}")
    private String googleApiKey;

    @Value("${app.refresh-token}")
    private String refreshToken;

    @Value("${app.cv-path}")
    private String cvPath;

    @Value("${app.job-keyword}")
    private String jobKeyword;

    @Value("${app.min-score}")
    private int minScore;

    @Value("${app.gemini-model}")
    private String geminiModel;

    @Value("${app.max-jobs}")
    private int maxJobs; // 0 = pas de limite

    @Value("${app.data-path}")
    private String dataPath;

    public String getGoogleApiKey() { return googleApiKey; }
    public String getRefreshToken() { return refreshToken; }
    public String getCvPath() { return cvPath; }
    public String getJobKeyword() { return jobKeyword; }
    public int getMinScore() { return minScore; }
    public String getGeminiModel() { return geminiModel; }
    public Integer getMaxJobs() { return maxJobs > 0 ? maxJobs : null; }
    public String getDataPath() { return dataPath; }
}
