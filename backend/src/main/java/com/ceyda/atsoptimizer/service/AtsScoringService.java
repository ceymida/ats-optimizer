package com.ceyda.atsoptimizer.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AtsScoringService {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+");
    private static final Pattern PHONE = Pattern.compile("(\\+?\\d[\\d\\s().-]{7,}\\d)");
    private static final Pattern YEAR = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern NUMBERS = Pattern.compile("\\b\\d+([.,]\\d+)?\\s*(%|yüzde|k\\b|m\\b|bin|milyon|adet|kişi|users|kullanıcı)?");

    private static final List<String> ACTION_VERBS = List.of(
            "geliştirdim", "geliştirdi", "geliştirme", "tasarladım", "tasarladı", "yönettim", "yönetti",
            "yönetim", "oluşturdum", "oluşturdu", "iyileştirdim", "iyileştirdi", "optimize", "otomatikleştirdim",
            "azalttım", "artırdım", "kurdum", "uyguladım", "analiz ettim", "liderlik", "koordine",
            "developed", "designed", "built", "led", "managed", "created", "implemented", "improved",
            "optimized", "automated", "reduced", "increased", "delivered", "launched", "migrated",
            "architected", "maintained", "collaborated", "achieved"
    );

    private static final List<String> EXPERIENCE_WORDS = List.of(
            "deneyim", "deneyimler", "iş deneyimi", "experience", "experiences", "work experience",
            "employment", "professional experience"
    );
    private static final List<String> EDUCATION_WORDS = List.of(
            "eğitim", "egitim", "education", "üniversite", "university", "lisans", "bachelor",
            "master", "yüksek lisans", "mezuniyet"
    );
    private static final List<String> SKILL_WORDS = List.of(
            "yetenek", "yetenekler", "beceri", "beceriler", "skills", "teknik beceriler",
            "technical skills", "technologies", "teknolojiler"
    );
    private static final List<String> PROJECT_WORDS = List.of(
            "proje", "projeler", "project", "projects", "portfolyo", "portfolio"
    );

    private final KeywordExtractor keywordExtractor;

    public AtsScoringService(KeywordExtractor keywordExtractor) {
        this.keywordExtractor = keywordExtractor;
    }

    public record FormatCheck(String label, boolean passed, int points) {
    }

    public record ScoreResult(
            int overallScore,
            int keywordScore,
            int formatScore,
            int readabilityScore,
            String grade,
            String summary,
            int wordCount,
            List<String> matchedKeywords,
            List<KeywordDetail> missingKeywords,
            List<String> suggestions,
            List<FormatCheck> checks
    ) {
        public record KeywordDetail(String term, double weight) {
        }
    }

    public ScoreResult score(String resumeText, String jobDescription) {
        String normalizedResume = TextUtils.normalize(resumeText);
        String normalizedJob = TextUtils.normalize(jobDescription);
        if (normalizedResume.isBlank()) {
            throw new IllegalArgumentException("Özgeçmiş metni boş olamaz.");
        }
        if (normalizedJob.isBlank()) {
            throw new IllegalArgumentException("İş ilanı metni boş olamaz.");
        }

        List<KeywordExtractor.Keyword> jobKeywords = keywordExtractor.extractJobKeywords(jobDescription);
        List<String> matched = new ArrayList<>();
        List<ScoreResult.KeywordDetail> missing = new ArrayList<>();
        double totalWeight = 0;
        double matchedWeight = 0;

        for (KeywordExtractor.Keyword keyword : jobKeywords) {
            totalWeight += keyword.weight();
            if (keywordExtractor.resumeContains(normalizedResume, keyword.term())) {
                matchedWeight += keyword.weight();
                matched.add(keyword.term());
            } else {
                missing.add(new ScoreResult.KeywordDetail(keyword.term(), keyword.weight()));
            }
        }

        int keywordScore = totalWeight == 0 ? 0 : (int) Math.round(matchedWeight / totalWeight * 100);
        List<FormatCheck> checks = runFormatChecks(resumeText, normalizedResume);
        int formatEarned = checks.stream().filter(FormatCheck::passed).mapToInt(FormatCheck::points).sum();
        int formatTotal = checks.stream().mapToInt(FormatCheck::points).sum();
        int formatScore = formatTotal == 0 ? 0 : (int) Math.round(formatEarned * 100.0 / formatTotal);

        int wordCount = TextUtils.wordCount(resumeText);
        int readabilityScore = computeReadability(resumeText, wordCount);
        int overall = (int) Math.round(keywordScore * 0.6 + formatScore * 0.25 + readabilityScore * 0.15);
        String grade = grade(overall);
        String summary = buildSummary(overall, keywordScore, formatScore, missing.size());
        List<String> suggestions = buildSuggestions(checks, missing, wordCount, readabilityScore);

        return new ScoreResult(
                overall,
                keywordScore,
                formatScore,
                readabilityScore,
                grade,
                summary,
                wordCount,
                matched,
                missing,
                suggestions,
                checks
        );
    }

    private List<FormatCheck> runFormatChecks(String rawText, String normalized) {
        List<FormatCheck> checks = new ArrayList<>();
        checks.add(new FormatCheck("E-posta adresi", EMAIL.matcher(rawText).find(), 8));
        checks.add(new FormatCheck("Telefon numarası", PHONE.matcher(rawText).find(), 6));
        checks.add(new FormatCheck("LinkedIn / GitHub bağlantısı",
                containsAny(normalized, List.of("linkedin", "github", "gitlab", "portfolio")), 5));
        checks.add(new FormatCheck("Deneyim bölümü", containsAny(normalized, EXPERIENCE_WORDS), 9));
        checks.add(new FormatCheck("Eğitim bölümü", containsAny(normalized, EDUCATION_WORDS), 6));
        checks.add(new FormatCheck("Yetenekler bölümü", containsAny(normalized, SKILL_WORDS), 9));
        checks.add(new FormatCheck("Projeler / portfolyo", containsAny(normalized, PROJECT_WORDS), 4));
        checks.add(new FormatCheck("Madde işaretli başarılar", hasBullets(rawText), 8));
        checks.add(new FormatCheck("Eylem fiilleri", containsAny(normalized, ACTION_VERBS), 8));
        checks.add(new FormatCheck("Tarih / yıl bilgisi", YEAR.matcher(rawText).find(), 6));
        checks.add(new FormatCheck("Sayısal başarılar", hasQuantifiedAchievements(rawText), 7));
        checks.add(new FormatCheck("Uygun uzunluk (300-1000 kelime)",
                TextUtils.wordCount(rawText) >= 300 && TextUtils.wordCount(rawText) <= 1000, 10));
        return checks;
    }

    private boolean containsAny(String normalized, List<String> terms) {
        for (String term : terms) {
            if (TextUtils.containsTerm(normalized, term)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBullets(String rawText) {
        if (rawText == null) {
            return false;
        }
        return rawText.lines().filter(line -> {
            String trimmed = line.trim();
            return trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("*")
                    || trimmed.startsWith("▪") || trimmed.startsWith("●") || trimmed.startsWith("–");
        }).count() >= 3;
    }

    private boolean hasQuantifiedAchievements(String rawText) {
        if (rawText == null) {
            return false;
        }
        Matcher matcher = NUMBERS.matcher(rawText);
        int count = 0;
        while (matcher.find()) {
            if (matcher.group(2) != null) {
                return true;
            }
            count++;
        }
        return count >= 3;
    }

    private int computeReadability(String rawText, int wordCount) {
        double avgSentence = TextUtils.averageSentenceLength(rawText);
        int sentenceScore;
        if (avgSentence == 0) {
            sentenceScore = 0;
        } else if (avgSentence >= 8 && avgSentence <= 24) {
            sentenceScore = 100;
        } else if (avgSentence < 8) {
            sentenceScore = (int) Math.round(Math.max(30, avgSentence / 8 * 100));
        } else {
            sentenceScore = (int) Math.round(Math.max(20, 100 - (avgSentence - 24) * 3));
        }

        int lengthScore;
        if (wordCount >= 350 && wordCount <= 900) {
            lengthScore = 100;
        } else if (wordCount < 350) {
            lengthScore = (int) Math.round(Math.max(20, wordCount / 350.0 * 100));
        } else {
            lengthScore = (int) Math.round(Math.max(30, 100 - (wordCount - 900) / 25.0));
        }
        return (int) Math.round(sentenceScore * 0.5 + lengthScore * 0.5);
    }

    private String grade(int overall) {
        if (overall >= 85) {
            return "Mükemmel";
        }
        if (overall >= 70) {
            return "İyi";
        }
        if (overall >= 50) {
            return "Geliştirilmeli";
        }
        return "Zayıf";
    }

    private String buildSummary(int overall, int keywordScore, int formatScore, int missingCount) {
        String base;
        if (overall >= 85) {
            base = "Özgeçmişiniz bu ilan için oldukça güçlü görünüyor.";
        } else if (overall >= 70) {
            base = "Özgeçmişiniz iyi durumda; küçük iyileştirmelerle öne çıkabilir.";
        } else if (overall >= 50) {
            base = "Özgeçmişiniz temel uyumu sağlıyor ancak ATS için geliştirilmesi gereken alanlar var.";
        } else {
            base = "Özgeçmişiniz bu ilana göre zayıf uyum gösteriyor; anahtar kelimeler ve format iyileştirilmeli.";
        }
        return base + String.format(
                " Anahtar kelime uyumu %%%d, format puanı %%%d. İlanda geçen %d önemli terim özgeçmişinizde eksik.",
                keywordScore, formatScore, missingCount);
    }

    private List<String> buildSuggestions(List<FormatCheck> checks,
                                          List<ScoreResult.KeywordDetail> missing,
                                          int wordCount,
                                          int readabilityScore) {
        Set<String> suggestions = new LinkedHashSet<>();
        int added = 0;
        for (ScoreResult.KeywordDetail keyword : missing) {
            if (added >= 6) {
                break;
            }
            suggestions.add(String.format(
                    "\"%s\" terimi ilanda öne çıkıyor ancak özgeçmişinizde geçmiyor. İlgili deneyiminizi somut bir örnekle ekleyin.",
                    keyword.term()));
            added++;
        }
        for (FormatCheck check : checks) {
            if (check.passed()) {
                continue;
            }
            switch (check.label()) {
                case "E-posta adresi" -> suggestions.add("İletişim bilgilerinize profesyonel bir e-posta adresi ekleyin.");
                case "Telefon numarası" -> suggestions.add("ATS'nin okuyabileceği formatta bir telefon numarası ekleyin.");
                case "LinkedIn / GitHub bağlantısı" ->
                        suggestions.add("LinkedIn veya GitHub profilinizin bağlantısını ekleyin.");
                case "Deneyim bölümü" ->
                        suggestions.add("\"Deneyim\" bölümünü net bir başlıkla ayırın ve iş deneyimlerinizi tarih aralıklarıyla yazın.");
                case "Eğitim bölümü" -> suggestions.add("\"Eğitim\" bölümünü derece, okul ve mezuniyet yılıyla ekleyin.");
                case "Yetenekler bölümü" ->
                        suggestions.add("İlandaki teknolojileri içeren ayrı bir \"Yetenekler\" bölümü oluşturun.");
                case "Projeler / portfolyo" ->
                        suggestions.add("İlgili projelerinizi kısa açıklama ve kullanılan teknolojilerle listeleyin.");
                case "Madde işaretli başarılar" ->
                        suggestions.add("Deneyimlerinizi en az 3 madde işaretiyle (• veya -) okunabilir şekilde listeleyin.");
                case "Eylem fiilleri" ->
                        suggestions.add("Başarılarınızı \"geliştirdim\", \"optimize ettim\", \"yönettim\" gibi güçlü eylem fiilleriyle başlatın.");
                case "Tarih / yıl bilgisi" ->
                        suggestions.add("İş ve eğitim bilgilerine yıl/tarih aralıkları ekleyin.");
                case "Sayısal başarılar" ->
                        suggestions.add("Başarılarınızı yüzde, kullanıcı sayısı veya süre gibi ölçülebilir sonuçlarla destekleyin.");
                case "Uygun uzunluk (300-1000 kelime)" -> {
                    if (wordCount < 300) {
                        suggestions.add("Özgeçmişiniz çok kısa; deneyim ve projelerinizi detaylandırarak 300+ kelimeye çıkarın.");
                    } else {
                        suggestions.add("Özgeçmişiniz çok uzun; ilgisiz detayları çıkararak 1000 kelimenin altına indirin.");
                    }
                }
                default -> {
                }
            }
        }
        if (readabilityScore < 60) {
            suggestions.add("Cümlelerinizi kısaltın; her maddeyi tek bir başarıya odaklayarak okunabilirliği artırın.");
        }
        return new ArrayList<>(suggestions);
    }

    public static String detectLanguageNote(String resumeText) {
        return resumeText.toLowerCase(Locale.ROOT).contains("deneyim") ? "tr" : "en";
    }
}
