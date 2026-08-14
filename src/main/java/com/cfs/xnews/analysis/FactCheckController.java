package com.cfs.xnews.analysis;

import com.cfs.xnews.analysis.dto.CreateFactCheckRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class FactCheckController {

    private final FactCheckService factCheckService;

    public FactCheckController(
            FactCheckService factCheckService
    ) {
        this.factCheckService = factCheckService;
    }

    @PostMapping("/{eventId}/fact-checks")
    public ResponseEntity<FactCheck> createFactCheck(
            @PathVariable Long eventId,
            @RequestBody CreateFactCheckRequest request
    ) {

        return ResponseEntity.ok(
                factCheckService.create(
                        eventId,
                        request
                )
        );
    }
}