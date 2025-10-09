package com.caspercodes.bankingapi.controller;

import com.caspercodes.bankingapi.dto.RegisterRequestDTO;
import com.caspercodes.bankingapi.model.RefreshToken;
import com.caspercodes.bankingapi.model.User;
import com.caspercodes.bankingapi.repository.RefreshTokenRepository;
import com.caspercodes.bankingapi.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/test/")
public class AuthTestController {
    private final UserRepository userRepository;
    private final RefreshTokenRepository tokenRepository;

    @PostMapping("register")
    public String testRegister(@Valid @RequestBody RegisterRequestDTO request) {
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword())
                .phoneNumber(request.getPhoneNumber())
                .build();

        User savedUser = userRepository.save(user);

        RefreshToken token = RefreshToken.builder()
                .token("Sample token" + savedUser.getId())
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        tokenRepository.save(token);
        return "Created user with ID: " + savedUser.getId() + " and  refresh token with token ID: " + token.getId();
    }

    @GetMapping("tokens")
    public List<RefreshToken> getTokens() {
        return tokenRepository.findAll();
    }
}
