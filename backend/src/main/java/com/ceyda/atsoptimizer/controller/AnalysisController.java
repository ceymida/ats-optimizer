package com.ceyda.atsoptimizer.controller;

import com.ceyda.atsoptimizer.dto.AnalysisResponse;
import com.ceyda.atsoptimizer.dto.HistoryItem;
import com.ceyda.atsoptimizer.service.AnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResponse analyze(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "resumeText", required = false) String resumeText,
            @RequestParam("jobDescription") String jobDescription) {
        return analysisService.analyze(file, resumeText, jobDescription);
    }

    @GetMapping("/analyses")
    public List<HistoryItem> history(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        return analysisService.history(limit);
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
