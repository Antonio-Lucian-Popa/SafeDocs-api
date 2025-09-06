package com.asusoftware.SafeDocs_api.web;

import com.asusoftware.SafeDocs_api.auth.CurrentUser;
import com.asusoftware.SafeDocs_api.auth.SimplePrincipal;
import com.asusoftware.SafeDocs_api.domain.Document;
import com.asusoftware.SafeDocs_api.repo.DocumentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentsSearchController {

    private final CurrentUser currentUser;
    private final DocumentRepository docs;

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/search")
    public ResponseEntity<List<Document>> search(@AuthenticationPrincipal SimplePrincipal me,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(required = false) String tagKey,
                                                 @RequestParam(required = false) String tagValue) {
        var user = currentUser.require(me);

        // Simplu: dacă ai tagKey + tagValue, facem query nativ pe JSONB; altfel căutăm în titlu
        if (tagKey != null && tagValue != null) {
            var list = em.createNativeQuery("""
          SELECT * FROM documents
          WHERE user_id = :uid
            AND tags ->> :k = :v
          ORDER BY created_at DESC
          """, Document.class)
                    .setParameter("uid", user.getId())
                    .setParameter("k", tagKey)
                    .setParameter("v", tagValue)
                    .getResultList();
            return ResponseEntity.ok(list);
        }

        if (q != null && !q.isBlank()) {
            var list = em.createNativeQuery("""
          SELECT * FROM documents
          WHERE user_id = :uid
            AND unaccent(lower(title)) LIKE unaccent(lower(:q))
          ORDER BY created_at DESC
          """, Document.class)
                    .setParameter("uid", user.getId())
                    .setParameter("q", "%" + q + "%")
                    .getResultList();
            return ResponseEntity.ok(list);
        }

        // fallback: ultimele documente
        var list = em.createNativeQuery("""
        SELECT * FROM documents
        WHERE user_id = :uid
        ORDER BY created_at DESC
        LIMIT 50
        """, Document.class)
                .setParameter("uid", user.getId())
                .getResultList();
        return ResponseEntity.ok(list);
    }
}
