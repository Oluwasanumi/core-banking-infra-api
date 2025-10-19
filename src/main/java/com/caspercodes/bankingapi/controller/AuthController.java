package com.caspercodes.bankingapi.controller;

import com.caspercodes.bankingapi.dto.*;
import com.caspercodes.bankingapi.service.AuthService;
import com.caspercodes.bankingapi.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and authentication")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with email and password.")
    public ResponseEntity<OtpResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.info("Register request for email: {}", request.getEmail());
        OtpResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("login")
    @Operation(summary = "User login", description = "Authenticates a user and returns access and refresh tokens.")
    public ResponseEntity<OtpResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("Login request for email: {}", request.getEmail());
        OtpResponseDTO response = authService.login(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("verify-otp")
    @Operation(summary = "Verify OTP", description = "Verifies the OTP sent to the user")
    public ResponseEntity<AuthResponseDTO> verifyOtp(@Valid @RequestBody VerifyOtpRequestDTO request) {
        log.info("OTP verification request for email: {}", request.getEmail());
        AuthResponseDTO response = authService.verifyOtpAndLogin(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(response);
    }

    @PostMapping("resend-otp")
    @Operation(summary = "Resend OTP", description = "Requests a new OTP code")
    public ResponseEntity<OtpResponseDTO> resendOtp(@RequestParam String email) {
        log.info("Resend OTP request for email: {}", email);
        otpService.resendOtp(email);
        return ResponseEntity.ok(OtpResponseDTO.builder()
                .message("New verification code sent to your email")
                .email(email)
                .expiresInMinutes(5)
                .build());
    }

    @PostMapping("refresh-token")
    @Operation(summary = "Refresh access token", description = "Generates a new access token using a valid refresh token.")
    public ResponseEntity<AuthResponseDTO> refresh(@RequestBody RefreshTokenRequestDTO request) {
        log.info("Refresh token request received");
        AuthResponseDTO response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("logout")
    @Operation(summary = "User logout", description = "Revoke refresh token and logout user.")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequestDTO request) {
        log.info("Logout request received");
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }
}
