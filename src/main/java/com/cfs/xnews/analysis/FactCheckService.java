package com.cfs.xnews.analysis;

import com.cfs.xnews.analysis.dto.CreateFactCheckRequest;
import com.cfs.xnews.event.NewsEvent;
import com.cfs.xnews.event.NewsEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FactCheckService {

    private final FactCheckRepository factCheckRepository;
    private final NewsEventRepository eventRepository;

    public FactCheckService(
            FactCheckRepository factCheckRepository,
            NewsEventRepository eventRepository
    ) {
        this.factCheckRepository = factCheckRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public FactCheck create(
            Long eventId,
            CreateFactCheckRequest request
    ) {

        NewsEvent event =
                eventRepository.findById(eventId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Event not found"
                                )
                        );

        FactCheck factCheck = new FactCheck();

        factCheck.setAgency(request.agency());
        factCheck.setClaim(request.claim());
        factCheck.setVerdict(request.verdict());
        factCheck.setExplanation(request.explanation());
        factCheck.setSourceUrl(request.sourceUrl());
        factCheck.setNewsEvent(event);

        // Update event verification status
        event.setVerificationStatus(
                request.verdict()
        );

        eventRepository.save(event);

        return factCheckRepository.save(factCheck);
    }
}