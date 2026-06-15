package com.example.bank.wallets.proxy;

import io.reactivex.rxjava3.core.Completable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes wallet domain events to Kafka.
 */
@Component
public class WalletEventPublisher {

    private static final String WALLET_PAYMENTS_TOPIC = "wallet.payments";
    private static final String WALLET_DEBIT_CARDS_TOPIC = "wallet.debit-cards";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Creates the publisher.
     *
     * @param kafkaTemplate kafka template
     */
    public WalletEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a wallet payment event.
     */
    public Completable publishWalletPayment(
            String sourceWalletId,
            String targetWalletId,
            String sourceDebitCardId,
            String targetDebitCardId,
            BigDecimal amount) {
        WalletPaymentEvent event = new WalletPaymentEvent(
                sourceWalletId,
                targetWalletId,
                sourceDebitCardId,
                targetDebitCardId,
                amount,
                LocalDateTime.now());
        return Completable.create(emitter -> kafkaTemplate.send(WALLET_PAYMENTS_TOPIC, sourceWalletId, event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        emitter.onError(throwable);
                    } else {
                        emitter.onComplete();
                    }
                }));
    }

    /**
     * Publishes a debit card link event.
     */
    public void publishDebitCardLinked(String walletId, String debitCardId) {
        kafkaTemplate.send(WALLET_DEBIT_CARDS_TOPIC, walletId, new WalletDebitCardLinkedEvent(walletId, debitCardId, LocalDateTime.now()));
    }

    /**
     * Event emitted after a wallet payment.
     */
    public record WalletPaymentEvent(
            String sourceWalletId,
            String targetWalletId,
            String sourceDebitCardId,
            String targetDebitCardId,
            BigDecimal amount,
            LocalDateTime createdAt) {
    }

    /**
     * Event emitted after a wallet is linked to a debit card.
     */
    public record WalletDebitCardLinkedEvent(String walletId, String debitCardId, LocalDateTime createdAt) {
    }
}

