package com.example.bank.credits.events;

import com.example.bank.credits.util.Constants;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes credit debt status events.
 */
@Component
@RequiredArgsConstructor
public class CreditDebtStatusPublisher {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${banking.kafka.retry.max-attempts:3}")
    private long maxRetries = 3;

    /**
     * Publishes the current overdue debt status for a customer.
     *
     * @param customerId customer identifier
     * @param hasOverdueDebt overdue debt flag
     */
    public Completable publish(String customerId, boolean hasOverdueDebt) {
        CreditDebtStatusEvent event = new CreditDebtStatusEvent(customerId, hasOverdueDebt);
        return Single.fromCallable(() -> {
                    redisTemplate.opsForValue().set(
                            Constants.CREDIT_OVERDUE_KEY_PREFIX + customerId,
                            Boolean.toString(hasOverdueDebt));
                    return Boolean.TRUE;
                })
                .ignoreElement()
                .andThen(Completable.defer(() -> Completable.fromCompletionStage(
                        kafkaTemplate.send(
                                Constants.CREDIT_DEBT_STATUS_TOPIC,
                                customerId,
                                AvroEventCodec.encodeCreditDebtStatus(event)))))
                .retry(maxRetries)
                .subscribeOn(Schedulers.io());
    }
}
