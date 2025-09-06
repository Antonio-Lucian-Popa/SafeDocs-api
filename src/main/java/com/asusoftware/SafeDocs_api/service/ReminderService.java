package com.asusoftware.SafeDocs_api.service;

import com.asusoftware.SafeDocs_api.config.AppProperties;
import com.asusoftware.SafeDocs_api.domain.Document;
import com.asusoftware.SafeDocs_api.domain.Reminder;
import com.asusoftware.SafeDocs_api.domain.ReminderChannel;
import com.asusoftware.SafeDocs_api.repo.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ReminderService {
    private final ReminderRepository repo;
    private final AppProperties props;

    public void upsertFor(Document d) {
        if (d.getExpiresAt() == null) return;
        for (Integer offset : props.getReminders().getOffsets()) {
            var when = d.getExpiresAt().minus(offset, ChronoUnit.DAYS);
            // dacă "when" e în trecut, sari peste
            if (when.isBefore(Instant.now())) continue;

            var r = Reminder.builder()
                    .user(d.getUser())
                    .document(d)
                    .remindOffsetDays(offset)
                    .channel(ReminderChannel.EMAIL)
                    .scheduledFor(when)
                    .createdAt(Instant.now())
                    .build();
            try {
                repo.save(r);
            } catch (Exception ignored) {
                // dacă există deja (unique constraint), îl ignorăm
            }
        }
    }
}
