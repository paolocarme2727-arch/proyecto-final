package com.example.bank.accounts.domain;

import com.example.bank.accounts.util.enums.OutboxEventStatusEnum;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Event stored before being published to external brokers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "outbox_events")
public class OutboxEvent {

    @Id
    private String id;
    @Indexed
    private String topic;
    @Indexed
    private String eventKey;
    private String eventType;
    private String payload;
    @Indexed
    private OutboxEventStatusEnum status;
    private int attempts;
    private String lastError;
    @Indexed
    private LocalDateTime nextAttemptAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
