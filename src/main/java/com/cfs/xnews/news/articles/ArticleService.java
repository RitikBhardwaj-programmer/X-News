package com.cfs.xnews.news.articles;



import com.cfs.xnews.news.articles.dto.ArticleResponse;
import com.cfs.xnews.news.articles.dto.CreateArticleRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(
            ArticleRepository articleRepository
    ) {
        this.articleRepository = articleRepository;
    }

    @Transactional
    public ArticleResponse createArticle(
            CreateArticleRequest request
    ) {

        if (articleRepository.existsByUrl(request.url())) {
            throw new RuntimeException(
                    "Article already exists"
            );
        }

        Article article = new Article(
                request.title(),
                request.description(),
                request.url(),
                request.source(),
                request.publishedAt()
        );

        Article saved = articleRepository.save(article);

        return ArticleResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> getAllArticles() {

        return articleRepository.findAll()
                .stream()
                .map(ArticleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse getArticle(Long id) {

        Article article = articleRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Article not found"
                        )
                );

        return ArticleResponse.from(article);
    }
}
