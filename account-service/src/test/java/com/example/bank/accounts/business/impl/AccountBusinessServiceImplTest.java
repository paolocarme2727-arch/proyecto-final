package com.example.bank.accounts.business.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.business.domain.AccountDomainService;
import com.example.bank.accounts.business.domain.impl.AccountDomainServiceImpl;
import com.example.bank.accounts.business.impl.strategy.BusinessAccountHolderValidationStrategy;
import com.example.bank.accounts.business.impl.strategy.BusinessAccountTypeValidationStrategy;
import com.example.bank.accounts.business.impl.strategy.CreditCardRequirementValidationStrategy;
import com.example.bank.accounts.business.impl.strategy.MinimumOpeningAmountValidationStrategy;
import com.example.bank.accounts.business.impl.strategy.NoOverdueDebtValidationStrategy;
import com.example.bank.accounts.business.impl.strategy.PersonalAccountTypeAvailabilityValidationStrategy;
import com.example.bank.accounts.business.impl.strategy.PymeProfileValidationStrategy;
import com.example.bank.accounts.business.impl.strategy.VipProfileValidationStrategy;
import com.example.bank.accounts.business.impl.strategy.VipSavingsMinimumAverageValidationStrategy;
import com.example.bank.accounts.config.AccountProperties;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.events.CreditDebtStatusCache;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.CustomerProfile;
import com.example.bank.accounts.expose.model.CustomerType;
import com.example.bank.accounts.proxy.CreditProductProxy;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.util.enums.CustomerProfileEnum;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for account lifecycle operations.
 */
class AccountBusinessServiceImplTest {

    private final BankAccountRepository accountRepository = mock(BankAccountRepository.class);
    private final CreditProductProxy creditProductProxy = mock(CreditProductProxy.class);
    private final CreditDebtStatusCache creditDebtStatusCache = mock(CreditDebtStatusCache.class);
    private final AccountProperties properties = AccountBusinessTestSupport.properties();
    private final AccountDomainService accountDomainService = new AccountDomainServiceImpl(accountRepository);
    private final AccountBusinessServiceImpl accountBusinessServiceImpl = new AccountBusinessServiceImpl(
            accountRepository,
            properties,
            accountDomainService,
            List.of(
                    new MinimumOpeningAmountValidationStrategy(properties),
                    new VipSavingsMinimumAverageValidationStrategy(properties),
                    new BusinessAccountTypeValidationStrategy(),
                    new BusinessAccountHolderValidationStrategy(),
                    new VipProfileValidationStrategy(),
                    new PymeProfileValidationStrategy()),
            List.of(
                    new NoOverdueDebtValidationStrategy(creditDebtStatusCache),
                    new CreditCardRequirementValidationStrategy(creditProductProxy),
                    new PersonalAccountTypeAvailabilityValidationStrategy(accountRepository)));

    @BeforeEach
    void useImmediateScheduler() {
        AccountBusinessTestSupport.useImmediateScheduler();
    }

    @AfterEach
    void resetSchedulers() {
        AccountBusinessTestSupport.resetSchedulers();
    }

    @Test
    void givenPymeCustomerWithCreditCard_whenCreateCheckingAccount_thenHasNoMaintenanceFee() {
        AccountRequest request = new AccountRequest("business-1", CustomerType.BUSINESS, AccountType.CHECKING)
                .customerProfile(CustomerProfile.PYME)
                .holders(List.of("holder-1"))
                .initialBalance(BigDecimal.valueOf(100));

        when(creditProductProxy.customerHasCreditCard("business-1")).thenReturn(io.reactivex.rxjava3.core.Single.just(Boolean.TRUE));
        when(creditDebtStatusCache.hasOverdueDebt("business-1")).thenReturn(io.reactivex.rxjava3.core.Single.just(Boolean.FALSE));
        when(accountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountBusinessServiceImpl.create(request)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(account -> {
                    assertThat(account.getMaintenanceFee()).isEqualByComparingTo(BigDecimal.ZERO);
                    assertThat(account.getCustomerProfileEnum()).isEqualTo(CustomerProfileEnum.PYME);
                    return true;
                });
    }
}
