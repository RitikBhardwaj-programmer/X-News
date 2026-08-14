package com.cfs.xnews.news.articles;



import com.cfs.xnews.kafka.ArticleEvent;
import com.cfs.xnews.kafka.KafkaProducer;
import com.cfs.xnews.news.articles.dto.ArticleResponse;
import com.cfs.xnews.news.articles.dto.CreateArticleRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArticleService {

    private final KafkaProducer kafkaProducer;
    private final ArticleRepository articleRepository;

    public ArticleService(
            KafkaProducer kafkaProducer, ArticleRepository articleRepository
    ) {
        this.kafkaProducer = kafkaProducer;
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

        // Save first → generates article ID
        Article saved = articleRepository.save(article);

        // Create Kafka event using generated ID
        ArticleEvent event = new ArticleEvent(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getUrl(),
                saved.getSource(),
                saved.getPublishedAt()
        );

        // Publish after saving
        kafkaProducer.publishArticle(event);

        return ArticleResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> getAllArticles() {

        return articleRepository.findAll()
                .stream()
                .map(ArticleResponse::from)
                .toList();
    }
    @Transactional
    public void deleteAllArticles() {
        articleRepository.deleteAllInBatch();
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
