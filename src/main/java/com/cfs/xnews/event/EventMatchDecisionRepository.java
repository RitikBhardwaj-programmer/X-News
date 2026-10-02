package com.cfs.xnews.event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventMatchDecisionRepository
        extends JpaRepository<EventMatchDecision, Long> {
}
