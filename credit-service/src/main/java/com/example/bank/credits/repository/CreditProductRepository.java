package com.example.bank.credits.repository;

import com.example.bank.credits.domain.CreditProduct;
import com.example.bank.credits.domain.CreditProductType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for credit products.
 */
public interface CreditProductRepository extends MongoRepository<CreditProduct, String> {

    /**
     * Checks if a customer already owns a product type.
     *
     * @param customerId customer identifier
     * @param type credit product type
     * @return true when a product exists
     */
    boolean existsByCustomerIdAndType(String customerId, CreditProductType type);

    /**
     * Checks if a customer owns any product type from a collection.
     *
     * @param customerId customer identifier
     * @param types product types to match
     * @return true when any matching product exists
     */
    boolean existsByCustomerIdAndTypeIn(String customerId, Collection<CreditProductType> types);

    /**
     * Checks if a customer has any overdue credit debt.
     *
     * @param customerId customer identifier
     * @return true when overdue debt exists
     */
    boolean existsByCustomerIdAndOverdueDebtTrue(String customerId);

    /**
     * Finds credit products by product type.
     *
     * @param type credit product type
     * @return matching products
     */
    List<CreditProduct> findByType(CreditProductType type);
}

