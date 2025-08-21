package com.example.chat_api.magic;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String baseUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine,
                        @Value("${app.base-url}") String baseUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.baseUrl = baseUrl;
    }

    public void sendMagicLink(String to, String token) {
        String link = baseUrl + "/magic/verify?token=" + token;

        Context ctx = new Context();
        ctx.setVariable("magicLink", link);
        String html = templateEngine.process("mail/magic-link", ctx);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Your magic link");
            helper.setText(html, true); // HTML
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send magic link", e);
        }
    }
}
