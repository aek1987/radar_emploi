package com.radaremploi.backend.model;

import java.util.List;

/**
 * Les noms de champs correspondent volontairement au contrat JSON déjà
 * utilisé par le frontend Angular (voir job.model.ts côté frontend).
 */
public record JobOffer(
        String source,
        String title,
        String company,
        String url,
        List<String> tags,
        int score,
        List<String> competences_manquantes,
        List<String> points_forts,
        String lettre
) {}
