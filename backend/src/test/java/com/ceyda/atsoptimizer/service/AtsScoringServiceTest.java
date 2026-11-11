package com.ceyda.atsoptimizer.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtsScoringServiceTest {

    private final AtsScoringService service = new AtsScoringService(new KeywordExtractor());

    private static final String RESUME = """
            Ayse Yilmaz
            ayse.yilmaz@example.com | +90 555 123 45 67 | linkedin.com/in/ayseyilmaz | github.com/ayseyilmaz

            DENEYIM
            Backend Developer, ACME Yazilim, 2021 - 2024
            - Java ve Spring Boot ile mikroservis gelistirdim.
            - PostgreSQL uzerinde calisan REST API tasarladim.
            - Docker ile deployment surecini otomatiklestirdim ve build suresini %30 azalttim.
            - 5 kisilik ekibe liderlik ettim.

            EGITIM
            Bilgisayar Muhendisligi, Ornek Universitesi, 2017 - 2021

            YETENEKLER
            Java, Spring Boot, PostgreSQL, Docker, Git, REST API, React

            PROJELER
            - E-ticaret siparis servisi: Spring Boot, PostgreSQL, Docker
            """;

    private static final String JOB = """
            We are looking for a Senior Java Backend Developer.

            Requirements:
            - Strong experience with Java and Spring Boot
            - Experience with Kafka and RabbitMQ for event-driven microservices
            - Hands-on Kubernetes and Docker experience
            - PostgreSQL or MongoDB knowledge
            - Experience with microservices architecture
            - AWS cloud experience is a plus
            - Agile/Scrum team experience
            """;

    @Test
    void scoresResumeAgainstJobDescription() {
        AtsScoringService.ScoreResult result = service.score(RESUME, JOB);

        assertThat(result.overallScore()).isBetween(1, 100);
        assertThat(result.keywordScore()).isGreaterThan(0);
        assertThat(result.formatScore()).isGreaterThan(0);
        assertThat(result.wordCount()).isGreaterThan(20);
        assertThat(result.grade()).isNotBlank();
        assertThat(result.matchedKeywords()).contains("Java", "Spring Boot", "PostgreSQL", "Docker", "Microservices");
        assertThat(result.missingKeywords())
                .extracting(AtsScoringService.ScoreResult.KeywordDetail::term)
                .contains("Kafka", "Kubernetes");
        assertThat(result.suggestions()).isNotEmpty();
    }

    @Test
    void detectsFormatIssuesForPlainTextResume() {
        AtsScoringService.ScoreResult result = service.score(
                "Kisa bir metin. Java biliyorum.", JOB);

        assertThat(result.formatScore()).isLessThan(60);
        assertThat(result.suggestions())
                .anyMatch(s -> s.contains("e-posta") || s.contains("Deneyim") || s.contains("Yetenekler"));
    }

    @Test
    void missingKeywordsAreSortedByImportance() {
        AtsScoringService.ScoreResult result = service.score(RESUME, JOB);
        List<Double> weights = result.missingKeywords().stream()
                .map(AtsScoringService.ScoreResult.KeywordDetail::weight)
                .toList();
        assertThat(weights).isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    @Test
    void rejectsEmptyJobDescription() {
        assertThatThrownBy(() -> service.score(RESUME, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("İş ilanı");
    }

    @Test
    void rejectsEmptyResume() {
        assertThatThrownBy(() -> service.score("  ", JOB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Özgeçmiş");
    }
}
