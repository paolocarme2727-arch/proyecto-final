package com.example.bank.accounts.business.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.domain.DebitCard;
import com.example.bank.accounts.domain.policy.MovementRules;
import com.example.bank.accounts.events.CreditDebtStatusCache;
import com.example.bank.accounts.events.DebitCardEventPublisher;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.CustomerProfile;
import com.example.bank.accounts.expose.model.CustomerType;
import com.example.bank.accounts.expose.model.DebitCardPaymentRequest;
import com.example.bank.accounts.expose.model.DebitCardRequest;
import com.example.bank.accounts.expose.model.MoneyRequest;
import com.example.bank.accounts.expose.model.TransferRequest;
import com.example.bank.accounts.proxy.CreditProductProxy;
import com.example.bank.accounts.repository.AccountMovementRepository;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.repository.DebitCardRepository;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import com.example.bank.accounts.util.enums.CustomerProfileEnum;
import com.example.bank.accounts.util.enums.MovementTypeEnum;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Business flow tests that verify account rules across services in the business layer.
 */
class AccountBusinessFlowTest {

    private final BankAccountRepository accountRepository = mock(BankAccountRepository.class);
    private final AccountMovementRepository movementRepository = mock(AccountMovementRepository.class);
    private final DebitCardRepository debitCardRepository = mock(DebitCardRepository.class);
    private final CreditProductProxy creditProductProxy = mock(CreditProductProxy.class);
    private final CreditDebtStatusCache creditDebtStatusCache = mock(CreditDebtStatusCache.class);
    private final DebitCardEventPublisher debitCardEventPublisher = mock(DebitCardEventPublisher.class);
    private final Map<String, BankAccount> accounts = new HashMap<>();
    private final Map<String, AccountMovement> movements = new HashMap<>();
    private final Map<String, DebitCard> debitCards = new HashMap<>();
    private final Map<String, Boolean> customerCreditCards = new HashMap<>();
    private final Map<String, Boolean> customerOverdueDebt = new HashMap<>();

    private AccountBusinessServiceImpl accountService;
    private MovementBusinessServiceImpl movementService;
    private TransferBusinessServiceImpl transferService;
    private DebitCardBusinessServiceImpl debitCardService;
    private int accountSequence;
    private int movementSequence;
    private int debitCardSequence;

    @BeforeEach
    void setUp() {
        AccountBusinessTestSupport.useImmediateScheduler();
        AccountProperties properties = AccountBusinessTestSupport.properties();
        AccountDomainService accountDomainService = new AccountDomainServiceImpl(accountRepository);
        MovementRules movementRules = new MovementRules(properties);

        accountService = new AccountBusinessServiceImpl(
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
        movementService = new MovementBusinessServiceImpl(
                accountRepository,
                movementRepository,
                movementRules,
                accountDomainService);
        transferService = new TransferBusinessServiceImpl(
                accountRepository,
                movementRepository,
                movementRules,
                accountDomainService);
        debitCardService = new DebitCardBusinessServiceImpl(
                accountRepository,
                movementRepository,
                debitCardRepository,
                debitCardEventPublisher,
                movementRules,
                accountDomainService);

        stubRepositories();
        stubExternalRules();
    }

    @AfterEach
    void resetSchedulers() {
        AccountBusinessTestSupport.resetSchedulers();
    }

    @Test
    void givenVipCustomerWithCreditCard_whenAccountOperates_thenBusinessRulesAreEnforced() {
        customerCreditCards.put("vip-1", Boolean.TRUE);

        BankAccount vipSavings = accountService.create(new AccountRequest(
                        "vip-1",
                        CustomerType.PERSONAL,
                        AccountType.SAVINGS)
                        .customerProfile(CustomerProfile.VIP)
                        .initialBalance(BigDecimal.valueOf(700)))
                .blockingGet();
        BankAccount targetChecking = accountService.create(new AccountRequest(
                        "target-1",
                        CustomerType.PERSONAL,
                        AccountType.CHECKING)
                        .initialBalance(BigDecimal.valueOf(100)))
                .blockingGet();

        transferService.transfer(new TransferRequest(
                        vipSavings.getId(),
                        targetChecking.getId(),
                        BigDecimal.valueOf(100)))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(response -> {
                    assertThat(response.getSourceBalance()).isEqualByComparingTo(BigDecimal.valueOf(600));
                    assertThat(response.getTargetBalance()).isEqualByComparingTo(BigDecimal.valueOf(200));
                    return true;
                });

        movementService.withdraw(vipSavings.getId(), new MoneyRequest(BigDecimal.valueOf(150)))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(ResponseStatusException.class);
        accountService.create(new AccountRequest("vip-1", CustomerType.PERSONAL, AccountType.SAVINGS)
                        .customerProfile(CustomerProfile.VIP)
                        .initialBalance(BigDecimal.valueOf(700)))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(ResponseStatusException.class);

        assertThat(vipSavings.getCustomerProfileEnum()).isEqualTo(CustomerProfileEnum.VIP);
        assertThat(vipSavings.getMinimumDailyAverageAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(vipSavings.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(600));
    }

    @Test
    void givenBusinessCustomer_whenPymeFlowRuns_thenBusinessRulesAreEnforced() {
        AccountRequest validPymeRequest = new AccountRequest(
                "business-1",
                CustomerType.BUSINESS,
                AccountType.CHECKING)
                .customerProfile(CustomerProfile.PYME)
                .holders(List.of("holder-1"))
                .initialBalance(BigDecimal.valueOf(300));

        accountService.create(validPymeRequest)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(ResponseStatusException.class);

        customerCreditCards.put("business-1", Boolean.TRUE);
        accountService.create(new AccountRequest("business-1", CustomerType.BUSINESS, AccountType.SAVINGS)
                        .customerProfile(CustomerProfile.PYME)
                        .holders(List.of("holder-1"))
                        .initialBalance(BigDecimal.valueOf(300)))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(ResponseStatusException.class);
        accountService.create(new AccountRequest("business-1", CustomerType.BUSINESS, AccountType.CHECKING)
                        .customerProfile(CustomerProfile.PYME)
                        .initialBalance(BigDecimal.valueOf(300)))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(ResponseStatusException.class);

        customerOverdueDebt.put("business-1", Boolean.TRUE);
        accountService.create(validPymeRequest)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(ResponseStatusException.class);
        customerOverdueDebt.put("business-1", Boolean.FALSE);

        BankAccount pymeChecking = accountService.create(validPymeRequest).blockingGet();
        DebitCard debitCard = debitCardService.createDebitCard(new DebitCardRequest(
                        "business-1",
                        pymeChecking.getId(),
                        "5100000000000001"))
                .blockingGet();

        debitCardService.payWithDebitCard(debitCard.getId(), new DebitCardPaymentRequest(BigDecimal.valueOf(50)))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(movement -> {
                    assertThat(movement.getType()).isEqualTo(MovementTypeEnum.DEBIT_CARD_PAYMENT);
                    assertThat(movement.getResultingBalance()).isEqualByComparingTo(BigDecimal.valueOf(250));
                    return true;
                });

        assertThat(pymeChecking.getType()).isEqualTo(AccountTypeEnum.CHECKING);
        assertThat(pymeChecking.getMaintenanceFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(debitCard.isActive()).isTrue();
        verify(debitCardEventPublisher).registerCreated(any(DebitCard.class));
    }

    private void stubRepositories() {
        when(accountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> saveAccount(
                invocation.getArgument(0)));
        when(accountRepository.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(accounts.get(
                invocation.getArgument(0))));
        when(accountRepository.existsByCustomerIdAndType(anyString(), any(AccountTypeEnum.class))).thenAnswer(
                invocation -> accountExists(invocation.getArgument(0), invocation.getArgument(1)));
        when(movementRepository.save(any(AccountMovement.class))).thenAnswer(invocation -> saveMovement(
                invocation.getArgument(0)));
        when(debitCardRepository.save(any(DebitCard.class))).thenAnswer(invocation -> saveDebitCard(
                invocation.getArgument(0)));
        when(debitCardRepository.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(debitCards.get(
                invocation.getArgument(0))));
    }

    private void stubExternalRules() {
        when(creditProductProxy.customerHasCreditCard(anyString())).thenAnswer(invocation -> Single.just(
                customerCreditCards.getOrDefault(invocation.getArgument(0), Boolean.FALSE)));
        when(creditDebtStatusCache.hasOverdueDebt(anyString())).thenAnswer(invocation -> Single.just(
                customerOverdueDebt.getOrDefault(invocation.getArgument(0), Boolean.FALSE)));
    }

    private BankAccount saveAccount(BankAccount account) {
        if (account.getId() == null) {
            account.setId("account-" + ++accountSequence);
        }
        accounts.put(account.getId(), account);
        return account;
    }

    private AccountMovement saveMovement(AccountMovement movement) {
        if (movement.getId() == null) {
            movement.setId("movement-" + ++movementSequence);
        }
        movements.put(movement.getId(), movement);
        return movement;
    }

    private DebitCard saveDebitCard(DebitCard debitCard) {
        if (debitCard.getId() == null) {
            debitCard.setId("card-" + ++debitCardSequence);
        }
        debitCards.put(debitCard.getId(), debitCard);
        return debitCard;
    }

    private boolean accountExists(String customerId, AccountTypeEnum type) {
        return accounts.values().stream()
                .anyMatch(account -> customerId.equals(account.getCustomerId()) && type == account.getType());
    }
}
