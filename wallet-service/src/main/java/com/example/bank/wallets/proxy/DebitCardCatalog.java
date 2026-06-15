package com.example.bank.wallets.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String KEY_PREFIX = "account:debit-card:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Consumes debit card registration events.
     *
     * @param payload debit card event JSON
     */
    @KafkaListener(
            topics = "account.debit-cards",
            groupId = "wallet-service-debit-cards",
            properties = "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer")
    public void onDebitCardRegistered(String payload) {
        try {
            DebitCardRegisteredEvent event = objectMapper.readValue(payload, DebitCardRegisteredEvent.class);
            stringRedisTemplate.opsForValue()
                    .set(KEY_PREFIX + event.debitCardId(), Boolean.toString(event.active()));
        } catch (JsonProcessingException ex) {
            log.error("Could not parse debit card event", ex);
        }
    }

    /**
     * Checks if a debit card exists and is active in the cache.
     *
     * @param debitCardId debit card identifier
     * @return true when the card can be linked
     */
    public Single<Boolean> exists(String debitCardId) {
        return Single.fromCallable(() -> Boolean.parseBoolean(stringRedisTemplate.opsForValue().get(KEY_PREFIX + debitCardId)))
                .subscribeOn(Schedulers.io());
    }

    /**
     * Event emitted when an account debit card is registered.
     */
    public record DebitCardRegisteredEvent(String debitCardId, String accountId, boolean active, LocalDateTime createdAt) {
    }
}

