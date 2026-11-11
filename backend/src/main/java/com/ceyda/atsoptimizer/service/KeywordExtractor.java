package com.ceyda.atsoptimizer.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class KeywordExtractor {

    private static final Map<String, List<String>> TERM_ALIASES = new LinkedHashMap<>();

    private static final Map<String, String> TECH_TERMS = buildTechTerms();

    private static final int MAX_KEYWORDS = 35;

    private static Map<String, String> buildTechTerms() {
        Map<String, String> terms = new LinkedHashMap<>();
        add(terms, "Java", "java");
        add(terms, "Spring Boot", "spring boot");
        add(terms, "Spring", "spring");
        add(terms, "Hibernate", "hibernate");
        add(terms, "JPA", "jpa");
        add(terms, "Maven", "maven");
        add(terms, "Gradle", "gradle");
        add(terms, "JUnit", "junit");
        add(terms, "Microservices", "microservices", "mikroservis", "mikroservisler");
        add(terms, "REST API", "rest api", "restful", "rest apis");
        add(terms, "GraphQL", "graphql");
        add(terms, "Kafka", "kafka");
        add(terms, "RabbitMQ", "rabbitmq");
        add(terms, "Docker", "docker");
        add(terms, "Kubernetes", "kubernetes", "k8s");
        add(terms, "AWS", "aws", "amazon web services");
        add(terms, "Azure", "azure");
        add(terms, "Google Cloud", "gcp", "google cloud");
        add(terms, "CI/CD", "ci/cd", "cicd", "continuous integration");
        add(terms, "Jenkins", "jenkins");
        add(terms, "Git", "git");
        add(terms, "GitHub Actions", "github actions");
        add(terms, "SQL", "sql");
        add(terms, "PostgreSQL", "postgresql", "postgres");
        add(terms, "MySQL", "mysql");
        add(terms, "Oracle", "oracle");
        add(terms, "MongoDB", "mongodb", "mongo");
        add(terms, "Redis", "redis");
        add(terms, "Elasticsearch", "elasticsearch");
        add(terms, "NoSQL", "nosql");
        add(terms, "HTML", "html", "html5");
        add(terms, "CSS", "css", "css3");
        add(terms, "JavaScript", "javascript", "js");
        add(terms, "TypeScript", "typescript");
        add(terms, "React", "react", "react.js", "reactjs");
        add(terms, "Next.js", "next.js", "nextjs");
        add(terms, "Angular", "angular");
        add(terms, "Vue.js", "vue", "vue.js", "vuejs");
        add(terms, "Node.js", "node.js", "nodejs", "node");
        add(terms, "Tailwind CSS", "tailwind", "tailwind css");
        add(terms, "Python", "python");
        add(terms, "Django", "django");
        add(terms, "Flask", "flask");
        add(terms, "C#", "c#", "csharp", ".net", "dotnet", "asp.net");
        add(terms, "C++", "c++", "cpp");
        add(terms, "Go", "golang");
        add(terms, "PHP", "php");
        add(terms, "Laravel", "laravel");
        add(terms, "Scala", "scala");
        add(terms, "Kotlin", "kotlin");
        add(terms, "Swift", "swift");
        add(terms, "Android", "android");
        add(terms, "iOS", "ios");
        add(terms, "Flutter", "flutter");
        add(terms, "React Native", "react native");
        add(terms, "Machine Learning", "machine learning", "ml", "makine öğrenmesi");
        add(terms, "Deep Learning", "deep learning", "derin öğrenme");
        add(terms, "Artificial Intelligence", "artificial intelligence", "ai", "yapay zeka");
        add(terms, "TensorFlow", "tensorflow");
        add(terms, "PyTorch", "pytorch");
        add(terms, "Pandas", "pandas");
        add(terms, "NumPy", "numpy");
        add(terms, "Data Analysis", "data analysis", "data analytics", "veri analizi");
        add(terms, "Data Science", "data science");
        add(terms, "Big Data", "big data");
        add(terms, "Spark", "spark", "apache spark");
        add(terms, "ETL", "etl");
        add(terms, "Agile", "agile", "çevik");
        add(terms, "Scrum", "scrum");
        add(terms, "Kanban", "kanban");
        add(terms, "Jira", "jira");
        add(terms, "Confluence", "confluence");
        add(terms, "OOP", "oop", "object oriented", "object-oriented");
        add(terms, "SOLID", "solid");
        add(terms, "Design Patterns", "design patterns");
        add(terms, "TDD", "tdd", "test driven", "test-driven");
        add(terms, "Unit Testing", "unit testing", "unit test", "birim testi");
        add(terms, "Integration Testing", "integration testing");
        add(terms, "Linux", "linux");
        add(terms, "Unix", "unix");
        add(terms, "Bash", "bash", "shell scripting");
        add(terms, "Nginx", "nginx");
        add(terms, "Tomcat", "tomcat");
        add(terms, "OAuth", "oauth", "oauth2");
        add(terms, "JWT", "jwt");
        add(terms, "Security", "security", "cybersecurity", "güvenlik");
        add(terms, "Monitoring", "monitoring");
        add(terms, "Prometheus", "prometheus");
        add(terms, "Grafana", "grafana");
        add(terms, "Selenium", "selenium");
        add(terms, "Cypress", "cypress");
        add(terms, "Playwright", "playwright");
        add(terms, "Figma", "figma");
        add(terms, "UI/UX", "ui/ux", "ux", "ui design");
        add(terms, "Product Management", "product management", "product manager", "ürün yönetimi");
        add(terms, "Project Management", "project management", "proje yönetimi");
        add(terms, "DevOps", "devops");
        add(terms, "Redux", "redux");
        add(terms, "Sass", "sass", "scss");
        add(terms, "Webpack", "webpack");
        add(terms, "Vite", "vite");
        add(terms, "Swagger", "swagger", "openapi");
        add(terms, "Postman", "postman");
        add(terms, "MVC", "mvc");
        add(terms, "DDD", "ddd", "domain driven design");
        add(terms, "Clean Code", "clean code");
        add(terms, "Code Review", "code review", "kod inceleme");
        add(terms, "Problem Solving", "problem solving", "problem çözme", "problem cozme");
        add(terms, "Communication", "communication", "iletişim", "iletisim");
        add(terms, "Leadership", "leadership", "liderlik");
        add(terms, "Teamwork", "teamwork", "team player", "takım çalışması", "takim calismasi");
        add(terms, "Analytical Thinking", "analytical", "analitik");
        add(terms, "English", "english", "ingilizce");
        return terms;
    }

    private static void add(Map<String, String> terms, String canonical, String... aliases) {
        terms.put(canonical.toLowerCase(Locale.ROOT), canonical);
        for (String alias : aliases) {
            TERM_ALIASES.computeIfAbsent(canonical, k -> new ArrayList<>()).add(alias);
        }
    }

    public record Keyword(String term, double weight) {
    }

    public List<Keyword> extractJobKeywords(String jobDescription) {
        String normalized = TextUtils.normalize(jobDescription);
        Map<String, Double> weights = new LinkedHashMap<>();
        Map<String, Integer> frequencies = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : TECH_TERMS.entrySet()) {
            String canonical = entry.getValue();
            List<String> candidates = TERM_ALIASES.getOrDefault(canonical, List.of(entry.getKey()));
            for (String candidate : candidates) {
                if (TextUtils.containsTerm(normalized, candidate)) {
                    weights.merge(canonical, 3.0, Math::max);
                    break;
                }
            }
        }

        List<String> tokens = TextUtils.tokens(jobDescription);
        for (String token : tokens) {
            if (token.length() < 3 || TextUtils.isStopword(token) || token.chars().allMatch(Character::isDigit)) {
                continue;
            }
            frequencies.merge(token, 1, Integer::sum);
        }

        frequencies.forEach((token, frequency) -> {
            if (frequency < 2) {
                return;
            }
            String canonical = TECH_TERMS.getOrDefault(token, token);
            if (weights.containsKey(canonical)) {
                return;
            }
            double weight = frequency >= 5 ? 2.0 : frequency >= 3 ? 1.5 : 1.0;
            weights.merge(canonical, weight, Math::max);
        });

        List<Keyword> keywords = new ArrayList<>();
        weights.forEach((term, weight) -> keywords.add(new Keyword(term, weight)));
        keywords.sort(Comparator.comparingDouble(Keyword::weight).reversed()
                .thenComparing(Keyword::term, String.CASE_INSENSITIVE_ORDER));
        keywords.removeIf(keyword -> keywords.stream().anyMatch(other ->
                !other.term().equalsIgnoreCase(keyword.term()) && isTokenPrefix(keyword.term(), other.term())));
        return keywords.size() > MAX_KEYWORDS ? keywords.subList(0, MAX_KEYWORDS) : keywords;
    }

    private boolean isTokenPrefix(String candidate, String longer) {
        String[] candidateTokens = TextUtils.normalize(candidate).split("\\s+");
        String[] longerTokens = TextUtils.normalize(longer).split("\\s+");
        if (candidateTokens.length >= longerTokens.length) {
            return false;
        }
        for (int i = 0; i < candidateTokens.length; i++) {
            if (!candidateTokens[i].equals(longerTokens[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean resumeContains(String normalizedResume, String canonicalTerm) {
        if (TextUtils.containsTerm(normalizedResume, canonicalTerm)) {
            return true;
        }
        for (String alias : TERM_ALIASES.getOrDefault(canonicalTerm, List.of())) {
            if (TextUtils.containsTerm(normalizedResume, alias)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTechTerm(String term) {
        return TECH_TERMS.containsKey(term.toLowerCase(Locale.ROOT));
    }
}
