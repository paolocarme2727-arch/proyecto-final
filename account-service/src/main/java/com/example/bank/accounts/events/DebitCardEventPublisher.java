package com.example.bank.accounts.events;

import com.example.bank.accounts.domain.DebitCard;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes debit card events for services that cache card metadata.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebitCardEventPublisher {

    private static final String TOPIC = "account.debit-cards";
    private static final String KEY_PREFIX = "account:debit-card:";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Publishes a debit card registration event.
     *
     * @param card created debit card
     * @return completion signal
     */
    public Completable publishCreated(DebitCard card) {
        DebitCardRegisteredEvent event = new DebitCardRegisteredEvent(
                card.getId(),
                card.getAccountId(),
                card.isActive(),
                LocalDateTime.now());
        return Completable.fromAction(() -> {
                    stringRedisTemplate.opsForValue()
                            .set(KEY_PREFIX + card.getId(), Boolean.toString(card.isActive()));
                })
                .andThen(Completable.fromCompletionStage(kafkaTemplate.send(TOPIC, card.getId(), event)))
                .doOnComplete(() -> log.info("Published debit card event {}", card.getId()))
                .subscribeOn(Schedulers.io());
    }

    /**
     * Event emitted when a debit card is available for wallet linking.
     */
    public record DebitCardRegisteredEvent(String debitCardId, String accountId, boolean active, LocalDateTime createdAt) {
    }
}

