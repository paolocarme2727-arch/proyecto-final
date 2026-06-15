package com.example.bank.accounts.business.impl;

import com.example.bank.accounts.domain.MovementType;
import com.example.bank.accounts.events.WalletPaymentEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Applies wallet payment events to debit-card linked bank accounts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletPaymentConsumer {

    private final BankAccountServiceImpl accountService;
    private final ObjectMapper objectMapper;

    /**
     * Consumes a raw wallet payment event.
     *
     * @param payload wallet payment JSON
     */
    @KafkaListener(
            topics = "wallet.payments",
            groupId = "account-service-wallet-payments",
            properties = "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer")
    public void onWalletPayment(String payload) {
        try {
            process(objectMapper.readValue(payload, WalletPaymentEvent.class))
                    .subscribe(
                            () -> log.info("Applied wallet payment event"),
                            error -> log.error("Could not apply wallet payment event", error));
        } catch (JsonProcessingException ex) {
            log.error("Could not parse wallet payment event", ex);
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
                ? accountService.registerWalletDebitCardMovement(event.sourceDebitCardId(), event.amount(), MovementType.WALLET_PAYMENT_OUT).ignoreElement()
                : Completable.complete();
        Completable targetCredit = hasText(event.targetDebitCardId())
                ? accountService.registerWalletDebitCardMovement(event.targetDebitCardId(), event.amount(), MovementType.WALLET_PAYMENT_IN).ignoreElement()
                : Completable.complete();
        return sourceCharge.andThen(targetCredit);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}


