package com.example.bank.customers.business.domain;

import com.example.bank.customers.domain.Customer;
import io.reactivex.rxjava3.core.Single;

/**
 * Defines shared customer domain lookups.
 */
public interface CustomerDomainService {

    /**
     * Finds an existing customer or raises a not-found error.
     *
     * @param id customer identifier
     * @return existing customer
     */
    Customer findExistingCustomer(String id);

    /**
     * Finds an existing customer reactively.
     *
     * @param id customer identifier
     * @return existing customer
     */
    Single<Customer> findExistingCustomerReactive(String id);
}
