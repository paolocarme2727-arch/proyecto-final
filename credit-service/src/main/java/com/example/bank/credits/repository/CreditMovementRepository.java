package com.example.bank.credits.repository;

import com.example.bank.credits.domain.CreditMovement;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for credit movements.
 */
public interface CreditMovementRepository extends MongoRepository<CreditMovement, String> {

    /**
     * Finds all movements for one credit product.
     *
     * @param creditProductId product identifier
     * @return movement history
     */
    List<CreditMovement> findByCreditProductIdOrderByCreatedAtDesc(String creditProductId);

    /**
     * Finds a limited page of movements for one credit product.
     *
     * @param creditProductId product identifier
     * @param pageable page settings
     * @return limited movement history
     */
    List<CreditMovement> findByCreditProductIdOrderByCreatedAtDesc(String creditProductId, Pageable pageable);

    /**
     * Finds movements for products in a time range.
     *
     * @param creditProductIds credit product identifiers
     * @param from start date-time
     * @param to end date-time
     * @return matching movements
     */
    List<CreditMovement> findByCreditProductIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
            Collection<String> creditProductIds,
            LocalDateTime from,
            LocalDateTime to);
}

