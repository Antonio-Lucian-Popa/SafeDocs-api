package com.asusoftware.SafeDocs_api.auth;

import com.asusoftware.SafeDocs_api.domain.User;
import com.asusoftware.SafeDocs_api.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentUser {
    private final UserRepository users;

    public User require(@AuthenticationPrincipal SimplePrincipal principal) {
        if (principal == null) throw new IllegalStateException("Unauthenticated");
        UUID id = principal.id();
        return users.findById(id).orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
