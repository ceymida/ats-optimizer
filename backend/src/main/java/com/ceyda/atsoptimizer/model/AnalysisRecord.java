package com.ceyda.atsoptimizer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "analysis_records")
public class AnalysisRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String resumeSource;

    private int overallScore;
    private int keywordScore;
    private int formatScore;
    private int readabilityScore;

    @Column(nullable = false)
    private Instant createdAt;

    protected AnalysisRecord() {
    }

    public AnalysisRecord(String fileName, String resumeSource, int overallScore,
                          int keywordScore, int formatScore, int readabilityScore) {
        this.fileName = fileName;
        this.resumeSource = resumeSource;
        this.overallScore = overallScore;
        this.keywordScore = keywordScore;
        this.formatScore = formatScore;
        this.readabilityScore = readabilityScore;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getResumeSource() {
        return resumeSource;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public int getKeywordScore() {
        return keywordScore;
    }

    public int getFormatScore() {
        return formatScore;
    }

    public int getReadabilityScore() {
        return readabilityScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
