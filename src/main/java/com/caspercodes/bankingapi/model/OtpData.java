package com.caspercodes.bankingapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpData implements Serializable {

    private String code;
    private Integer attempts;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
    private String email;
    private OtpType type;

    public enum OtpType {
        REGISTRATION,
        LOGIN,
        PASSWORD_RESET,
    }
}
