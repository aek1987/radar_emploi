package com.radaremploi.backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class CvExtractionService {

    public String extractText(String path) throws IOException {
        String lower = path.toLowerCase();

        if (lower.endsWith(".pdf")) {
            return extractFromPdf(path);
        } else if (lower.endsWith(".docx")) {
            return extractFromDocx(path);
        } else if (lower.endsWith(".txt")) {
            return Files.readString(Path.of(path));
        }

        throw new IllegalArgumentException("Format de CV non supporté : " + path);
    }

    private String extractFromPdf(String path) throws IOException {
        try (PDDocument document = Loader.loadPDF(new java.io.File(path))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractFromDocx(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path);
             XWPFDocument document = new XWPFDocument(fis)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                sb.append(paragraph.getText()).append("\n");
            }
            return sb.toString();
        }
    }
}
