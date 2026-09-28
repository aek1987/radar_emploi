package com.radaremploi.backend.service;

import org.springframework.stereotype.Service;

import com.radaremploi.backend.config.AppProperties;
import com.radaremploi.backend.model.JobOffer;
import com.radaremploi.backend.model.JobsResponse;
import com.radaremploi.backend.model.RawJob;
import com.radaremploi.backend.model.ScoreResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PipelineService {

    private final CvExtractionService cvExtractionService;
    private final JobFetchService jobFetchService;
    private final GeminiService geminiService;
    private final DataStoreService dataStoreService;
    private final AppProperties props;

    public PipelineService(CvExtractionService cvExtractionService,
                            JobFetchService jobFetchService,
                            GeminiService geminiService,
                            DataStoreService dataStoreService,
                            AppProperties props) {
        this.cvExtractionService = cvExtractionService;
        this.jobFetchService = jobFetchService;
        this.geminiService = geminiService;
        this.dataStoreService = dataStoreService;
        this.props = props;
    }

    /** Exécute tout le pipeline et sauvegarde le résultat. À appeler depuis un thread à part. */
    public JobsResponse run() throws Exception {
        String apiKey = props.getGoogleApiKey();
        String model = props.getGeminiModel();
        String keyword = props.getJobKeyword();
        int minScore = props.getMinScore();
        Integer maxJobs = props.getMaxJobs();

        String cvText = cvExtractionService.extractText(props.getCvPath());

        List<RawJob> jobs = jobFetchService.fetchAll(keyword);
        if (maxJobs != null && jobs.size() > maxJobs) {
            jobs = jobs.subList(0, maxJobs);
        }

        List<JobOffer> offers = new ArrayList<>();
        for (RawJob job : jobs) {
            ScoreResult result = geminiService.scoreJob(cvText, job, apiKey, model);
            String letter = result.score() >= minScore
                    ? geminiService.generateCoverLetter(cvText, job, apiKey, model)
                    : "";

            List<String> tags = job.tags().size() > 8 ? job.tags().subList(0, 8) : job.tags();

            offers.add(new JobOffer(
                    job.source(),
                    job.title(),
                    job.company(),
                    job.url(),
                    tags,
                    result.score(),
                    result.competencesManquantes(),
                    result.pointsForts(),
                    letter
            ));
        }

        offers.sort(Comparator.comparingInt(JobOffer::score).reversed());

        JobsResponse response = new JobsResponse(
                Instant.now().toString(),
                keyword,
                minScore,
                offers.size(),
                offers
        );

        dataStoreService.save(response);
        return response;
    }
}
