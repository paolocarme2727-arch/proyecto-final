package com.example.bank.accounts.repository;

import com.example.bank.accounts.domain.AccountType;
import com.example.bank.accounts.domain.BankAccount;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for bank account documents.
 */
public interface BankAccountRepository extends MongoRepository<BankAccount, String> {

    /**
     * Finds all accounts for a customer.
     *
     * @param customerId customer identifier
     * @return customer accounts
     */
    List<BankAccount> findByCustomerId(String customerId);

    /**
     * Checks if a customer already owns an account type.
     *
     * @param customerId customer identifier
     * @param type account type
     * @return true when an account exists
     */
    boolean existsByCustomerIdAndType(String customerId, AccountType type);

    /**
     * Finds accounts by product type.
     *
     * @param type account product type
     * @return matching accounts
     */
    List<BankAccount> findByType(AccountType type);
}

