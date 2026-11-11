package com.ceyda.atsoptimizer.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordExtractorTest {

    private final KeywordExtractor extractor = new KeywordExtractor();

    @Test
    void detectsTechAliasesWithCanonicalNames() {
        var keywords = extractor.extractJobKeywords(
                "We use spring boot, k8s, postgres, nodejs and react.js in our stack.");

        assertThat(keywords)
                .extracting(KeywordExtractor.Keyword::term)
                .contains("Spring Boot", "Kubernetes", "PostgreSQL", "Node.js", "React");
    }

    @Test
    void doesNotMatchPartialWords() {
        var keywords = extractor.extractJobKeywords("We are looking for a JavaScript developer.");

        assertThat(keywords)
                .extracting(KeywordExtractor.Keyword::term)
                .contains("JavaScript")
                .doesNotContain("Java");
    }

    @Test
    void extractsFrequentNonTechKeywords() {
        var keywords = extractor.extractJobKeywords(
                "microservices architecture. microservices design. event driven event driven architecture.");

        assertThat(keywords)
                .extracting(KeywordExtractor.Keyword::term)
                .contains("Microservices");
    }
}
