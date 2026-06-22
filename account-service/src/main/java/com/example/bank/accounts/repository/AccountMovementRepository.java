package com.example.bank.accounts.repository;

import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.util.enums.MovementTypeEnum;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for account movements.
 */
public interface AccountMovementRepository extends MongoRepository<AccountMovement, String> {

    /**
     * Finds all movements for one account.
     *
     * @param accountId account identifier
     * @return account movement history
     */
    List<AccountMovement> findByAccountIdOrderByCreatedAtDesc(String accountId);

    /**
     * Finds a limited page of movements for one account.
     *
     * @param accountId account identifier
     * @param pageable page settings
     * @return limited movement history
     */
    List<AccountMovement> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);

    /**
     * Finds a limited page of movements by account and movement type.
     *
     * @param accountId account identifier
     * @param type movement type
     * @param pageable page settings
     * @return limited movement history
     */
    List<AccountMovement> findByAccountIdAndTypeOrderByCreatedAtDesc(String accountId, MovementTypeEnum type, Pageable pageable);

    /**
     * Finds movements for accounts in a time range.
     *
     * @param accountIds account identifiers
     * @param from start date-time
     * @param to end date-time
     * @return matching movements
     */
    List<AccountMovement> findByAccountIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
            Collection<String> accountIds,
            LocalDateTime from,
            LocalDateTime to);
}

