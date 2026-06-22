package com.example.bank.wallets.events;

import com.example.bank.wallets.util.Constants;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Caches debit cards that are available for Yanki wallet linking.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebitCardCatalog {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${banking.kafka.retry.max-attempts:3}")
    private long maxRetries = 3;

    /**
     * Consumes debit card registration Avro events.
     *
     * @param payload debit card Avro payload
     */
    @KafkaListener(topics = "account.debit-cards", groupId = "wallet-service-debit-cards")
    public void onDebitCardRegistered(byte[] payload) {
        try {
            DebitCardRegisteredEvent event = AvroEventCodec.decodeDebitCardRegistered(payload);
            Single.fromCallable(() -> {
                        stringRedisTemplate.opsForValue()
                                .set(Constants.DEBIT_CARD_KEY_PREFIX + event.debitCardId(),
                                        Boolean.toString(event.active()));
                        return Boolean.TRUE;
                    })
                    .subscribeOn(Schedulers.io())
                    .retry(maxRetries)
                    .subscribe(
                            ignored -> log.info("Evento de tarjeta de debito actualizado {}", event.debitCardId()),
                            error -> log.error("No se pudo actualizar el evento de tarjeta de debito", error));
        } catch (IllegalStateException ex) {
            log.error("No se pudo interpretar el evento de tarjeta de debito", ex);
        }
    }

    /**
     * Checks if a debit card exists and is active in the cache.
     *
     * @param debitCardId debit card identifier
     * @return true when the card can be linked
     */
    public Single<Boolean> exists(String debitCardId) {
        return Single.fromCallable(() -> Boolean.parseBoolean(stringRedisTemplate.opsForValue().get(
                        Constants.DEBIT_CARD_KEY_PREFIX + debitCardId)))
                .subscribeOn(Schedulers.io());
    }

    /**
     * Event emitted when an account debit card is registered.
     */
    public record DebitCardRegisteredEvent(
            String debitCardId,
            String accountId,
            boolean active,
            LocalDateTime createdAt) {
    }
}
