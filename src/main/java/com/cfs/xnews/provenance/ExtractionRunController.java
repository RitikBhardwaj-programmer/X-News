package com.cfs.xnews.provenance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Resolves a run id (e.g. an event's summaryRunId) to the model, prompt
// version and code version that produced the output.
@RestController
@RequestMapping("/api/v1/runs")
public class ExtractionRunController {

    private final ExtractionRunRepository repository;

    public ExtractionRunController(ExtractionRunRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExtractionRun> getRun(@PathVariable Long id) {
        return ResponseEntity.of(repository.findById(id));
    }
}
