package com.caspercodes.bankingapi.controller;

import com.caspercodes.bankingapi.model.User;
import com.caspercodes.bankingapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test/")
@RequiredArgsConstructor
public class TestController {
    private final UserRepository userRepository;

    @GetMapping("create-user")
    public String createDummyUser() {
        User dummyUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("dummyPassword123")
                .phoneNumber("+1234567890")
                .build();

        User savedUser = userRepository.save(dummyUser);
        return "Created user with ID: " + savedUser.getId();
    }

    @GetMapping("all-users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("user/{email}")
    public User getUserByEmail(@PathVariable String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}
