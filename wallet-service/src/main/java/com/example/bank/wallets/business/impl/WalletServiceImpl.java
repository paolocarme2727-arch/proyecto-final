package com.example.bank.wallets.business.impl;

import com.example.bank.wallets.business.WalletService;
import com.example.bank.wallets.business.domain.WalletDomainService;
import com.example.bank.wallets.business.impl.strategy.WalletCreationValidationStrategy;
import com.example.bank.wallets.business.impl.strategy.WalletPaymentValidationStrategy;
import com.example.bank.wallets.business.util.WalletBusinessUtils;
import com.example.bank.wallets.domain.Wallet;
import com.example.bank.wallets.events.DebitCardCatalog;
import com.example.bank.wallets.events.WalletEventPublisher;
import com.example.bank.wallets.expose.model.DebitCardLinkRequest;
import com.example.bank.wallets.expose.model.WalletPaymentRequest;
import com.example.bank.wallets.expose.model.WalletPaymentResponse;
import com.example.bank.wallets.expose.model.WalletRequest;
import com.example.bank.wallets.repository.WalletRepository;
import com.example.bank.wallets.util.CommonUtils;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implements Yanki wallet operations without REST calls to other microservices.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final DebitCardCatalog debitCardCatalog;
    private final WalletEventPublisher eventPublisher;
    private final WalletDomainService walletDomainService;
    private final List<WalletCreationValidationStrategy> creationValidationStrategies;
    private final List<WalletPaymentValidationStrategy> paymentValidationStrategies;

    /**
     * Creates a wallet for a person that may not be a bank customer.
     */
    @Override
    public Single<Wallet> create(WalletRequest request) {
        return validateCreation(request)
                .andThen(Single.fromCallable(() -> walletRepository.save(toNewWallet(request))))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(wallet -> log.info("Monedero creado {}", wallet.getId()));
    }

    /**
     * Lists all wallets.
     */
    @Override
    public Flowable<Wallet> findAll() {
        return Single.fromCallable(walletRepository::findAll)
                .subscribeOn(Schedulers.io())
                .flattenAsFlowable(wallets -> wallets);
    }

    /**
     * Finds one wallet.
     */
    @Override
    public Single<Wallet> findById(String id) {
        return walletDomainService.findExistingWalletReactive(id);
    }

    /**
     * Links a wallet to a debit card and emits an event for downstream account synchronization.
     */
    @Override
    public Single<Wallet> linkDebitCard(String id, DebitCardLinkRequest request) {
        return debitCardCatalog.exists(request.getDebitCardId())
                .flatMap(available -> available
                        ? walletDomainService.findExistingWalletReactive(id)
                        : Single.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "La tarjeta de débito no está disponible para vinculación")))
                .flatMap(wallet -> Single.fromCallable(() -> walletRepository.save(
                        applyDebitCardLink(wallet, request))))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(wallet -> eventPublisher.publishDebitCardLinked(wallet.getId(), wallet.getDebitCardId()));
    }

    /**
     * Sends money from one wallet to another using phone numbers.
     */
    @Override
    public Single<WalletPaymentResponse> sendPayment(WalletPaymentRequest request) {
        Single<Wallet> source = walletDomainService.findExistingWalletByPhoneNumberReactive(
                request.getSourcePhoneNumber());
        Single<Wallet> target = walletDomainService.findExistingWalletByPhoneNumberReactive(
                request.getTargetPhoneNumber());
        return Single.zip(source, target, WalletPair::new)
                .flatMap(pair -> executePayment(pair.source(), pair.target(), request)
                        .flatMap(response -> eventPublisher.publishWalletPayment(
                                response.getSourceWalletId(),
                                response.getTargetWalletId(),
                                pair.source().getDebitCardId(),
                                pair.target().getDebitCardId(),
                                response.getAmount()).andThen(Single.just(response))));
    }

    private Completable validateCreation(WalletRequest request) {
        return Completable.merge(creationValidationStrategies.stream()
                .map(strategy -> strategy.validate(request))
                .toList());
    }

    private Single<WalletPaymentResponse> executePayment(
            Wallet source,
            Wallet target,
            WalletPaymentRequest request) {
        paymentValidationStrategies.forEach(strategy -> strategy.validate(source, target, request));
        applyWalletPayment(source, target, request);
        return Single.zip(
                Single.fromCallable(() -> walletRepository.save(source)),
                Single.fromCallable(() -> walletRepository.save(target)),
                (savedSource, savedTarget) -> toWalletPaymentResponse(savedSource, savedTarget, request));
    }

    private Wallet toNewWallet(WalletRequest request) {
        LocalDateTime now = LocalDateTime.now();
        return Wallet.builder()
                .documentType(WalletBusinessUtils.toDomainDocumentType(request))
                .documentNumber(request.getDocumentNumber())
                .phoneNumber(request.getPhoneNumber())
                .imei(request.getImei())
                .email(request.getEmail())
                .balance(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Wallet applyDebitCardLink(Wallet wallet, DebitCardLinkRequest request) {
        wallet.setDebitCardId(request.getDebitCardId());
        wallet.setUpdatedAt(LocalDateTime.now());
        return wallet;
    }

    private void applyWalletPayment(Wallet source, Wallet target, WalletPaymentRequest request) {
        if (!CommonUtils.hasText(source.getDebitCardId())) {
            source.setBalance(source.getBalance().subtract(request.getAmount()));
        }
        if (!CommonUtils.hasText(target.getDebitCardId())) {
            target.setBalance(target.getBalance().add(request.getAmount()));
        }
        source.setUpdatedAt(LocalDateTime.now());
        target.setUpdatedAt(LocalDateTime.now());
    }

    private WalletPaymentResponse toWalletPaymentResponse(
            Wallet source,
            Wallet target,
            WalletPaymentRequest request) {
        return new WalletPaymentResponse()
                .sourceWalletId(source.getId())
                .targetWalletId(target.getId())
                .amount(request.getAmount())
                .sourceBalance(source.getBalance())
                .targetBalance(target.getBalance())
                .createdAt(LocalDateTime.now().atOffset(ZoneOffset.UTC));
    }

    private record WalletPair(Wallet source, Wallet target) {
    }
}
