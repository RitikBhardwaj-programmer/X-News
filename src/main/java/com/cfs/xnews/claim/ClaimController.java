package com.cfs.xnews.claim;

import com.cfs.xnews.event.NewsEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ClaimController {

    private final ClaimService claimService;
    private final NewsEventRepository eventRepository;

    public ClaimController(ClaimService claimService, NewsEventRepository eventRepository) {
        this.claimService = claimService;
        this.eventRepository = eventRepository;
    }

    public record ReviewRequest(String decision, String note) {
    }

    // Public (signed-in users): only approved claims are ever shown.
    @GetMapping("/api/v1/events/{id}/claims")
    public ResponseEntity<List<ClaimService.ClaimView>> getEventClaims(@PathVariable Long id) {

        if (!eventRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(claimService.list("approved", id, ClaimService.MAX_LIST));
    }

    // Admin only (SecurityConfig: /api/v1/admin/**).
    @GetMapping("/api/v1/admin/claims")
    public List<ClaimService.ClaimView> listForReview(
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return claimService.list(status, null, limit);
    }

    @PostMapping("/api/v1/admin/claims/{id}/review")
    public ResponseEntity<ClaimReview> review(
            @PathVariable Long id,
            @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.of(claimService.review(id, request.decision(), request.note(), authentication.getName()));
    }
}
