package com.ceyda.atsoptimizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OpenAiSuggestionClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiSuggestionClient.class);
    private static final int MAX_RESUME_CHARS = 6000;
    private static final int MAX_JOB_CHARS = 4000;

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;

    public OpenAiSuggestionClient(
            ObjectMapper objectMapper,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-4o-mini}") String model,
            @Value("${app.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.openai.timeout-seconds:25}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.baseUrl = baseUrl;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    public boolean isEnabled() {
        return !apiKey.isBlank();
    }

    public Optional<List<String>> suggest(String resumeText, String jobDescription,
                                          List<String> missingKeywords) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", model);
            payload.put("temperature", 0.4);
            ObjectNode responseFormat = payload.putObject("response_format");
            responseFormat.put("type", "json_object");

            ArrayNode messages = payload.putArray("messages");
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content",
                    "Sen deneyimli bir ATS (Applicant Tracking System) danışmanısın. "
                            + "Türkçe, somut ve uygulanabilir öneriler üretirsin. Yalnızca JSON döndür: "
                            + "{\"suggestions\": [\"...\", \"...\"]}. En fazla 6 öneri yaz, her biri tek cümle olsun. "
                            + "Markdown, madde işareti veya ekstra alan kullanma.");

            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", buildPrompt(resumeText, jobDescription, missingKeywords));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OpenAI isteği başarısız oldu: HTTP {} - {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (!contentNode.isTextual()) {
                return Optional.empty();
            }
            JsonNode parsed = objectMapper.readTree(stripCodeFence(contentNode.asText()));
            JsonNode suggestionsNode = parsed.path("suggestions");
            if (!suggestionsNode.isArray()) {
                return Optional.empty();
            }
            List<String> suggestions = new ArrayList<>();
            for (JsonNode node : suggestionsNode) {
                if (node.isTextual() && !node.asText().isBlank()) {
                    suggestions.add(node.asText().trim());
                }
                if (suggestions.size() >= 6) {
                    break;
                }
            }
            return suggestions.isEmpty() ? Optional.empty() : Optional.of(suggestions);
        } catch (Exception e) {
            log.warn("OpenAI önerileri alınamadı, kural tabanlı önerilere dönülüyor: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String buildPrompt(String resumeText, String jobDescription, List<String> missingKeywords) {
        String resume = truncate(resumeText, MAX_RESUME_CHARS);
        String job = truncate(jobDescription, MAX_JOB_CHARS);
        String missing = missingKeywords.isEmpty() ? "(yok)" : String.join(", ", missingKeywords);
        return """
                Aşağıdaki özgeçmişi hedef iş ilanına göre ATS uyumu açısından değerlendir.

                İŞ İLANI:
                %s

                ÖZGEÇMİŞ:
                %s

                İLANDA OLUP ÖZGEÇMİŞTE EKSİK OLAN TERİMLER:
                %s

                Kural tabanlı sistemin zaten verdiği genel önerileri tekrarlamak yerine,
                bu özgeçmişe özel 5-6 somut iyileştirme önerisi ver. Örneğin hangi deneyime
                hangi terimin nasıl ekleneceğini, hangi başarının ölçülebilir hale getirileceğini söyle.
                """.formatted(job, resume, missing);
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
