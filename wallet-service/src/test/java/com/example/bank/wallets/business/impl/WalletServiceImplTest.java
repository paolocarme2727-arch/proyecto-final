package com.example.bank.wallets.business.impl;

import com.example.bank.wallets.proxy.DebitCardCatalog;
import com.example.bank.wallets.service.DocumentTypeCatalog;
import com.example.bank.wallets.proxy.WalletEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bank.wallets.domain.Wallet;
import com.example.bank.wallets.repository.WalletRepository;
import com.example.bank.wallets.expose.model.DebitCardLinkRequest;
import com.example.bank.wallets.expose.model.DocumentType;
import com.example.bank.wallets.expose.model.WalletPaymentRequest;
import com.example.bank.wallets.expose.model.WalletPaymentResponse;
import com.example.bank.wallets.expose.model.WalletRequest;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for Yanki wallet business rules.
 */
class WalletServiceImplTest {

    private final WalletRepository walletRepository = mock(WalletRepository.class);
    private final DocumentTypeCatalog documentTypeCatalog = mock(DocumentTypeCatalog.class);
    private final DebitCardCatalog debitCardCatalog = mock(DebitCardCatalog.class);
    private final WalletEventPublisher eventPublisher = mock(WalletEventPublisher.class);
    private final WalletServiceImpl walletService = new WalletServiceImpl(walletRepository, documentTypeCatalog, debitCardCatalog, eventPublisher);

    @BeforeEach
    void useImmediateScheduler() {
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    @AfterEach
    void resetSchedulers() {
        RxJavaPlugins.reset();
    }

    /**
     * Verifies wallet creation without needing a bank customer.
     */
    @Test
    void givenSupportedDocumentType_whenCreateWallet_thenPersistsWallet() {
        WalletRequest request = new WalletRequest(DocumentType.DNI, "12345678", "999999999", "imei-1", "user@example.com");

        when(documentTypeCatalog.isSupported(com.example.bank.wallets.domain.DocumentType.DNI)).thenReturn(Single.just(Boolean.TRUE));
        when(walletRepository.existsByPhoneNumber("999999999")).thenReturn(Boolean.FALSE);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestObserver<Wallet> observer = walletService.create(request).test();

        observer.awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(wallet -> {
                    assertThat(wallet.getPhoneNumber()).isEqualTo("999999999");
                    assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
                    return true;
                });
    }

    /**
     * Verifies wallet payments by phone number and event publication.
     */
    @Test
    void givenWalletsWithBalance_whenSendPayment_thenMovesBalanceAndPublishesEvent() {
        Wallet source = Wallet.builder().id("source").phoneNumber("900").balance(BigDecimal.TEN).build();
        Wallet target = Wallet.builder().id("target").phoneNumber("901").balance(BigDecimal.ONE).build();
        WalletPaymentRequest request = new WalletPaymentRequest("900", "901", BigDecimal.valueOf(4));

        when(walletRepository.findByPhoneNumber("900")).thenReturn(Optional.of(source));
        when(walletRepository.findByPhoneNumber("901")).thenReturn(Optional.of(target));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventPublisher.publishWalletPayment("source", "target", null, null, BigDecimal.valueOf(4))).thenReturn(Completable.complete());

        TestObserver<WalletPaymentResponse> observer = walletService.sendPayment(request).test();

        observer.awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(response -> {
                    assertThat(response.getSourceBalance()).isEqualByComparingTo(BigDecimal.valueOf(6));
                    assertThat(response.getTargetBalance()).isEqualByComparingTo(BigDecimal.valueOf(5));
                    return true;
                });
        verify(eventPublisher).publishWalletPayment("source", "target", null, null, BigDecimal.valueOf(4));
    }

    /**
     * Verifies that wallets only link debit cards available in the account catalog.
     */
    @Test
    void givenUnknownDebitCard_whenLinkDebitCard_thenRejectsLink() {
        when(debitCardCatalog.exists("missing-card")).thenReturn(Single.just(Boolean.FALSE));

        TestObserver<Wallet> observer = walletService.linkDebitCard(
                "wallet-1",
                new DebitCardLinkRequest("missing-card")).test();

        observer.awaitDone(5, TimeUnit.SECONDS).assertError(ResponseStatusException.class);
    }

    /**
     * Verifies that linked wallets publish bank debit-card information instead of moving internal wallet balance.
     */
    @Test
    void givenLinkedWallets_whenSendPayment_thenPublishesDebitCardsAndKeepsWalletBalances() {
        Wallet source = Wallet.builder().id("source").phoneNumber("900").debitCardId("source-card").balance(BigDecimal.ZERO).build();
        Wallet target = Wallet.builder().id("target").phoneNumber("901").debitCardId("target-card").balance(BigDecimal.ONE).build();
        WalletPaymentRequest request = new WalletPaymentRequest("900", "901", BigDecimal.valueOf(4));

        when(walletRepository.findByPhoneNumber("900")).thenReturn(Optional.of(source));
        when(walletRepository.findByPhoneNumber("901")).thenReturn(Optional.of(target));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventPublisher.publishWalletPayment("source", "target", "source-card", "target-card", BigDecimal.valueOf(4))).thenReturn(Completable.complete());

        TestObserver<WalletPaymentResponse> observer = walletService.sendPayment(request).test();

        observer.awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(response -> {
                    assertThat(response.getSourceBalance()).isEqualByComparingTo(BigDecimal.ZERO);
                    assertThat(response.getTargetBalance()).isEqualByComparingTo(BigDecimal.ONE);
                    return true;
                });
        verify(eventPublisher).publishWalletPayment("source", "target", "source-card", "target-card", BigDecimal.valueOf(4));
    }
}

