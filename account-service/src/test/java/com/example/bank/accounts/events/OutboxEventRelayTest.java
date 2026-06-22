package com.example.bank.accounts.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.domain.OutboxEvent;
import com.example.bank.accounts.repository.OutboxEventRepository;
import com.example.bank.accounts.util.enums.OutboxEventStatusEnum;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for outbox event delivery.
 */
class OutboxEventRelayTest {

    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final DebitCardEventPublisher debitCardEventPublisher = mock(DebitCardEventPublisher.class);
    private final OutboxEventRelay relay = new OutboxEventRelay(outboxEventRepository, debitCardEventPublisher);

    @BeforeEach
    void useImmediateScheduler() {
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    @AfterEach
    void resetSchedulers() {
        RxJavaPlugins.reset();
    }

    @Test
    void givenPendingEvent_whenPublishPendingEvents_thenMarksAsPublished() {
        OutboxEvent event = OutboxEvent.builder()
                .id("outbox-1")
                .eventKey("card-1")
                .status(OutboxEventStatusEnum.PENDING)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        when(outboxEventRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(OutboxEventStatusEnum.PENDING),
                any(LocalDateTime.class))).thenReturn(List.of(event));
        when(debitCardEventPublisher.publishOutboxEvent(event)).thenReturn(Completable.complete());
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        relay.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatusEnum.PUBLISHED);
    }

    @Test
    void givenPendingEventWithLastRetry_whenPublishFails_thenMarksAsFailed() {
        OutboxEvent event = OutboxEvent.builder()
                .id("outbox-2")
                .eventKey("card-2")
                .status(OutboxEventStatusEnum.PENDING)
                .attempts(2)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        when(outboxEventRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(OutboxEventStatusEnum.PENDING),
                any(LocalDateTime.class))).thenReturn(List.of(event));
        when(debitCardEventPublisher.publishOutboxEvent(event))
                .thenReturn(Completable.error(new RuntimeException("kafka")));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        relay.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatusEnum.FAILED);
        assertThat(event.getAttempts()).isEqualTo(3);
    }
}
