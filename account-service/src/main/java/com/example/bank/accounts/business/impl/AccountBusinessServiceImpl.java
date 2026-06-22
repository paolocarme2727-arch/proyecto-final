package com.example.bank.accounts.business.impl;

import static com.example.bank.accounts.util.CommonUtils.defaultList;

import com.example.bank.accounts.business.AccountService;
import com.example.bank.accounts.business.impl.strategy.AccountCreationValidationStrategy;
import com.example.bank.accounts.business.impl.strategy.AccountValidationStrategy;
import com.example.bank.accounts.business.domain.AccountDomainService;
import com.example.bank.accounts.business.util.AccountBusinessUtils;
import com.example.bank.accounts.config.AccountProperties;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.BalanceResponse;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import com.example.bank.accounts.util.enums.CustomerProfileEnum;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handles account lifecycle operations and account creation rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountBusinessServiceImpl implements AccountService {

    private final BankAccountRepository accountRepository;
    private final AccountProperties properties;
    private final AccountDomainService accountDomainService;
    private final List<AccountValidationStrategy> accountValidationStrategies;
    private final List<AccountCreationValidationStrategy> accountCreationValidationStrategies;

    /**
     * Creates an account after validating customer and account type rules.
     */
    public Single<BankAccount> create(AccountRequest request) {
        return validateNewAccount(request)
                .andThen(Single.fromCallable(() -> accountRepository.save(toNewAccount(request))))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(account -> log.info("Cuenta creada {}", account.getId()));
    }

    /**
     * Returns all bank accounts.
     */
    public Flowable<BankAccount> findAll() {
        return Single.fromCallable(accountRepository::findAll)
                .subscribeOn(Schedulers.io())
                .flattenAsFlowable(accounts -> accounts);
    }

    /**
     * Returns one account or raises a 404 error.
     */
    public Single<BankAccount> findById(String id) {
        return accountDomainService.findExistingAccountReactive(id);
    }

    /**
     * Updates account metadata without changing the balance.
     */
    public Single<BankAccount> update(String id, AccountRequest request) {
        return accountDomainService.findExistingAccountReactive(id)
                .flatMap(account -> Single.fromCallable(() -> {
                    validateAccountShape(request);
                    return accountRepository.save(applyAccountUpdate(account, request));
                }))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(account -> log.info("Cuenta actualizada {}", account.getId()));
    }

    /**
     * Deletes an account if it exists.
     */
    public Single<Boolean> delete(String id) {
        return accountDomainService.findExistingAccountReactive(id)
                .flatMap(account -> Single.fromCallable(() -> {
                    accountRepository.delete(account);
                    return Boolean.TRUE;
                }))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(ignored -> log.info("Cuenta eliminada {}", id))
                .doOnError(error -> log.error("Error al eliminar cuenta"))
                .onErrorReturnItem(Boolean.FALSE);
    }

    /**
     * Returns the account available balance.
     */
    public Single<BalanceResponse> getBalance(String id) {
        return accountDomainService.findExistingAccountReactive(id)
                .map(account ->
                        new BalanceResponse().productId(account.getId()).availableBalance(account.getBalance()));
    }

    private Completable validateNewAccount(AccountRequest request) {
        return Completable.fromAction(() -> validateAccountShape(request))
                .andThen(Flowable.fromIterable(accountCreationValidationStrategies)
                        .concatMapCompletable(strategy -> strategy.validate(request)));
    }

    private void validateAccountShape(AccountRequest request) {
        accountValidationStrategies.forEach(strategy -> strategy.validate(request));
    }

    private BankAccount toNewAccount(AccountRequest request) {
        LocalDateTime now = LocalDateTime.now();
        BankAccount account = BankAccount.builder()
                .customerId(request.getCustomerId())
                .customerTypeEnum(AccountBusinessUtils.toDomainCustomerType(request))
                .customerProfileEnum(AccountBusinessUtils.toDomainCustomerProfile(request))
                .type(AccountBusinessUtils.toDomainAccountType(request))
                .balance(request.getInitialBalance() == null ? BigDecimal.ZERO : request.getInitialBalance())
                .minimumOpeningAmount(AccountBusinessUtils.resolveMinimumOpeningAmount(request, properties))
                .minimumDailyAverageAmount(AccountBusinessUtils.resolveMinimumDailyAverageAmount(request, properties))
                .movementDay(request.getMovementDay())
                .holders(defaultList(request.getHolders()))
                .authorizedSigners(defaultList(request.getAuthorizedSigners()))
                .freeTransactionLimit(properties.freeTransactionLimit())
                .transactionFee(properties.transactionFee())
                .chargedFees(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();
        applyProductSettings(account);
        return account;
    }

    private BankAccount applyAccountUpdate(BankAccount account, AccountRequest request) {
        account.setCustomerId(request.getCustomerId());
        account.setCustomerTypeEnum(AccountBusinessUtils.toDomainCustomerType(request));
        account.setCustomerProfileEnum(AccountBusinessUtils.toDomainCustomerProfile(request));
        account.setType(AccountBusinessUtils.toDomainAccountType(request));
        account.setMinimumOpeningAmount(AccountBusinessUtils.resolveMinimumOpeningAmount(request, properties));
        account.setMinimumDailyAverageAmount(AccountBusinessUtils.resolveMinimumDailyAverageAmount(request, properties));
        account.setMovementDay(request.getMovementDay());
        account.setHolders(defaultList(request.getHolders()));
        account.setAuthorizedSigners(defaultList(request.getAuthorizedSigners()));
        account.setFreeTransactionLimit(properties.freeTransactionLimit());
        account.setTransactionFee(properties.transactionFee());
        account.setChargedFees(account.getChargedFees() == null ? BigDecimal.ZERO : account.getChargedFees());
        account.setUpdatedAt(LocalDateTime.now());
        applyProductSettings(account);
        return account;
    }

    private void applyProductSettings(BankAccount account) {
        boolean pymeChecking = account.getCustomerProfileEnum() == CustomerProfileEnum.PYME
                && account.getType() == AccountTypeEnum.CHECKING;
        account.setMaintenanceFee(account.getType() == AccountTypeEnum.CHECKING && !pymeChecking
                ? properties.checkingMaintenanceFee()
                : BigDecimal.ZERO);
        account.setMonthlyMovementLimit(account.getType() == AccountTypeEnum.SAVINGS
                ? properties.savingsMonthlyMovementLimit()
                : null);
    }

}
