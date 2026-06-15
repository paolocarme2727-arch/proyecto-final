package com.example.bank.wallets.business.impl;

import com.example.bank.wallets.business.WalletService;
import com.example.bank.wallets.domain.DocumentType;
import com.example.bank.wallets.domain.Wallet;
import com.example.bank.wallets.expose.model.DebitCardLinkRequest;
import com.example.bank.wallets.expose.model.WalletPaymentRequest;
import com.example.bank.wallets.expose.model.WalletPaymentResponse;
import com.example.bank.wallets.expose.model.WalletRequest;
import com.example.bank.wallets.repository.WalletRepository;
import com.example.bank.wallets.proxy.DebitCardCatalog;
import com.example.bank.wallets.service.DocumentTypeCatalog;
import com.example.bank.wallets.proxy.WalletEventPublisher;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final DocumentTypeCatalog documentTypeCatalog;
    private final DebitCardCatalog debitCardCatalog;
    private final WalletEventPublisher eventPublisher;

    /**
     * Creates a wallet for a person that may not be a bank customer.
     */
    @Override
    public Single<Wallet> create(WalletRequest request) {
        DocumentType documentType = DocumentType.valueOf(request.getDocumentType().getValue());
        return documentTypeCatalog.isSupported(documentType)
                .flatMap(supported -> supported
                        ? Single.fromCallable(() -> walletRepository.existsByPhoneNumber(request.getPhoneNumber()))
                        : Single.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported document type")))
                .flatMap(exists -> exists
                        ? Single.error(new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already has a wallet"))
                        : Single.fromCallable(() -> walletRepository.save(toNewWallet(request))))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(wallet -> log.info("Created wallet {}", wallet.getId()));
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
        return findExisting(id);
    }

    /**
     * Links a wallet to a debit card and emits an event for downstream account synchronization.
     */
    @Override
    public Single<Wallet> linkDebitCard(String id, DebitCardLinkRequest request) {
        return debitCardCatalog.exists(request.getDebitCardId())
                .flatMap(available -> available
                        ? findExisting(id)
                        : Single.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debit card is not available for linking")))
                .flatMap(wallet -> {
                    wallet.setDebitCardId(request.getDebitCardId());
                    wallet.setUpdatedAt(LocalDateTime.now());
                    return Single.fromCallable(() -> walletRepository.save(wallet));
                })
                .subscribeOn(Schedulers.io())
                .doOnSuccess(wallet -> eventPublisher.publishDebitCardLinked(wallet.getId(), wallet.getDebitCardId()));
    }

    /**
     * Sends money from one wallet to another using phone numbers.
     */
    @Override
    public Single<WalletPaymentResponse> sendPayment(WalletPaymentRequest request) {
        Single<Wallet> source = findByPhoneNumber(request.getSourcePhoneNumber());
        Single<Wallet> target = findByPhoneNumber(request.getTargetPhoneNumber());
        return Single.zip(source, target, (sourceWallet, targetWallet) -> new WalletPair(sourceWallet, targetWallet))
                .flatMap(pair -> executePayment(pair.source(), pair.target(), request)
                        .flatMap(response -> eventPublisher.publishWalletPayment(
                                response.getSourceWalletId(),
                                response.getTargetWalletId(),
                                pair.source().getDebitCardId(),
                                pair.target().getDebitCardId(),
                                response.getAmount()).andThen(Single.just(response))));
    }

    private Single<WalletPaymentResponse> executePayment(Wallet source, Wallet target, WalletPaymentRequest request) {
        if (source.getId().equals(target.getId())) {
            return Single.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and target wallets must be different"));
        }
        boolean sourceLinked = hasText(source.getDebitCardId());
        boolean targetLinked = hasText(target.getDebitCardId());
        if (!sourceLinked && source.getBalance().compareTo(request.getAmount()) < 0) {
            return Single.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient wallet balance"));
        }
        if (!sourceLinked) {
            source.setBalance(source.getBalance().subtract(request.getAmount()));
        }
        if (!targetLinked) {
            target.setBalance(target.getBalance().add(request.getAmount()));
        }
        source.setUpdatedAt(LocalDateTime.now());
        target.setUpdatedAt(LocalDateTime.now());
        return Single.zip(
                Single.fromCallable(() -> walletRepository.save(source)),
                Single.fromCallable(() -> walletRepository.save(target)),
                (savedSource, savedTarget) -> new WalletPaymentResponse()
                        .sourceWalletId(savedSource.getId())
                        .targetWalletId(savedTarget.getId())
                        .amount(request.getAmount())
                        .sourceBalance(savedSource.getBalance())
                        .targetBalance(savedTarget.getBalance())
                        .createdAt(LocalDateTime.now().atOffset(ZoneOffset.UTC)));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Single<Wallet> findExisting(String id) {
        return Single.fromCallable(() -> walletRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found")))
                .subscribeOn(Schedulers.io());
    }

    private Single<Wallet> findByPhoneNumber(String phoneNumber) {
        return Single.fromCallable(() -> walletRepository.findByPhoneNumber(phoneNumber)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found")))
                .subscribeOn(Schedulers.io());
    }

    private Wallet toNewWallet(WalletRequest request) {
        LocalDateTime now = LocalDateTime.now();
        return Wallet.builder()
                .documentType(DocumentType.valueOf(request.getDocumentType().getValue()))
                .documentNumber(request.getDocumentNumber())
                .phoneNumber(request.getPhoneNumber())
                .imei(request.getImei())
                .email(request.getEmail())
                .balance(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private record WalletPair(Wallet source, Wallet target) {
    }
}


