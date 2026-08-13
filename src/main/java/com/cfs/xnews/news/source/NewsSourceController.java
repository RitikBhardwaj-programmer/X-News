package com.cfs.xnews.news.source;



import com.cfs.xnews.news.collection.NewsCollectionService;
import com.cfs.xnews.news.collection.dto.CollectedArticle;
import com.cfs.xnews.news.source.dto.CreateNewsSourceRequest;
import com.cfs.xnews.news.source.dto.NewsSourceResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sources")
public class NewsSourceController {

    private final NewsSourceService service;
    private final NewsCollectionService collectionService;

    public NewsSourceController(
            NewsSourceService service,
            NewsCollectionService collectionService
    ) {
        this.service = service;
        this.collectionService = collectionService;
    }

    @PostMapping
    public ResponseEntity<NewsSourceResponse> createSource(
            @Valid @RequestBody CreateNewsSourceRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createSource(request));
    }

    @GetMapping
    public ResponseEntity<List<NewsSourceResponse>> getSources() {

        return ResponseEntity.ok(
                service.getAllSources()
        );
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<NewsSourceResponse>>
    getEnabledSources() {

        return ResponseEntity.ok(
                service.getEnabledSources()
        );
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<Void> enableSource(
            @PathVariable Long id
    ) {

        service.enableSource(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<Void> disableSource(
            @PathVariable Long id
    ) {

        service.disableSource(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/fetch")
    public ResponseEntity<List<CollectedArticle>> fetchSource(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                service.fetchArticles(id)
        );
    }

    @PostMapping("/{id}/collect")
    public ResponseEntity<String> collect(
            @PathVariable Long id
    ) {

        int count =
                collectionService.collectFromSource(id);

        return ResponseEntity.ok(
                "Collected " + count + " new articles"
        );
    }
}
