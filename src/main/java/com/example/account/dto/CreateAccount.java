package com.example.account.dto;

import lombok.*;

import java.time.LocalDateTime;

public class CreateAccount {
    @Getter
    @Setter
    public static class request{
        private Long userId;
        private Long initialBalance;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class response{
        private Long userId;
        private String accountNumber;
        private LocalDateTime registeredAt;
    }
}
