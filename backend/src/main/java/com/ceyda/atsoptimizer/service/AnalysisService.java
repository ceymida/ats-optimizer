package com.ceyda.atsoptimizer.service;

import com.ceyda.atsoptimizer.dto.AnalysisResponse;
import com.ceyda.atsoptimizer.dto.HistoryItem;
import com.ceyda.atsoptimizer.model.AnalysisRecord;
import com.ceyda.atsoptimizer.repository.AnalysisRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisService {

    private static final int MAX_RESUME_CHARS = 40_000;
    private static final int MAX_JOB_CHARS = 20_000;

    private final ResumeTextExtractor resumeTextExtractor;
    private final AtsScoringService atsScoringService;
    private final OpenAiSuggestionClient openAiSuggestionClient;
    private final AnalysisRecordRepository repository;

    public AnalysisService(ResumeTextExtractor resumeTextExtractor,
                           AtsScoringService atsScoringService,
                           OpenAiSuggestionClient openAiSuggestionClient,
                           AnalysisRecordRepository repository) {
        this.resumeTextExtractor = resumeTextExtractor;
        this.atsScoringService = atsScoringService;
        this.openAiSuggestionClient = openAiSuggestionClient;
        this.repository = repository;
    }

    @Transactional
    public AnalysisResponse analyze(MultipartFile file, String resumeText, String jobDescription) {
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new IllegalArgumentException("İş ilanı metni zorunludur.");
        }
        if (jobDescription.length() > MAX_JOB_CHARS) {
            throw new IllegalArgumentException("İş ilanı metni çok uzun (en fazla " + MAX_JOB_CHARS + " karakter).");
        }

        boolean hasFile = file != null && !file.isEmpty();
        boolean hasText = resumeText != null && !resumeText.isBlank();
        if (!hasFile && !hasText) {
            throw new IllegalArgumentException("Bir özgeçmiş dosyası yükleyin veya metni yapıştırın.");
        }

        String extracted = hasFile ? resumeTextExtractor.extract(file) : resumeText;
        if (extracted.length() > MAX_RESUME_CHARS) {
            throw new IllegalArgumentException("Özgeçmiş metni çok uzun (en fazla " + MAX_RESUME_CHARS + " karakter).");
        }

        AtsScoringService.ScoreResult result = atsScoringService.score(extracted, jobDescription);

        List<String> suggestions = new ArrayList<>(result.suggestions());
        boolean aiEnhanced = false;
        List<String> missingTerms = result.missingKeywords().stream()
                .limit(15)
                .map(AtsScoringService.ScoreResult.KeywordDetail::term)
                .toList();
        var aiSuggestions = openAiSuggestionClient.suggest(extracted, jobDescription, missingTerms);
        if (aiSuggestions.isPresent()) {
            suggestions = new ArrayList<>(aiSuggestions.get());
            aiEnhanced = true;
        }

        String fileName = hasFile ? file.getOriginalFilename() : "yapistirilan-metin.txt";
        if (fileName == null || fileName.isBlank()) {
            fileName = "ozgecmis";
        }
        String source = hasFile ? "FILE" : "TEXT";

        AnalysisRecord record = repository.save(new AnalysisRecord(
                fileName,
                source,
                result.overallScore(),
                result.keywordScore(),
                result.formatScore(),
                result.readabilityScore()
        ));

        List<AnalysisResponse.KeywordDetail> missing = result.missingKeywords().stream()
                .map(k -> new AnalysisResponse.KeywordDetail(k.term(), k.weight()))
                .toList();
        List<AnalysisResponse.CheckItem> checks = result.checks().stream()
                .map(c -> new AnalysisResponse.CheckItem(c.label(), c.passed(), c.points()))
                .toList();

        return new AnalysisResponse(
                record.getId(),
                result.overallScore(),
                result.keywordScore(),
                result.formatScore(),
                result.readabilityScore(),
                result.grade(),
                result.summary(),
                result.wordCount(),
                result.matchedKeywords(),
                missing,
                suggestions,
                checks,
                aiEnhanced,
                record.getFileName(),
                record.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<HistoryItem> history(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return repository.findAll(PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(r -> new HistoryItem(
                        r.getId(),
                        r.getFileName(),
                        r.getOverallScore(),
                        r.getKeywordScore(),
                        r.getFormatScore(),
                        r.getCreatedAt()))
                .toList();
    }
}
