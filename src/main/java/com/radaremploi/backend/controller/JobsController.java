package com.radaremploi.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.radaremploi.backend.config.AppProperties;
import com.radaremploi.backend.model.JobsResponse;
import com.radaremploi.backend.service.DataStoreService;
import com.radaremploi.backend.service.PipelineService;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping
public class JobsController {

    private final DataStoreService dataStoreService;
    private final PipelineService pipelineService;
    private final AppProperties props;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile String lastError = null;

    public JobsController(DataStoreService dataStoreService,
                           PipelineService pipelineService,
                           AppProperties props) {
        this.dataStoreService = dataStoreService;
        this.pipelineService = pipelineService;
        this.props = props;
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "job-board-backend"));
    }

    @GetMapping("/api/offres")
    public ResponseEntity<JobsResponse> getOffres() {
        return ResponseEntity.ok(dataStoreService.load());
    }

    @GetMapping("/api/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("running", running.get());
        body.put("error", lastError);
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/api/refresh", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "X-Refresh-Token", required = false) String tokenHeader) {

        String suppliedToken = token != null ? token : tokenHeader;
        String expectedToken = props.getRefreshToken();

        if (expectedToken == null || expectedToken.isBlank() || !expectedToken.equals(suppliedToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (props.getGoogleApiKey() == null || props.getGoogleApiKey().isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "error", "GOOGLE_API_KEY non configurée sur le serveur"));
        }

        if (!running.compareAndSet(false, true)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("ok", false, "error", "Un rafraîchissement est déjà en cours"));
        }

        lastError = null;
        Thread worker = new Thread(() -> {
            try {
                pipelineService.run();
            } catch (Exception e) {
                lastError = e.getMessage();
            } finally {
                running.set(false);
            }
        });
        worker.setDaemon(true);
        worker.start();

        return ResponseEntity.ok(Map.of("ok", true, "message", "Rafraîchissement lancé en arrière-plan"));
    }
}
