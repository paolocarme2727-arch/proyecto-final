package com.example.bank.accounts.business.impl.strategy;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.CustomerType;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for personal account type availability validation.
 */
class PersonalAccountTypeAvailabilityValidationStrategyTest {

    private final BankAccountRepository accountRepository = mock(BankAccountRepository.class);
    private final PersonalAccountTypeAvailabilityValidationStrategy strategy =
            new PersonalAccountTypeAvailabilityValidationStrategy(accountRepository);

    @Test
    void givenDuplicatePersonalSavingsAccount_whenValidate_thenRejectsRequest() {
        AccountRequest request = new AccountRequest("customer-1", CustomerType.PERSONAL, AccountType.SAVINGS);

        when(accountRepository.existsByCustomerIdAndType("customer-1", AccountTypeEnum.SAVINGS))
                .thenReturn(Boolean.TRUE);

        strategy.validate(request)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(ResponseStatusException.class);
    }

    @Test
    void givenPersonalFixedTermAccount_whenValidate_thenDoesNotCheckDuplicateType() {
        AccountRequest request = new AccountRequest("customer-1", CustomerType.PERSONAL, AccountType.FIXED_TERM)
                .initialBalance(BigDecimal.valueOf(100))
                .movementDay(LocalDate.now().getDayOfMonth());

        strategy.validate(request)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors();
        verify(accountRepository, never()).existsByCustomerIdAndType(
                eq("customer-1"),
                eq(AccountTypeEnum.FIXED_TERM));
    }
}
