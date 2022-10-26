package com.example.account.dto;

import com.example.account.type.TransactionResultType;
import lombok.*;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

public class UseBalance {
    /**
     * {
     *      "userId":1,
     *      "accountNumber":"1000000000",
     *      "amount":1000
     * }
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @Builder
    public static class Request{
        @NotNull
        @Min(1)
        private Long userId;

        @NotBlank
        @Size(min=10, max=10)
        private String accountNumber;

        @NotNull
        @Min(10)
        @Max(100_000_000_0)
        private Long amount;
    }

    /**
     * {
     *     "accountNumber":"1234567890",
     *     "transactionResult":"5",
     *     "transactionId":"c15612gads1g26w8ew984hg22hggg",
     *     "amount":1000,
     *     "transactedAt":"2022-06-01T23:26:14.671859"
     * }
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response{
        private String accountNumber;
        private TransactionResultType transactionResult;
        private String transactionId;
        private Long amount;
        private LocalDateTime registeredAt;

        public static Response from(TransactionDto transactionDto){
            return Response.builder()
                    .accountNumber(transactionDto.getAccountNumber())
                    .transactionResult(transactionDto.getTransactionResultType())
                    .transactionId(transactionDto.getTransactionId())
                    .amount(transactionDto.getAmount())
                    .registeredAt(transactionDto.getTransactedAt())
                    .build();
        }
    }
}
