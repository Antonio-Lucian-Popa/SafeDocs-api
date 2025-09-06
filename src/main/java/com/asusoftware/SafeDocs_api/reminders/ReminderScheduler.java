package com.asusoftware.SafeDocs_api.reminders;

import com.asusoftware.SafeDocs_api.domain.Reminder;
import com.asusoftware.SafeDocs_api.repo.ReminderRepository;
import com.asusoftware.SafeDocs_api.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@EnableScheduling
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final ReminderRepository reminders;
    private final MailService mail;

    // rulează din 10 în 10 minute
    @Scheduled(cron = "0 */10 * * * *", zone = "Europe/Bucharest")
    @Transactional
    public void sendDue() {
        var now = Instant.now();
        var due = reminders.findByScheduledForBeforeAndSentAtIsNull(now);
        if (due.isEmpty()) return;

        for (Reminder r : due) {
            try {
                var user = r.getUser();
                var doc  = r.getDocument();

                var fmtDate = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        .withLocale(Locale.getDefault())
                        .withZone(ZoneId.of("Europe/Bucharest"));
                String exp = doc.getExpiresAt() != null ? fmtDate.format(doc.getExpiresAt()) : "N/A";

                String subject = "[SafeDocs] Expiră în curând: " + doc.getTitle();
                String text = """
            Salut %s,

            Documentul "%s" va expira pe %s.
            (Reminder trimis cu %d zile înainte)

            Poți vedea documentul în aplicație.
            """.formatted(user.getDisplayName() != null ? user.getDisplayName() : user.getEmail(),
                        doc.getTitle(), exp, r.getRemindOffsetDays());

                mail.send(user.getEmail(), subject, text);
                r.setSentAt(Instant.now());
                // JPA dirty checking îl va salva la commit (suntem în @Transactional)
            } catch (Exception e) {
                log.warn("Reminder send failed id={} reason={}", r.getId(), e.toString());
            }
        }
    }
}
