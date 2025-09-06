package com.asusoftware.SafeDocs_api.service;

import com.asusoftware.SafeDocs_api.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender sender;
    private final AppProperties props;

    public void send(String to, String subject, String text) {
        if (!props.getMail().isEnabled()) return; // fail-safe dev
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.getMail().getFrom());
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        sender.send(msg);
    }
}