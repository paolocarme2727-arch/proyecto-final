package com.example.bank.accounts.business.impl;

import com.example.bank.accounts.business.BankAccountService;
import com.example.bank.accounts.proxy.CreditProductProxy;
import com.example.bank.accounts.config.AccountProperties;
import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.AccountType;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.domain.CustomerProfile;
import com.example.bank.accounts.domain.CustomerType;
import com.example.bank.accounts.domain.DebitCard;
import com.example.bank.accounts.domain.MovementType;
import com.example.bank.accounts.events.CreditDebtStatusCache;
import com.example.bank.accounts.events.DebitCardEventPublisher;
import com.example.bank.accounts.expose.model.AccountProductReportResponse;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.BalanceResponse;
import com.example.bank.accounts.expose.model.DebitCardPaymentRequest;
import com.example.bank.accounts.expose.model.DebitCardRequest;
import com.example.bank.accounts.expose.model.MoneyRequest;
import com.example.bank.accounts.expose.model.TransferRequest;
import com.example.bank.accounts.expose.model.TransferResponse;
import com.example.bank.accounts.repository.AccountMovementRepository;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.repository.DebitCardRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implements account CRUD operations, deposits, withdrawals and banking rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final DebitCardRepository debitCardRepository;
    private final AccountProperties properties;
    private final CreditProductProxy creditProductProxy;
    private final CreditDebtStatusCache creditDebtStatusCache;
    private final DebitCardEventPublisher debitCardEventPublisher;

    /**
     * Creates an account after validating customer and account type rules.
     */
    @Override
    public Single<BankAccount> create(AccountRequest request) {
        return validateNewAccount(request)
                .andThen(Single.fromCallable(() -> accountRepository.save(toNewAccount(request))))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(account -> log.info("Created account {}", account.getId()));
    }

    /**
     * Returns all bank accounts.
     */
    @Override
    public Flowable<BankAccount> findAll() {
        return Single.fromCallable(accountRepository::findAll)
                .subscribeOn(Schedulers.io())
                .flattenAsFlowable(accounts -> accounts);
    }

    /**
     * Returns one account or raises a 404 error.
     */
    @Override
    public Single<BankAccount> findById(String id) {
        return findExisting(id);
    }

    /**
     * Updates account metadata without changing the balance.
     */
    @Override
    public Single<BankAccount> update(String id, AccountRequest request) {
        return findExisting(id)
                .map(account -> {
                    validateAccountShape(request);
                    account.setCustomerId(request.getCustomerId());
                    account.setCustomerType(toDomainCustomerType(request));
                    account.setCustomerProfile(toDomainCustomerProfile(request));
                    account.setType(toDomainAccountType(request));
                    account.setMinimumOpeningAmount(resolveMinimumOpeningAmount(request));
                    account.setMinimumDailyAverageAmount(resolveMinimumDailyAverageAmount(request));
                    account.setMovementDay(request.getMovementDay());
                    account.setHolders(defaultList(request.getHolders()));
                    account.setAuthorizedSigners(defaultList(request.getAuthorizedSigners()));
                    account.setFreeTransactionLimit(properties.freeTransactionLimit());
                    account.setTransactionFee(properties.transactionFee());
                    account.setChargedFees(account.getChargedFees() == null ? BigDecimal.ZERO : account.getChargedFees());
                    account.setUpdatedAt(LocalDateTime.now());
                    applyProductSettings(account);
                    return accountRepository.save(account);
                })
                .subscribeOn(Schedulers.io())
                .doOnSuccess(account -> log.info("Updated account {}", account.getId()));
    }

    /**
     * Deletes an account if it exists.
     */
    @Override
    public Single<Boolean> delete(String id) {
        return findExisting(id)
                .map(account -> {
                    accountRepository.delete(account);
                    return Boolean.TRUE;
                })
                .subscribeOn(Schedulers.io())
                .doOnSuccess(ignored -> log.info("Deleted account {}", id));
    }

    /**
     * Registers a deposit movement.
     */
    @Override
    public Single<AccountMovement> deposit(String id, MoneyRequest request) {
        return registerMovement(id, request.getAmount(), MovementType.DEPOSIT);
    }

    /**
     * Registers a withdrawal movement.
     */
    @Override
    public Single<AccountMovement> withdraw(String id, MoneyRequest request) {
        return registerMovement(id, request.getAmount(), MovementType.WITHDRAWAL);
    }

    /**
     * Returns the account available balance.
     */
    @Override
    public Single<BalanceResponse> getBalance(String id) {
        return findExisting(id)
                .map(account -> new BalanceResponse().productId(account.getId()).availableBalance(account.getBalance()));
    }

    /**
     * Returns every movement for one account.
     */
    @Override
    public Flowable<AccountMovement> findMovements(String id) {
        return findExisting(id)
                .map(account -> movementRepository.findByAccountIdOrderByCreatedAtDesc(account.getId()))
                .flattenAsFlowable(movements -> movements)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Transfers money to another account in the bank and stores both sides of the movement.
     */
    @Override
    public Single<TransferResponse> transfer(TransferRequest request) {
        return validateTransferRequest(request)
                .andThen(Single.fromCallable(() -> executeTransfer(findExistingSync(request.getSourceAccountId()), findExistingSync(request.getTargetAccountId()), request)))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(response -> log.info("Transferred {} from account {} to account {}",
                        response.getAmount(), response.getSourceAccountId(), response.getTargetAccountId()));
    }

    /**
     * Generates a complete account report by product type and time interval.
     */
    @Override
    public Single<AccountProductReportResponse> getProductReport(
            com.example.bank.accounts.expose.model.AccountType type,
            OffsetDateTime from,
            OffsetDateTime to) {
        return Single.fromCallable(() -> {
                    AccountType accountType = AccountType.valueOf(type.getValue());
                    List<BankAccount> accounts = accountRepository.findByType(accountType);
                    List<String> accountIds = accounts.stream().map(BankAccount::getId).toList();
                    List<AccountMovement> movements = accountIds.isEmpty()
                            ? List.of()
                            : movementRepository.findByAccountIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
                                    accountIds,
                                    from.toLocalDateTime(),
                                    to.toLocalDateTime());
                    return toAccountReport(type, from, to, accounts.size(), movements);
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * Lists the latest debit-card-style movements for a bank account.
     */
    @Override
    public Flowable<AccountMovement> findRecentDebitCardMovements(String id) {
        return findExisting(id)
                .map(account -> movementRepository.findByAccountIdAndTypeOrderByCreatedAtDesc(
                        account.getId(),
                        MovementType.DEBIT_CARD_PAYMENT,
                        PageRequest.of(0, 10)))
                .flattenAsFlowable(movements -> movements)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Creates a debit card linked to an existing account.
     */
    @Override
    public Single<DebitCard> createDebitCard(DebitCardRequest request) {
        return findExisting(request.getAccountId())
                .flatMap(account -> {
                    if (!account.getCustomerId().equals(request.getCustomerId())) {
                        return Single.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account does not belong to customer"));
                    }
                    DebitCard card = DebitCard.builder()
                            .customerId(request.getCustomerId())
                            .accountId(request.getAccountId())
                            .cardNumber(request.getCardNumber())
                            .active(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return Single.fromCallable(() -> debitCardRepository.save(card));
                })
                .flatMap(card -> debitCardEventPublisher.publishCreated(card).andThen(Single.just(card)))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(card -> log.info("Created debit card {}", card.getId()));
    }

    /**
     * Pays with a debit card by charging its linked account.
     */
    @Override
    public Single<AccountMovement> payWithDebitCard(String id, DebitCardPaymentRequest request) {
        return findExistingDebitCard(id)
                .flatMap(card -> registerMovement(card.getAccountId(), request.getAmount(), MovementType.DEBIT_CARD_PAYMENT))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(movement -> log.info("Registered debit card payment with card {}", id));
    }

    Single<AccountMovement> registerWalletDebitCardMovement(String cardId, BigDecimal amount, MovementType movementType) {
        return findExistingDebitCard(cardId)
                .flatMap(card -> registerMovement(card.getAccountId(), amount, movementType));
    }

    private Single<AccountMovement> registerMovement(String id, BigDecimal amount, MovementType movementType) {
        return Single.fromCallable(() -> {
                    BankAccount account = findExistingSync(id);
                    validateMovement(account, amount, movementType);
                    return saveMovement(account, amount, movementType);
                })
                .subscribeOn(Schedulers.io())
                .doOnSuccess(movement -> log.info("Registered {} on account {}", movement.getType(), id));
    }

    private AccountMovement saveMovement(BankAccount account, BigDecimal amount, MovementType movementType) {
        BigDecimal fee = calculateTransactionFee(account);
        account.setBalance(calculateResultingBalance(account, amount, movementType, fee));
        account.setMonthlyMovementCount(account.getMonthlyMovementCount() + 1);
        account.setChargedFees((account.getChargedFees() == null ? BigDecimal.ZERO : account.getChargedFees()).add(fee));
        account.setUpdatedAt(LocalDateTime.now());
        BankAccount saved = accountRepository.save(account);
        return movementRepository.save(AccountMovement.builder()
                .accountId(saved.getId())
                .type(movementType)
                .amount(amount)
                .fee(fee)
                .resultingBalance(saved.getBalance())
                .createdAt(LocalDateTime.now())
                .build());
    }

    private TransferResponse executeTransfer(BankAccount source, BankAccount target, TransferRequest request) {
        if (source.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and target accounts must be different");
        }
        validateMovement(source, request.getAmount(), MovementType.TRANSFER_OUT);
        validateMovement(target, request.getAmount(), MovementType.TRANSFER_IN);
        BigDecimal sourceFee = calculateTransactionFee(source);
        BigDecimal targetFee = calculateTransactionFee(target);
        source.setBalance(calculateResultingBalance(source, request.getAmount(), MovementType.TRANSFER_OUT, sourceFee));
        target.setBalance(calculateResultingBalance(target, request.getAmount(), MovementType.TRANSFER_IN, targetFee));
        source.setMonthlyMovementCount(source.getMonthlyMovementCount() + 1);
        target.setMonthlyMovementCount(target.getMonthlyMovementCount() + 1);
        source.setChargedFees((source.getChargedFees() == null ? BigDecimal.ZERO : source.getChargedFees()).add(sourceFee));
        target.setChargedFees((target.getChargedFees() == null ? BigDecimal.ZERO : target.getChargedFees()).add(targetFee));
        source.setUpdatedAt(LocalDateTime.now());
        target.setUpdatedAt(LocalDateTime.now());
        return saveTransferMovements(accountRepository.save(source), accountRepository.save(target), request, sourceFee, targetFee);
    }

    private TransferResponse saveTransferMovements(
            BankAccount source,
            BankAccount target,
            TransferRequest request,
            BigDecimal sourceFee,
            BigDecimal targetFee) {
        LocalDateTime now = LocalDateTime.now();
        AccountMovement sourceMovement = movementRepository.save(AccountMovement.builder()
                .accountId(source.getId())
                .type(MovementType.TRANSFER_OUT)
                .amount(request.getAmount())
                .fee(sourceFee)
                .resultingBalance(source.getBalance())
                .createdAt(now)
                .build());
        AccountMovement targetMovement = movementRepository.save(AccountMovement.builder()
                .accountId(target.getId())
                .type(MovementType.TRANSFER_IN)
                .amount(request.getAmount())
                .fee(targetFee)
                .resultingBalance(target.getBalance())
                .createdAt(now)
                .build());
        return new TransferResponse()
                .sourceAccountId(source.getId())
                .targetAccountId(target.getId())
                .amount(request.getAmount())
                .sourceMovementId(sourceMovement.getId())
                .targetMovementId(targetMovement.getId())
                .sourceBalance(source.getBalance())
                .targetBalance(target.getBalance())
                .createdAt(now.atOffset(ZoneOffset.UTC));
    }

    private Completable validateTransferRequest(TransferRequest request) {
        return Completable.fromAction(() -> {
            if (request.getSourceAccountId().equals(request.getTargetAccountId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and target accounts must be different");
            }
        });
    }

    private void validateMovement(BankAccount account, BigDecimal amount, MovementType movementType) {
        BigDecimal fee = calculateTransactionFee(account);
        BigDecimal resultingBalance = calculateResultingBalance(account, amount, movementType, fee);
        if (resultingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }
        if (account.getType() == AccountType.FIXED_TERM
                && account.getMovementDay() != null
                && account.getMovementDay() != LocalDate.now().getDayOfMonth()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fixed term account can move only on its configured day");
        }
        if (account.getType() == AccountType.SAVINGS
                && account.getMonthlyMovementLimit() != null
                && account.getMonthlyMovementCount() >= account.getMonthlyMovementLimit()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Savings account monthly movement limit exceeded");
        }
        if (account.getCustomerProfile() == CustomerProfile.VIP
                && account.getType() == AccountType.SAVINGS
                && resultingBalance.compareTo(resolveStoredMinimumDailyAverageAmount(account)) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIP savings account minimum daily average would be violated");
        }
    }

    private Completable validateNewAccount(AccountRequest request) {
        return Completable.fromAction(() -> validateAccountShape(request))
                .andThen(validateNoOverdueDebt(request.getCustomerId()))
                .andThen(validateCreditCardRequirement(request))
                .andThen(Completable.fromAction(() -> {
                    if (toDomainCustomerType(request) == CustomerType.PERSONAL
                            && toDomainAccountType(request) != AccountType.FIXED_TERM
                            && accountRepository.existsByCustomerIdAndType(request.getCustomerId(), toDomainAccountType(request))) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Personal customer already owns this account type");
                    }
                }));
    }

    private Completable validateNoOverdueDebt(String customerId) {
        return creditDebtStatusCache.hasOverdueDebt(customerId)
                .flatMapCompletable(hasOverdueDebt -> hasOverdueDebt
                        ? Completable.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer has overdue credit debt"))
                        : Completable.complete());
    }

    private void validateAccountShape(AccountRequest request) {
        BigDecimal minimumOpeningAmount = resolveMinimumOpeningAmount(request);
        BigDecimal initialBalance = request.getInitialBalance() == null ? BigDecimal.ZERO : request.getInitialBalance();
        if (initialBalance.compareTo(minimumOpeningAmount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Initial balance is lower than the minimum opening amount");
        }
        if (toDomainCustomerProfile(request) == CustomerProfile.VIP
                && toDomainAccountType(request) == AccountType.SAVINGS
                && initialBalance.compareTo(properties.vipMonthlyMinimumAverageAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIP savings account requires the minimum daily average amount at opening");
        }
        if (toDomainCustomerType(request) == CustomerType.BUSINESS && toDomainAccountType(request) != AccountType.CHECKING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business customers only can own checking accounts");
        }
        if (toDomainCustomerType(request) == CustomerType.BUSINESS && defaultList(request.getHolders()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business accounts require at least one holder");
        }
        if (toDomainCustomerProfile(request) == CustomerProfile.VIP && toDomainCustomerType(request) != CustomerType.PERSONAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIP profile is available only for personal customers");
        }
        if (toDomainCustomerProfile(request) == CustomerProfile.PYME && toDomainCustomerType(request) != CustomerType.BUSINESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PYME profile is available only for business customers");
        }
    }

    private Completable validateCreditCardRequirement(AccountRequest request) {
        boolean requiresCreditCard = (toDomainCustomerProfile(request) == CustomerProfile.VIP && toDomainAccountType(request) == AccountType.SAVINGS)
                || (toDomainCustomerProfile(request) == CustomerProfile.PYME && toDomainAccountType(request) == AccountType.CHECKING);
        if (!requiresCreditCard) {
            return Completable.complete();
        }
        return creditProductProxy.customerHasCreditCard(request.getCustomerId())
                .flatMapCompletable(hasCreditCard -> hasCreditCard
                        ? Completable.complete()
                        : Completable.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer must have a credit card to open this account")));
    }

    private Single<BankAccount> findExisting(String id) {
        return Single.fromCallable(() -> findExistingSync(id))
                .subscribeOn(Schedulers.io());
    }

    private BankAccount findExistingSync(String id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private Single<DebitCard> findExistingDebitCard(String id) {
        return Single.fromCallable(() -> {
                    DebitCard card = debitCardRepository.findById(id)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Debit card not found"));
                    if (!card.isActive()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debit card is inactive");
                    }
                    return card;
                })
                .subscribeOn(Schedulers.io());
    }

    private BankAccount toNewAccount(AccountRequest request) {
        LocalDateTime now = LocalDateTime.now();
        BankAccount account = BankAccount.builder()
                .customerId(request.getCustomerId())
                .customerType(toDomainCustomerType(request))
                .customerProfile(toDomainCustomerProfile(request))
                .type(toDomainAccountType(request))
                .balance(request.getInitialBalance() == null ? BigDecimal.ZERO : request.getInitialBalance())
                .minimumOpeningAmount(resolveMinimumOpeningAmount(request))
                .minimumDailyAverageAmount(resolveMinimumDailyAverageAmount(request))
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

    private void applyProductSettings(BankAccount account) {
        boolean pymeChecking = account.getCustomerProfile() == CustomerProfile.PYME && account.getType() == AccountType.CHECKING;
        account.setMaintenanceFee(account.getType() == AccountType.CHECKING && !pymeChecking ? properties.checkingMaintenanceFee() : BigDecimal.ZERO);
        account.setMonthlyMovementLimit(account.getType() == AccountType.SAVINGS ? properties.savingsMonthlyMovementLimit() : null);
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(value -> !value.isBlank()).toList();
    }

    private CustomerType toDomainCustomerType(AccountRequest request) {
        return CustomerType.valueOf(request.getCustomerType().getValue());
    }

    private AccountType toDomainAccountType(AccountRequest request) {
        return AccountType.valueOf(request.getType().getValue());
    }

    private CustomerProfile toDomainCustomerProfile(AccountRequest request) {
        return request.getCustomerProfile() == null ? CustomerProfile.REGULAR : CustomerProfile.valueOf(request.getCustomerProfile().getValue());
    }

    private BigDecimal resolveMinimumOpeningAmount(AccountRequest request) {
        return request.getMinimumOpeningAmount() == null ? properties.minimumOpeningAmount() : request.getMinimumOpeningAmount();
    }

    private BigDecimal resolveMinimumDailyAverageAmount(AccountRequest request) {
        return toDomainCustomerProfile(request) == CustomerProfile.VIP && toDomainAccountType(request) == AccountType.SAVINGS
                ? properties.vipMonthlyMinimumAverageAmount()
                : BigDecimal.ZERO;
    }

    private BigDecimal calculateTransactionFee(BankAccount account) {
        int freeLimit = account.getFreeTransactionLimit() == 0 ? properties.freeTransactionLimit() : account.getFreeTransactionLimit();
        BigDecimal fee = account.getTransactionFee() == null ? properties.transactionFee() : account.getTransactionFee();
        return account.getMonthlyMovementCount() >= freeLimit ? fee : BigDecimal.ZERO;
    }

    private BigDecimal calculateResultingBalance(BankAccount account, BigDecimal amount, MovementType movementType, BigDecimal fee) {
        BigDecimal signedAmount = isCreditMovement(movementType) ? amount : amount.negate();
        return account.getBalance().add(signedAmount).subtract(fee);
    }

    private BigDecimal resolveStoredMinimumDailyAverageAmount(BankAccount account) {
        return account.getMinimumDailyAverageAmount() == null ? properties.vipMonthlyMinimumAverageAmount() : account.getMinimumDailyAverageAmount();
    }

    private boolean isCreditMovement(MovementType movementType) {
        return movementType == MovementType.DEPOSIT
                || movementType == MovementType.TRANSFER_IN
                || movementType == MovementType.WALLET_PAYMENT_IN;
    }

    private AccountProductReportResponse toAccountReport(
            com.example.bank.accounts.expose.model.AccountType type,
            OffsetDateTime from,
            OffsetDateTime to,
            int productCount,
            List<AccountMovement> movements) {
        BigDecimal totalAmount = movements.stream()
                .map(AccountMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees = movements.stream()
                .map(movement -> movement.getFee() == null ? BigDecimal.ZERO : movement.getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<String> accountIds = movements.stream().map(AccountMovement::getAccountId).collect(java.util.stream.Collectors.toSet());
        return new AccountProductReportResponse()
                .productType(type)
                .from(from)
                .to(to)
                .productCount(productCount == 0 ? accountIds.size() : productCount)
                .movementCount(movements.size())
                .totalAmount(totalAmount)
                .totalFees(totalFees)
                .movements(movements.stream().map(this::toApiMovement).toList());
    }

    private com.example.bank.accounts.expose.model.AccountMovement toApiMovement(AccountMovement movement) {
        return new com.example.bank.accounts.expose.model.AccountMovement()
                .id(movement.getId())
                .accountId(movement.getAccountId())
                .type(com.example.bank.accounts.expose.model.MovementType.fromValue(movement.getType().name()))
                .amount(movement.getAmount())
                .fee(movement.getFee())
                .resultingBalance(movement.getResultingBalance())
                .createdAt(movement.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}


