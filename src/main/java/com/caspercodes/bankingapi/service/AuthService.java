package com.caspercodes.bankingapi.service;

import com.caspercodes.bankingapi.dto.AuthResponseDTO;
import com.caspercodes.bankingapi.dto.LoginRequestDTO;
import com.caspercodes.bankingapi.dto.OtpResponseDTO;
import com.caspercodes.bankingapi.dto.RegisterRequestDTO;
import com.caspercodes.bankingapi.exception.EmailAlreadyExistsException;
import com.caspercodes.bankingapi.exception.InvalidTokenException;
import com.caspercodes.bankingapi.exception.TokenExpiredException;
import com.caspercodes.bankingapi.model.OtpData;
import com.caspercodes.bankingapi.model.RefreshToken;
import com.caspercodes.bankingapi.model.User;
import com.caspercodes.bankingapi.repository.RefreshTokenRepository;
import com.caspercodes.bankingapi.repository.UserRepository;
import com.caspercodes.bankingapi.security.CustomUserDetails;
import com.caspercodes.bankingapi.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;

    @Transactional
    public OtpResponseDTO register(RegisterRequestDTO request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email {} already exists", request.getEmail());
            throw new EmailAlreadyExistsException("Email already exists " + request.getEmail());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .isVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User with ID: {} registered successfully", savedUser.getId());

        otpService.generateAndSendOtp(savedUser.getEmail(), OtpData.OtpType.REGISTRATION);

        return OtpResponseDTO.builder()
                .message("Registration successful! Please check your email for verification code.")
                .email(maskEmail(savedUser.getEmail()))
                .expiresInMinutes(5)
                .build();
    }

    @Transactional
    public OtpResponseDTO login(LoginRequestDTO request) {
        log.info("Login attempt for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        log.info("User with email: {} authenticated successfully", request.getEmail());

        otpService.generateAndSendOtp(request.getEmail(), OtpData.OtpType.LOGIN);

        return OtpResponseDTO.builder()
                .message("Login successful! Please check your email for verification code.")
                .email(maskEmail(request.getEmail()))
                .expiresInMinutes(5)
                .build();
    }

    @Transactional
    public AuthResponseDTO verifyOtpAndLogin(String email, String otp) {
        log.info("OTP verification attempt for email: {}", email);

        otpService.verifyOtp(email, otp);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getIsVerified()) {
            user.setIsVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            log.info("User verified: {}", email);
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        UserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        saveRefreshToken(user, refreshToken);

        log.info("Tokens generated for verified user: {}", email);
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponseDTO refreshAccessToken(String refreshTokenString) {
        log.debug("Attempting to refresh access token");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.getRevoked()) {
            log.warn("Attempting use of refresh token that has been revoked");
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Attempting use of refresh token that has expired");
            throw new TokenExpiredException("Refresh token has expired");
        }

        User user = refreshToken.getUser();
        UserDetails userDetails = new CustomUserDetails(user);

        String newAccessToken = jwtUtil.generateToken(userDetails);
        String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        saveRefreshToken(user, newRefreshToken);

        log.info("Access token refreshed successfully for user: {}", user.getEmail());
        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String refreshTokenString) {
        log.debug("Attempting to logout");

        refreshTokenRepository.findByToken(refreshTokenString)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    log.info("User logged out successfully. Refresh token revoked.");
                });
    }

    private void saveRefreshToken(User user, String tokenString) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenString)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(refreshToken);
        log.debug("Refresh token saved successfully for user: {}", user.getEmail());
    }

    private AuthResponseDTO buildAuthResponse(User user, String accessToken, String refreshToken) {
        AuthResponseDTO.UserInfo userInfo = AuthResponseDTO.UserInfo.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900L) // 15 minutes in seconds
                .user(userInfo)
                .build();

    }

    private String maskEmail(String email) {
        String[] parts = email.split("@");
        if (parts.length == 2) {
            String username = parts[0];
            String domain = parts[1];
            if (username.length() > 2) {
                String maskedUsername = username.charAt(0) + "***" + username.charAt(username.length() - 1);
                return maskedUsername + "@" + domain;
            }
        }
        return email;
    }
}