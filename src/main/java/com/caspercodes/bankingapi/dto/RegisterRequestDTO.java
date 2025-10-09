package com.caspercodes.bankingapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 150, message = "First name must be between 2 and 150 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 150, message = "Last name must be between 2 and 150 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+!=]).*$",
            message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, and one special character")
    private String password;

    @Pattern(regexp = "^(?:\\+?[1-9]\\d{1,14}|0\\d{7,14})$",
            message = "Phone number must be valid")
    private String phoneNumber;
}

