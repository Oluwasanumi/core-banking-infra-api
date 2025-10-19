package com.caspercodes.bankingapi.service;

import com.caspercodes.bankingapi.exception.TooManyAttemptsException;
import com.caspercodes.bankingapi.model.OtpData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;

    @Value("${otp.expiration:300}")
    private int otpExpiration;

    @Value("${otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${otp.lock-duration:900}")
    private int lockDuration;

    @Value("${otp.length:6}")
    private int otpLength;

    private static final String OTP_PREFIX = "otp:";
    private static final String LOCK_PREFIX = "otp-lock:";
    private static final SecureRandom random = new SecureRandom();

    public void generateAndSendOtp(String email, OtpData.OtpType type) {
        log.info("Generating OTP for email: {} and type: {}", email, type);

        if (isUserLocked(email)) {
            throw new TooManyAttemptsException("Too many failed attempts. Try again in 15 minutes.");
        }

        String code = generateOtpCode();

        OtpData otpData = OtpData.builder()
                .code(code)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusSeconds(otpExpiration))
                .email(email)
                .type(type)
                .build();

        String key = OTP_PREFIX + email;
        redisTemplate.opsForValue().set(key, otpData, otpExpiration, TimeUnit.SECONDS);
        log.info("OTP generated and stored in Redis for email: {}", email);
    }

    // To check if the user is locked
    private boolean isUserLocked(String email) {
        String lockKey = LOCK_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    // Lock user
    private void lockUser(String email) {
        String lockKey = LOCK_PREFIX + email;
        redisTemplate.opsForValue().set(lockKey, true, lockDuration, TimeUnit.SECONDS);
        log.warn("User with email: {} has been locked for {} seconds due to multiple failed OTP attempts", email, lockDuration);
    }

    // Generate a secure OTP code
    private String generateOtpCode() {
        int bound = (int) Math.pow(10, otpLength);
        int otp = random.nextInt(bound);
        return String.format("%0" + otpLength + "d", otp);
    }

    private void resendOtp(String email) {
        log.info("Resending OTP for email: {}", email);

        String key = OTP_PREFIX + email;
        OtpData existingOtp = (OtpData) redisTemplate.opsForValue().get(key);

        OtpData.OtpType type = (existingOtp != null) ? existingOtp.getType() : OtpData.OtpType.LOGIN;

        generateAndSendOtp(email, type);
    }
}
