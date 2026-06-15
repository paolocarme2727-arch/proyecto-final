package com.example.bank.accounts.expose.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.business.BankAccountService;
import com.example.bank.accounts.mappers.BankAccountMapper;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.BankAccount;
import com.example.bank.accounts.expose.model.CustomerProfile;
import com.example.bank.accounts.expose.model.CustomerType;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for account REST status mapping.
 */
class BankAccountApiImplTest {

    private final BankAccountService accountService = mock(BankAccountService.class);
    private final BankAccountApiImpl api = new BankAccountApiImpl(accountService, new BankAccountMapper());

    /**
     * Verifies that business rule rejections are exposed as client errors.
     */
    @Test
    void givenVipSavingsWithoutCreditCard_whenCreateAccount_thenReturnsBadRequest() {
        AccountRequest request = new AccountRequest("vip-customer-1", CustomerType.PERSONAL, AccountType.SAVINGS)
                .customerProfile(CustomerProfile.VIP)
                .initialBalance(BigDecimal.valueOf(700))
                .minimumOpeningAmount(BigDecimal.ZERO);

        when(accountService.create(any(AccountRequest.class))).thenReturn(Single.error(new RuntimeException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer must have a credit card"))));

        ResponseEntity<BankAccount> response = api.createAccount(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

