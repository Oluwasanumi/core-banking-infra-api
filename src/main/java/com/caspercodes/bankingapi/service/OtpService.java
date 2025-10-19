package com.caspercodes.bankingapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
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
}
