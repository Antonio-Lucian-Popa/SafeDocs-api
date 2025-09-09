package com.asusoftware.SafeDocs_api.web;

import com.asusoftware.SafeDocs_api.auth.CurrentUser;
import com.asusoftware.SafeDocs_api.auth.SimplePrincipal;
import com.asusoftware.SafeDocs_api.domain.Document;
import com.asusoftware.SafeDocs_api.dto.DocumentListItem;
import com.asusoftware.SafeDocs_api.repo.DocumentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentsSearchController {

    private final CurrentUser currentUser;
    private final DocumentRepository docs;

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/search")
    public ResponseEntity<List<DocumentListItem>> search(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tagKey,
            @RequestParam(required = false) String tagValue
    ) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_auth");

        if (tagKey != null && tagValue != null) {
            var rows = docs.findByTagRaw(userId, tagKey, tagValue);
            var list = rows.stream().map(r -> new DocumentListItem(
                    (UUID) r[0], (String) r[1], (UUID) r[2],
                    (String) r[3], r[4] == null ? null : ((Number) r[4]).longValue(),
                    (java.time.Instant) r[5], (java.time.Instant) r[6]
            )).toList();
            return ResponseEntity.ok(list);
        }

        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(docs.searchByTitle(userId, q));
        }

        return ResponseEntity.ok(docs.findTop50ForUser(userId, org.springframework.data.domain.PageRequest.of(0, 50)));
    }


}
