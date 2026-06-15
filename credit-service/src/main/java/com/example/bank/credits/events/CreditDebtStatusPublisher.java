package com.example.bank.credits.events;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes credit debt status events.
 */
@Component
@RequiredArgsConstructor
public class CreditDebtStatusPublisher {

    private static final String TOPIC = "credit.debt-status";
    private static final String KEY_PREFIX = "credit:overdue:";

    private final KafkaTemplate<String, CreditDebtStatusEvent> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;

    /**
     * Publishes the current overdue debt status for a customer.
     *
     * @param customerId customer identifier
     * @param hasOverdueDebt overdue debt flag
     */
    public Completable publish(String customerId, boolean hasOverdueDebt) {
        CreditDebtStatusEvent event = new CreditDebtStatusEvent(customerId, hasOverdueDebt);
        return Completable.fromAction(() -> redisTemplate.opsForValue().set(KEY_PREFIX + customerId, Boolean.toString(hasOverdueDebt)))
                .andThen(Completable.fromCompletionStage(kafkaTemplate.send(TOPIC, customerId, event)))
                .subscribeOn(Schedulers.io());
    }
}

