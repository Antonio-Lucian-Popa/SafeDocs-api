package com.asusoftware.SafeDocs_api.auth;

import com.asusoftware.SafeDocs_api.domain.AuthProvider;
import com.asusoftware.SafeDocs_api.domain.User;
import com.asusoftware.SafeDocs_api.repo.UserRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleTokenVerifier googleVerifier;
    private final UserRepository users;
    private final JwtService jwt;

    public record GoogleLoginRequest(@NotBlank String idToken) {}
    public record AuthResponse(String accessToken) {}

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@RequestBody GoogleLoginRequest body) throws Exception {
        var payload = googleVerifier.verify(body.idToken());
        if (payload == null) return ResponseEntity.status(401).build();

        String email = payload.getEmail();
        String sub   = payload.getSubject();
        String name  = (String) payload.get("name");

        var user = users.findByGoogleSub(sub).orElseGet(() ->
                users.findByEmail(email).orElseGet(() -> {
                    var u = User.builder()
                            .email(email)
                            .displayName(name)
                            .provider(AuthProvider.GOOGLE)
                            .googleSub(sub)
                            .active(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return users.save(u);
                })
        );
        if (user.getGoogleSub() == null) { user.setGoogleSub(sub); user.setProvider(AuthProvider.GOOGLE); users.save(user); }

        var token = jwt.issueAccess(user.getId(), user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
