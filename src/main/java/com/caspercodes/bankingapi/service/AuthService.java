package com.caspercodes.bankingapi.service;

import com.caspercodes.bankingapi.dto.AuthResponseDTO;
import com.caspercodes.bankingapi.dto.LoginRequestDTO;
import com.caspercodes.bankingapi.dto.RegisterRequestDTO;
import com.caspercodes.bankingapi.exception.EmailAlreadyExistsException;
import com.caspercodes.bankingapi.exception.InvalidTokenException;
import com.caspercodes.bankingapi.exception.TokenExpiredException;
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
    private AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
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
                .build();

        User savedUser = userRepository.save(user);
        log.info("User with ID: {} registered successfully", savedUser.getId());

        // Generate tokens
        UserDetails userDetails = new CustomUserDetails(savedUser);
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        saveRefreshToken(savedUser, refreshToken);
        return buildAuthResponse(savedUser, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Extract user from authentication
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        user.setLastLoginAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        log.info("User with email: {} logged in successfully", user.getEmail());

        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        saveRefreshToken(user, refreshToken);
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
}