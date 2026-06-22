package com.example.bank.accounts.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.domain.DebitCard;
import com.example.bank.accounts.domain.OutboxEvent;
import com.example.bank.accounts.repository.OutboxEventRepository;
import com.example.bank.accounts.util.enums.OutboxEventStatusEnum;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Unit tests for debit card outbox event registration.
 */
class DebitCardEventPublisherTest {

    private final KafkaTemplate<String, byte[]> kafkaTemplate = mock(KafkaTemplate.class);
    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final DebitCardEventPublisher publisher = new DebitCardEventPublisher(
            kafkaTemplate,
            stringRedisTemplate,
            outboxEventRepository);

    @Test
    void givenDebitCard_whenPublishCreated_thenStoresPendingOutboxEvent() {
        DebitCard card = DebitCard.builder()
                .id("card-1")
                .accountId("account-1")
                .active(true)
                .build();
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        publisher.publishCreated(card)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors();

        org.mockito.Mockito.verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventKey()).isEqualTo("card-1");
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEventStatusEnum.PENDING);
        byte[] payload = Base64.getDecoder().decode(captor.getValue().getPayload());
        DebitCardEventPublisher.DebitCardRegisteredEvent event =
                AvroEventCodec.decodeDebitCardRegistered(payload);
        assertThat(event.debitCardId()).isEqualTo("card-1");
        assertThat(event.accountId()).isEqualTo("account-1");
        assertThat(event.active()).isTrue();
    }
}
