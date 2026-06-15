package com.example.bank.wallets.repository;

import com.example.bank.wallets.domain.Wallet;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for Yanki wallets.
 */
public interface WalletRepository extends MongoRepository<Wallet, String> {

    /**
     * Finds a wallet by its mobile number.
     *
     * @param phoneNumber mobile phone number
     * @return matching wallet
     */
    Optional<Wallet> findByPhoneNumber(String phoneNumber);

    /**
     * Checks if a mobile number is already registered.
     *
     * @param phoneNumber mobile phone number
     * @return true when the phone exists
     */
    boolean existsByPhoneNumber(String phoneNumber);
}

