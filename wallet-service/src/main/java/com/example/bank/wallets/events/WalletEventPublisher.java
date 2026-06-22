package com.example.bank.wallets.events;

import com.example.bank.wallets.util.Constants;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes wallet domain events to Kafka.
 */
@Slf4j
@Component
public class WalletEventPublisher {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${banking.kafka.retry.max-attempts:3}")
    private long maxRetries = 3;

    /**
     * Creates the publisher.
     *
     * @param kafkaTemplate kafka template
     */
    public WalletEventPublisher(KafkaTemplate<String, byte[]> kafkaTemplate) {
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
        return Completable.defer(() -> Completable.fromCompletionStage(kafkaTemplate.send(
                        Constants.WALLET_PAYMENTS_TOPIC,
                        sourceWalletId,
                        AvroEventCodec.encodeWalletPayment(event))))
                .retry(maxRetries)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Publishes a debit card link event.
     */
    public void publishDebitCardLinked(String walletId, String debitCardId) {
        WalletDebitCardLinkedEvent event = new WalletDebitCardLinkedEvent(
                walletId,
                debitCardId,
                LocalDateTime.now());
        Completable.defer(() -> Completable.fromCompletionStage(kafkaTemplate.send(
                        Constants.WALLET_DEBIT_CARDS_TOPIC,
                        walletId,
                        AvroEventCodec.encodeWalletDebitCardLinked(event))))
                .retry(maxRetries)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> log.info("Evento de tarjeta vinculada al monedero publicado {}", walletId),
                        error -> log.error("No se pudo publicar el evento de tarjeta vinculada al monedero", error));
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

