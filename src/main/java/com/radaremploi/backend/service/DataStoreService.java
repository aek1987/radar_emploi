package com.radaremploi.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.radaremploi.backend.model.JobsResponse;

import java.io.File;
import java.io.IOException;

@Service
public class DataStoreService {

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.data-path}")
    private String dataPath;

    @Value("${app.job-keyword}")
    private String defaultKeyword;

    @Value("${app.min-score}")
    private int defaultMinScore;

    public JobsResponse load() {
        File file = new File(dataPath);
        if (!file.exists()) {
            return JobsResponse.empty(defaultKeyword, defaultMinScore);
        }
        try {
            return mapper.readValue(file, JobsResponse.class);
        } catch (IOException e) {
            return JobsResponse.empty(defaultKeyword, defaultMinScore);
        }
    }

    public void save(JobsResponse response) throws IOException {
        File file = new File(dataPath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, response);
    }
}
