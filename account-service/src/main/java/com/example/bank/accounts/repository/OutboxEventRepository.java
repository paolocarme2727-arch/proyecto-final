package com.example.bank.accounts.repository;

import com.example.bank.accounts.domain.OutboxEvent;
import com.example.bank.accounts.util.enums.OutboxEventStatusEnum;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for outbox events.
 */
public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {

    /**
     * Finds pending events that can be retried now.
     *
     * @param status event status
     * @param nextAttemptAt maximum retry date
     * @return pending outbox events
     */
    List<OutboxEvent> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEventStatusEnum status,
            LocalDateTime nextAttemptAt);
}
