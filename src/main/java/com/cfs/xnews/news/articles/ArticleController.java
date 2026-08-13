package com.cfs.xnews.news.articles;



import com.cfs.xnews.news.articles.dto.ArticleResponse;
import com.cfs.xnews.news.articles.dto.CreateArticleRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(
            ArticleService articleService
    ) {
        this.articleService = articleService;
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> createArticle(
            @Valid @RequestBody CreateArticleRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        articleService.createArticle(request)
                );
    }

    @GetMapping
    public ResponseEntity<List<ArticleResponse>> getArticles() {

        return ResponseEntity.ok(
                articleService.getAllArticles()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticle(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                articleService.getArticle(id)
        );
    }
}
