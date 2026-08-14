package com.cfs.xnews.event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsEventRepository
        extends JpaRepository<NewsEvent, Long> {
}