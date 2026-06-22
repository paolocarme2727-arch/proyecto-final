package com.example.bank.accounts.events;

import com.example.bank.accounts.domain.OutboxEvent;
import com.example.bank.accounts.repository.OutboxEventRepository;
import com.example.bank.accounts.util.enums.OutboxEventStatusEnum;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Publishes pending outbox events with retry support.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final DebitCardEventPublisher debitCardEventPublisher;

    @Value("${banking.accounts.outbox.retry-delay-seconds:10}")
    private int retryDelaySeconds;

    @Value("${banking.accounts.outbox.max-retries:3}")
    private int maxRetries = 3;

    /**
     * Publishes pending events periodically.
     */
    @Scheduled(fixedDelayString = "${banking.accounts.outbox.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        Single.fromCallable(() -> outboxEventRepository
                        .findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                                OutboxEventStatusEnum.PENDING,
                                LocalDateTime.now()))
                .flattenAsFlowable(events -> events)
                .flatMapCompletable(this::publishEvent)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> log.debug("Publicación de outbox completada"),
                        error -> log.error("Error al publicar eventos outbox", error));
    }

    private Completable publishEvent(OutboxEvent event) {
        return debitCardEventPublisher.publishOutboxEvent(event)
                .andThen(markPublished(event))
                .onErrorResumeNext(error -> markPendingForRetry(event, error));
    }

    private Completable markPublished(OutboxEvent event) {
        return Single.fromCallable(() -> {
                    event.setStatus(OutboxEventStatusEnum.PUBLISHED);
                    event.setUpdatedAt(LocalDateTime.now());
                    event.setLastError(null);
                    return outboxEventRepository.save(event);
                })
                .ignoreElement();
    }

    private Completable markPendingForRetry(OutboxEvent event, Throwable error) {
        return Single.fromCallable(() -> {
                    int attempts = event.getAttempts() + 1;
                    event.setAttempts(attempts);
                    event.setLastError(error.getMessage());
                    if (attempts >= maxRetries) {
                        event.setStatus(OutboxEventStatusEnum.FAILED);
                        event.setNextAttemptAt(null);
                    } else {
                        event.setStatus(OutboxEventStatusEnum.PENDING);
                        event.setNextAttemptAt(LocalDateTime.now().plusSeconds((long) attempts * retryDelaySeconds));
                    }
                    event.setUpdatedAt(LocalDateTime.now());
                    return outboxEventRepository.save(event);
                })
                .ignoreElement()
                .doOnComplete(() -> log.error(
                        event.getStatus() == OutboxEventStatusEnum.FAILED
                                ? "El evento outbox {} falló luego de agotar sus reintentos"
                                : "No se pudo publicar el evento outbox {}, se reintentará",
                        event.getId(),
                        error));
    }
}
