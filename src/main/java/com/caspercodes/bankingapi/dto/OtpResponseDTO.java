package com.caspercodes.bankingapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OtpResponseDTO {

    private String message;

    private String email;

    private Integer expiresInMinutes;
}
