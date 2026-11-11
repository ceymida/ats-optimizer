package com.ceyda.atsoptimizer.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtils {

    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{N}+#.]+");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("[.!?]+\\s+");

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "a", "adet", "all", "also", "an", "and", "any", "are", "as", "at", "be", "been", "bir", "bu", "by",
            "can", "da", "daha", "de", "for", "from", "gibi", "had", "has", "have", "her", "his", "how", "i",
            "ile", "in", "into", "is", "it", "its", "için", "ki", "kadar", "lar", "le", "ler", "mi", "more",
            "most", "not", "of", "on", "or", "our", "out", "olan", "olarak", "olduğu", "olmak", "ve", "veya",
            "was", "we", "were", "will", "with", "you", "your", "the", "to", "that", "this", "these", "those",
            "ile", "göre", "ancak", "ama", "fakat", "çok", "en", "hem", "ya", "her", "hangi", "nasıl", "ne",
            "job", "work", "role", "team", "company", "requirements", "responsibilities", "description",
            "looking", "candidate", "experience", "years", "year", "plus", "etc", "gibi", "bir", "the",
            "ability", "strong", "good", "knowledge", "using", "use", "used", "new", "other", "well",
            "must", "should", "would", "could", "may", "might", "about", "across", "within", "while",
            "senior", "junior", "mid", "principal", "staff", "intern", "remote", "hybrid", "onsite",
            "office", "salary", "benefits", "equity", "contract", "permanent", "join", "growing",
            "code", "review", "reviews", "engineer", "engineering",
            "aday", "adaylar", "aranıyor", "beklenen", "sorumluluklar", "nitelikler", "tecrübe", "deneyim"
    ));

    private TextUtils() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replace("i\u0307", "i")
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static List<String> tokens(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(normalize(text));
        while (matcher.find()) {
            String token = matcher.group();
            token = token.replaceAll("^\\.+|\\.+$", "");
            if (token.length() < 2 || token.chars().allMatch(Character::isDigit)) {
                continue;
            }
            result.add(token);
        }
        return result;
    }

    public static boolean isStopword(String token) {
        return STOPWORDS.contains(token);
    }

    public static boolean containsTerm(String haystackNormalized, String term) {
        String normalizedTerm = normalize(term);
        if (normalizedTerm.isEmpty()) {
            return false;
        }
        String quoted = Pattern.quote(normalizedTerm);
        Pattern pattern = Pattern.compile("(?<![\\p{L}\\p{N}+#.])" + quoted + "(?![\\p{L}\\p{N}+#.])");
        return pattern.matcher(haystackNormalized).find();
    }

    public static int wordCount(String text) {
        return tokens(text).size();
    }

    public static double averageSentenceLength(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String[] sentences = SENTENCE_SPLIT.split(text.trim());
        int sentenceCount = 0;
        int words = 0;
        for (String sentence : sentences) {
            if (sentence.isBlank()) {
                continue;
            }
            int count = tokens(sentence).size();
            if (count > 0) {
                sentenceCount++;
                words += count;
            }
        }
        return sentenceCount == 0 ? 0 : (double) words / sentenceCount;
    }
}
