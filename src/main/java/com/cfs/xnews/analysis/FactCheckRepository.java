package com.cfs.xnews.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FactCheckRepository
        extends JpaRepository<FactCheck, Long> {
}