package com.radaremploi.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.radaremploi.backend.model.RawJob;
import com.radaremploi.backend.model.ScoreResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    // Le plan gratuit Gemini limite le nombre de requêtes par minute.
    // On attend un peu entre chaque appel, et on réessaie en cas de dépassement.
    private static final long SECONDS_BETWEEN_CALLS = 20;
    private static final int MAX_RETRIES = 3;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public ScoreResult scoreJob(String cvText, RawJob job, String apiKey, String model) {
        String prompt = """
                Tu es un assistant de recrutement technique. Compare ce CV et cette offre DevOps.

                CV :
                %s

                Offre (%s chez %s) :
                %s

                Réponds UNIQUEMENT avec un objet JSON valide, sans texte autour :
                {"score": <entier 0-100>, "competences_manquantes": ["..."], "points_forts": ["..."]}
                """.formatted(truncate(cvText, 6000), job.title(), job.company(), job.description());

        String raw = callGemini(prompt, apiKey, model);
        try {
            String cleaned = raw.strip()
                    .replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceFirst("```$", "")
                    .strip();
            JsonNode node = mapper.readTree(cleaned);
            int score = node.path("score").asInt(0);
            List<String> manquantes = toList(node.path("competences_manquantes"));
            List<String> forts = toList(node.path("points_forts"));
            return new ScoreResult(score, manquantes, forts);
        } catch (Exception e) {
            log.warn("Impossible de parser la réponse de scoring : {}", e.getMessage());
            return ScoreResult.empty();
        }
    }

    public String generateCoverLetter(String cvText, RawJob job, String apiKey, String model) {
        String prompt = """
                Rédige une lettre de motivation courte (150-200 mots), en français, professionnelle,
                personnalisée pour cette offre précise (référence des éléments concrets de l'annonce et du profil).

                CV :
                %s

                Offre (%s chez %s) :
                %s
                """.formatted(truncate(cvText, 6000), job.title(), job.company(), job.description());

        return callGemini(prompt, apiKey, model);
    }

    private String callGemini(String prompt, String apiKey, String model) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s"
                .formatted(model, apiKey);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                JsonNode response = mapper.readTree(
                        restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class));
                sleep(SECONDS_BETWEEN_CALLS);
                return response
                        .path("candidates").path(0)
                        .path("content").path("parts").path(0)
                        .path("text").asText("");
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    long wait = 20L * attempt;
                    log.warn("Limite atteinte, attente de {}s ({}/{})", wait, attempt, MAX_RETRIES);
                    sleep(wait);
                } else {
                    log.error("Erreur Gemini ({}) : {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return "";
                }
            } catch (Exception e) {
                log.error("Erreur d'appel à Gemini : {}", e.getMessage());
                return "";
            }
        }
        return "";
    }

    private List<String> toList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode n : arrayNode) list.add(n.asText());
        }
        return list;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
