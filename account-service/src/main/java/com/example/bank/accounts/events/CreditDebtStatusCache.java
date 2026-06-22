package com.example.bank.accounts.events;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Stores credit debt status events in Redis for fast acquisition validations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditDebtStatusCache {

    private static final String KEY_PREFIX = "credit:overdue:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${banking.kafka.retry.max-attempts:3}")
    private long maxRetries = 3;

    /**
     * Consumes credit debt status changes.
     *
     * @param payload debt status Avro payload
     */
    @KafkaListener(topics = "credit.debt-status", groupId = "account-service")
    public void onCreditDebtStatus(byte[] payload) {
        CreditDebtStatusEvent event = AvroEventCodec.decodeCreditDebtStatus(payload);
        Single.fromCallable(() -> {
                    stringRedisTemplate.opsForValue()
                            .set(KEY_PREFIX + event.customerId(), Boolean.toString(event.hasOverdueDebt()));
                    return Boolean.TRUE;
                })
                .subscribeOn(Schedulers.io())
                .retry(maxRetries)
                .subscribe(
                        ignored -> log.info(
                                "Evento de deuda vencida actualizado para el cliente {}",
                                event.customerId()),
                        error -> log.error("No se pudo actualizar el evento de deuda vencida", error));
    }

    /**
     * Checks if the customer has overdue credit debt.
     *
     * @param customerId customer identifier
     * @return true when there is overdue debt
     */
    public Single<Boolean> hasOverdueDebt(String customerId) {
        return Single.fromCallable(() -> Boolean.parseBoolean(
                        stringRedisTemplate.opsForValue().get(KEY_PREFIX + customerId)))
                .subscribeOn(Schedulers.io());
    }
}

