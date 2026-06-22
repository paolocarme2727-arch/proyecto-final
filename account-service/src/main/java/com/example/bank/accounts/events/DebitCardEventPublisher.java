package com.example.bank.accounts.events;

import com.example.bank.accounts.domain.DebitCard;
import com.example.bank.accounts.domain.OutboxEvent;
import com.example.bank.accounts.repository.OutboxEventRepository;
import com.example.bank.accounts.util.enums.OutboxEventStatusEnum;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Registers debit card events in the outbox and publishes pending events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebitCardEventPublisher {

    private static final String TOPIC = "account.debit-cards";
    private static final String KEY_PREFIX = "account:debit-card:";
    private static final String EVENT_TYPE = "DebitCardRegistered";

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final OutboxEventRepository outboxEventRepository;

    @Value("${banking.kafka.retry.max-attempts:3}")
    private long maxRetries = 3;

    /**
     * Stores a debit card registration event in the outbox.
     *
     * @param card created debit card
     * @return completion signal
     */
    public Completable publishCreated(DebitCard card) {
        return Single.fromCallable(() -> {
                    registerCreated(card);
                    return Boolean.TRUE;
                })
                .ignoreElement()
                .subscribeOn(Schedulers.io());
    }

    /**
     * Stores a debit card registration event synchronously.
     *
     * @param card created debit card
     */
    public void registerCreated(DebitCard card) {
        outboxEventRepository.save(toOutboxEvent(card));
        log.info("Evento de tarjeta de debito registrado en outbox {}", card.getId());
    }

    /**
     * Publishes an outbox event to Redis and Kafka.
     *
     * @param event pending outbox event
     * @return completion signal
     */
    public Completable publishOutboxEvent(OutboxEvent event) {
        return Single.fromCallable(() -> {
                    DebitCardRegisteredEvent payload = toDebitCardRegisteredEvent(event);
                    stringRedisTemplate.opsForValue()
                            .set(KEY_PREFIX + payload.debitCardId(), Boolean.toString(payload.active()));
                    return AvroEventCodec.encodeDebitCardRegistered(payload);
                })
                .flatMapCompletable(payload -> Completable.defer(() -> Completable.fromCompletionStage(
                        kafkaTemplate.send(TOPIC, event.getEventKey(), payload))))
                .retry(maxRetries)
                .doOnComplete(() -> log.info("Evento de tarjeta de debito publicado {}", event.getEventKey()))
                .subscribeOn(Schedulers.io());
    }

    private OutboxEvent toOutboxEvent(DebitCard card) {
        LocalDateTime now = LocalDateTime.now();
        DebitCardRegisteredEvent event = new DebitCardRegisteredEvent(
                card.getId(),
                card.getAccountId(),
                card.isActive(),
                now);
        return OutboxEvent.builder()
                .topic(TOPIC)
                .eventKey(card.getId())
                .eventType(EVENT_TYPE)
                .payload(toPayload(event))
                .status(OutboxEventStatusEnum.PENDING)
                .attempts(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private String toPayload(DebitCardRegisteredEvent event) {
        byte[] payload = AvroEventCodec.encodeDebitCardRegistered(event);
        return Base64.getEncoder().encodeToString(payload);
    }

    private DebitCardRegisteredEvent toDebitCardRegisteredEvent(OutboxEvent event) {
        byte[] payload = Base64.getDecoder().decode(event.getPayload());
        return AvroEventCodec.decodeDebitCardRegistered(payload);
    }

    /**
     * Event emitted when a debit card is available for wallet linking.
     */
    public record DebitCardRegisteredEvent(
            String debitCardId,
            String accountId,
            boolean active,
            LocalDateTime createdAt) {
    }
}
