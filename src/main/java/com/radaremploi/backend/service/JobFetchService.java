package com.radaremploi.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.radaremploi.backend.model.RawJob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobFetchService {

    private static final Logger log = LoggerFactory.getLogger(JobFetchService.class);
    private static final int MAX_DESCRIPTION_LENGTH = 4000;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<RawJob> fetchAll(String keyword) {
        List<RawJob> jobs = new ArrayList<>();
        jobs.addAll(fetchRemotive(keyword));
        jobs.addAll(fetchArbeitnow(keyword));
        jobs.addAll(fetchRemoteOk(keyword));

        // Dédoublonnage par URL
        Map<String, RawJob> unique = new LinkedHashMap<>();
        for (RawJob job : jobs) {
            if (job.url() != null && !job.url().isBlank()) {
                unique.putIfAbsent(job.url(), job);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private List<RawJob> fetchRemotive(String keyword) {
        List<RawJob> results = new ArrayList<>();
        try {
            String url = "https://remotive.com/api/remote-jobs?search=" + keyword;
            JsonNode root = mapper.readTree(restTemplate.getForObject(url, String.class));
            for (JsonNode j : root.path("jobs")) {
                results.add(new RawJob(
                        "Remotive",
                        text(j, "title"),
                        text(j, "company_name"),
                        text(j, "url"),
                        cleanHtml(text(j, "description")),
                        tagsOf(j.path("tags"))
                ));
            }
        } catch (Exception e) {
            log.warn("[Remotive] Erreur : {}", e.getMessage());
        }
        return results;
    }

    private List<RawJob> fetchArbeitnow(String keyword) {
        List<RawJob> results = new ArrayList<>();
        try {
            String url = "https://www.arbeitnow.com/api/job-board-api";
            JsonNode root = mapper.readTree(restTemplate.getForObject(url, String.class));
            String kw = keyword.toLowerCase();
            for (JsonNode j : root.path("data")) {
                String title = text(j, "title");
                String desc = text(j, "description");
                boolean remote = j.path("remote").asBoolean(false);
                if (remote && (title.toLowerCase().contains(kw) || desc.toLowerCase().contains(kw))) {
                    results.add(new RawJob(
                            "Arbeitnow",
                            title,
                            text(j, "company_name"),
                            text(j, "url"),
                            cleanHtml(desc),
                            tagsOf(j.path("tags"))
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("[Arbeitnow] Erreur : {}", e.getMessage());
        }
        return results;
    }

    private List<RawJob> fetchRemoteOk(String keyword) {
        List<RawJob> results = new ArrayList<>();
        try {
            String url = "https://remoteok.com/api";
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (job-search-agent)");
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode root = mapper.readTree(response.getBody());
            String kw = keyword.toLowerCase();

            for (JsonNode j : root) {
                if (!j.has("position")) continue; // le premier élément est des métadonnées, pas une offre
                String title = text(j, "position");
                List<String> tags = tagsOf(j.path("tags"));
                String tagsJoined = String.join(" ", tags).toLowerCase();
                if (title.toLowerCase().contains(kw) || tagsJoined.contains(kw)) {
                    results.add(new RawJob(
                            "RemoteOK",
                            title,
                            text(j, "company"),
                            text(j, "url"),
                            cleanHtml(text(j, "description")),
                            tags
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("[RemoteOK] Erreur : {}", e.getMessage());
        }
        return results;
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("");
    }

    private List<String> tagsOf(JsonNode tagsNode) {
        List<String> tags = new ArrayList<>();
        if (tagsNode.isArray()) {
            for (JsonNode t : tagsNode) tags.add(t.asText());
        }
        return tags;
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        String cleaned = html.replaceAll("<[^<]+?>", " ");
        return cleaned.length() > MAX_DESCRIPTION_LENGTH
                ? cleaned.substring(0, MAX_DESCRIPTION_LENGTH)
                : cleaned;
    }
}
