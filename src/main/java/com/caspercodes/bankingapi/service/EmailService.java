package com.caspercodes.bankingapi.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.View;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final View error;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String to, String otp, int expiryMinutes) {
        log.info("Preparing to send OTP email to {}", to);

        try {
            Context context = new Context();
            context.setVariable("greeting", "Hello!");
            context.setVariable("message", "We received a request to verify your email address. " +
                    "Please use the verification code below to complete your registration.");
            context.setVariable("otp", otp);
            context.setVariable("expiryMinutes", expiryMinutes);
            context.setVariable("subject", "Your Verification Code");

            String htmlContent = templateEngine.process("email/otp-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("Your Banking API Verification Code");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendLoginOtpEmail(String to, String otp, int expiryMinutes) {
        log.info("Preparing to send login OTP email to: {}", to);

        try {
            Context context = new Context();
            context.setVariable("greeting", "Welcome back!");
            context.setVariable("message",
                    "We detected a login attempt to your account. " + "Please use the verification code below to complete your login.");
            context.setVariable("otp", otp);
            context.setVariable("expiryMinutes", expiryMinutes);
            context.setVariable("subject", "Your Login Verification Code");

            String htmlContent = templateEngine.process("email/otp-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("Your Banking API Login Code");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Login OTP email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send login OTP email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String firstName) {
        log.info("Preparing to send welcome email to: {}", to);

        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("subject", "Welcome to Banking API");

            String htmlContent = templateEngine.process("email/welcome-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("Welcome to Banking API!");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Welcome email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }
}
