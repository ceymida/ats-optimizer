package com.ceyda.atsoptimizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ResumeTextExtractor {

    private static final int MAX_PDF_PAGES = 20;

    private final ObjectMapper objectMapper;

    public ResumeTextExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Özgeçmiş dosyası boş.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".pdf")) {
                return extractPdf(file.getBytes());
            }
            if (name.endsWith(".json")) {
                return extractJson(file.getBytes());
            }
            if (name.endsWith(".txt") || name.endsWith(".md")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            throw new IllegalArgumentException("Desteklenmeyen dosya türü. PDF, JSON veya TXT yükleyin.");
        } catch (IOException e) {
            throw new IllegalArgumentException("Dosya okunamadı: " + e.getMessage(), e);
        }
    }

    private String extractPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("Şifreli PDF dosyaları desteklenmiyor.");
            }
            if (document.getNumberOfPages() > MAX_PDF_PAGES) {
                throw new IllegalArgumentException("PDF en fazla " + MAX_PDF_PAGES + " sayfa olabilir.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException(
                        "PDF içinden metin çıkarılamadı. Dosya taranmış görüntü olabilir; metin tabanlı PDF yükleyin.");
            }
            return text;
        }
    }

    private String extractJson(byte[] bytes) throws IOException {
        JsonNode root = objectMapper.readTree(bytes);
        List<String> parts = new ArrayList<>();
        flatten(root, parts);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("JSON içinde metin alanı bulunamadı.");
        }
        return String.join("\n", parts);
    }

    private void flatten(JsonNode node, List<String> parts) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            if (!node.asText().isBlank()) {
                parts.add(node.asText());
            }
        } else if (node.isNumber() || node.isBoolean()) {
            parts.add(node.asText());
        } else if (node.isArray()) {
            node.forEach(child -> flatten(child, parts));
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> flatten(entry.getValue(), parts));
        }
    }
}
