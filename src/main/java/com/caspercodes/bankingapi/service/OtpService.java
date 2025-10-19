package com.caspercodes.bankingapi.service;

import com.caspercodes.bankingapi.exception.InvalidOtpException;
import com.caspercodes.bankingapi.exception.OtpExpiredException;
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

    private static final String OTP_PREFIX = "otp:";

    private static final String LOCK_PREFIX = "otp-lock:";

    private static final SecureRandom random = new SecureRandom();

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


        if (type == OtpData.OtpType.REGISTRATION) {
            emailService.sendOtpEmail(email, code, otpExpiration / 60);
        } else if (type == OtpData.OtpType.LOGIN) {
            emailService.sendLoginOtpEmail(email, code, otpExpiration / 60);
        }
    }

    public void verifyOtp(String email, String code) {
        log.info("Verifying OTP for email: {}", email);


        if (isUserLocked(email)) {
            throw new TooManyAttemptsException(
                    "Too many failed attempts. Please try again in 15 minutes."
            );
        }


        String key = OTP_PREFIX + email;
        OtpData otpData = (OtpData) redisTemplate.opsForValue().get(key);


        if (otpData == null) {
            log.warn("OTP not found or expired for: {}", email);
            throw new OtpExpiredException("OTP has expired. Please request a new one.");
        }


        if (otpData.getExpiredAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for: {}", email);
            redisTemplate.delete(key);
            throw new OtpExpiredException("OTP has expired. Please request a new one.");
        }


        if (otpData.getAttempts() >= maxAttempts) {
            log.warn("Max OTP attempts exceeded for: {}", email);
            lockUser(email);
            redisTemplate.delete(key);
            throw new TooManyAttemptsException(
                    "Too many failed attempts. Account locked for 15 minutes."
            );
        }


        if (!otpData.getCode().equals(code)) {
            log.warn("Invalid OTP attempt for: {}. Attempts: {}", email, otpData.getAttempts() + 1);


            otpData.setAttempts(otpData.getAttempts() + 1);


            long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(key, otpData, ttl, TimeUnit.SECONDS);

            throw new InvalidOtpException(
                    String.format("Invalid OTP. %d attempts remaining.",
                            maxAttempts - otpData.getAttempts())
            );
        }


        redisTemplate.delete(key);
        log.info("OTP verified successfully for: {}", email);
    }


    public void resendOtp(String email) {
        log.info("Resending OTP for email: {}", email);

        String key = OTP_PREFIX + email;
        OtpData existingOtp = (OtpData) redisTemplate.opsForValue().get(key);

        OtpData.OtpType type = (existingOtp != null) ? existingOtp.getType() : OtpData.OtpType.LOGIN;

        generateAndSendOtp(email, type);
    }


    private boolean isUserLocked(String email) {
        String lockKey = LOCK_PREFIX + email;
        return redisTemplate.hasKey(lockKey);
    }


    private void lockUser(String email) {
        String lockKey = LOCK_PREFIX + email;
        redisTemplate.opsForValue().set(lockKey, true, lockDuration, TimeUnit.SECONDS);
        log.warn("User with email: {} has been locked for {} seconds due to multiple failed OTP attempts", email, lockDuration);
    }


    private String generateOtpCode() {
        int bound = (int) Math.pow(10, otpLength);
        int otp = random.nextInt(bound);
        return String.format("%0" + otpLength + "d", otp);
    }
}
