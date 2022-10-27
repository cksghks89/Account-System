package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        Account account = Account.builder()
                .accountUser(accountUser)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.S)
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build()
                );
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.useBalance(
                12L,
                "1000000000",
                200L
        );

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(9800L, captor.getValue().getBalanceSnapshot());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(TransactionResultType.S,
                transactionDto.getTransactionResultType());
        assertEquals(TransactionType.USE, transactionDto.getTransactionType());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void useBalance_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L,
                        "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
    void useBalance_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L,
                        "1234567890", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 잔액 사용 실패")
    void useBalance_UserUnMatch() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        AccountUser harry = AccountUser.builder()
                .id(13L)
                .name("Harry").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(harry)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L,
                        "1234567890", 1000L));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UNMATCHED, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌는 사용할 수 없다.")
    void deleteAccountFailed_alreadyUnregistered() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .balance(1L)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .accountNumber("1000000012")
                        .build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L,
                        "1234567890",
                        1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우")
    void exceedAmount_UseBalance() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        Account account = Account.builder()
                .accountUser(accountUser)
                .balance(100L)
                .accountNumber("1000000012")
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        //then
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(12L,
                        "1234567890", 1000L));

        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void saveFailedUseTransaction() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();
        Account account = Account.builder()
                .accountUser(accountUser)
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
                .accountNumber("1000000012")
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build()
                );
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        transactionService.saveFailedUseTransaction("1234567890", 200L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(TransactionResultType.F, captor.getValue().getTransactionResultType());
    }

    @Test
    void successCancelBalance() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.CANCEL)
                        .transactionResultType(TransactionResultType.S)
                        .transactedAt(LocalDateTime.now())
                        .transactionId("transactionIdForCancel")
                        .amount(1000L)
                        .balanceSnapshot(10000L)
                        .build()
                );
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.cancelBalance(
                "transactionId",
                "1000000000",
                1000L
        );

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1000L, captor.getValue().getAmount());
        assertEquals(10000L + 1000L, captor.getValue().getBalanceSnapshot());
        assertEquals(TransactionResultType.S, transactionDto.getTransactionResultType());
        assertEquals(TransactionType.CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패")
    void cancelTransactionAccount_AccountNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1234567890", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 거래 없음 - 잔액 사용 취소 실패")
    void cancelTransactionAccount_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1234567890", 1000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 잔액 사용 취소 실패")
    void cancelTransaction_UserUnMatch() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        AccountUser harry = AccountUser.builder()
                .id(13L)
                .name("harry")
                .build();

        Account pobiAccount = Account.builder()
                .accountStatus(AccountStatus.IN_USE)
                .accountUser(pobi)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        Account harryAccount = Account.builder()
                .accountStatus(AccountStatus.IN_USE)
                .accountUser(harry)
                .balance(10000L)
                .accountNumber("1000000013")
                .build();

        Transaction transaction = Transaction.builder()
                .account(pobiAccount)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .transactedAt(LocalDateTime.now())
                .transactionId("transactionIdForCancel")
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(harryAccount));

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1234567890", 1000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래금액과 취소금액이 다름 - 잔액 사용 취소 실패")
    void cancelTransaction_CancelMustFully() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        Account pobiAccount = Account.builder()
                .accountStatus(AccountStatus.IN_USE)
                .accountUser(pobi)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        Transaction transaction = Transaction.builder()
                .account(pobiAccount)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .transactedAt(LocalDateTime.now())
                .transactionId("transactionIdForCancel")
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(pobiAccount));

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1234567890", 2000L));

        //then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 날짜가 너무 오래됨 - 잔액 사용 취소 실패")
    void cancelTransaction_TooOldOrderToCancel() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        Account pobiAccount = Account.builder()
                .accountStatus(AccountStatus.IN_USE)
                .accountUser(pobi)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        Transaction transaction = Transaction.builder()
                .account(pobiAccount)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .transactionId("transactionIdForCancel")
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(pobiAccount));

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1234567890", 1000L));

        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
    }
}
