package com.example.bank.customers.repository;

import com.example.bank.customers.domain.Customer;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for customer documents.
 */
public interface CustomerRepository extends MongoRepository<Customer, String> {

    /**
     * Finds an existing customer by its official document number.
     *
     * @param documentNumber official document number
     * @return matching customer when present
     */
    Optional<Customer> findByDocumentNumber(String documentNumber);
}

