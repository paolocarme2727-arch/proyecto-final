package com.example.bank.accounts.events;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Stores credit debt status events in Redis for fast acquisition validations.
 */
@Component
@RequiredArgsConstructor
public class CreditDebtStatusCache {

    private static final String KEY_PREFIX = "credit:overdue:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Consumes credit debt status changes.
     *
     * @param event debt status event
     */
    @KafkaListener(topics = "credit.debt-status", groupId = "account-service")
    public void onCreditDebtStatus(CreditDebtStatusEvent event) {
        stringRedisTemplate.opsForValue()
                .set(KEY_PREFIX + event.customerId(), Boolean.toString(event.hasOverdueDebt()));
    }

    /**
     * Checks if the customer has overdue credit debt.
     *
     * @param customerId customer identifier
     * @return true when there is overdue debt
     */
    public Single<Boolean> hasOverdueDebt(String customerId) {
        return Single.fromCallable(() -> Boolean.parseBoolean(stringRedisTemplate.opsForValue().get(KEY_PREFIX + customerId)))
                .subscribeOn(Schedulers.io());
    }
}

