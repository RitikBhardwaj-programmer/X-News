package com.cfs.xnews.news.source;






import com.cfs.xnews.news.source.dto.CreateNewsSourceRequest;
import com.cfs.xnews.news.source.dto.NewsSourceResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NewsSourceService {

    private final NewsSourceRepository repository;

    public NewsSourceService(
            NewsSourceRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public NewsSourceResponse createSource(
            CreateNewsSourceRequest request
    ) {

        if (repository.existsByUrl(request.url())) {
            throw new RuntimeException(
                    "News source already exists"
            );
        }

        NewsSource source = new NewsSource(
                request.name(),
                request.url(),
                request.type()
        );

        NewsSource saved = repository.save(source);

        return NewsSourceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<NewsSourceResponse> getAllSources() {

        return repository.findAll()
                .stream()
                .map(NewsSourceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NewsSourceResponse> getEnabledSources() {

        return repository.findByEnabledTrue()
                .stream()
                .map(NewsSourceResponse::from)
                .toList();
    }

    @Transactional
    public void enableSource(Long id) {

        NewsSource source = repository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "News source not found"
                        )
                );

        source.setEnabled(true);
    }

    @Transactional
    public void disableSource(Long id) {

        NewsSource source = repository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "News source not found"
                        )
                );

        source.setEnabled(false);
    }
}