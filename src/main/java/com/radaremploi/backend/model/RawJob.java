package com.radaremploi.backend.model;

import java.util.List;

/** Offre telle que récupérée depuis Remotive/Arbeitnow/RemoteOK, avant passage par Gemini. */
public record RawJob(
        String source,
        String title,
        String company,
        String url,
        String description,
        List<String> tags
) {}
