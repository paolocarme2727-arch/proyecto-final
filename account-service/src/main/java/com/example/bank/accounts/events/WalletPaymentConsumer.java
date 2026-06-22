package com.example.bank.accounts.events;

import com.example.bank.accounts.business.DebitCardService;
import com.example.bank.accounts.util.enums.MovementTypeEnum;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Applies wallet payment events to debit-card linked bank accounts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletPaymentConsumer {

    private final DebitCardService debitCardService;

    @Value("${banking.kafka.retry.max-attempts:3}")
    private long maxRetries = 3;

    /**
     * Consumes a wallet payment Avro event.
     *
     * @param payload wallet payment Avro payload
     */
    @KafkaListener(topics = "wallet.payments", groupId = "account-service-wallet-payments")
    public void onWalletPayment(byte[] payload) {
        try {
            process(AvroEventCodec.decodeWalletPayment(payload))
                    .retry(maxRetries)
                    .subscribe(
                            () -> log.info("Evento de pago de monedero aplicado"),
                            error -> log.error("No se pudo aplicar el evento de pago de monedero", error));
        } catch (IllegalStateException ex) {
            log.error("No se pudo interpretar el evento de pago de monedero", ex);
        }
    }

    /**
     * Applies a wallet payment to linked debit card accounts.
     *
     * @param event wallet payment event
     * @return completion signal
     */
    public Completable process(WalletPaymentEvent event) {
        Completable sourceCharge = hasText(event.sourceDebitCardId())
                ? debitCardService.registerWalletDebitCardMovement(
                        event.sourceDebitCardId(), event.amount(), MovementTypeEnum.WALLET_PAYMENT_OUT).ignoreElement()
                : Completable.complete();
        Completable targetCredit = hasText(event.targetDebitCardId())
                ? debitCardService.registerWalletDebitCardMovement(
                        event.targetDebitCardId(), event.amount(), MovementTypeEnum.WALLET_PAYMENT_IN).ignoreElement()
                : Completable.complete();
        return sourceCharge.andThen(targetCredit);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
