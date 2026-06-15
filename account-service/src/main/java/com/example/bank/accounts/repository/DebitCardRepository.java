package com.example.bank.accounts.repository;

import com.example.bank.accounts.domain.DebitCard;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for debit cards.
 */
public interface DebitCardRepository extends MongoRepository<DebitCard, String> {
}

