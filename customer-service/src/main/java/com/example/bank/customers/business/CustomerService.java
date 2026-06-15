package com.example.bank.customers.business;

import com.example.bank.customers.domain.Customer;
import com.example.bank.customers.expose.model.CustomerRequest;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * Customer use cases exposed through RxJava types.
 */
public interface CustomerService {

    /**
     * Creates a new customer.
     *
     * @param request customer data
     * @return created customer
     */
    Single<Customer> create(CustomerRequest request);

    /**
     * Lists all customers.
     *
     * @return customer stream
     */
    Flowable<Customer> findAll();

    /**
     * Finds a customer by id.
     *
     * @param id customer identifier
     * @return matching customer
     */
    Single<Customer> findById(String id);

    /**
     * Updates an existing customer.
     *
     * @param id customer identifier
     * @param request replacement data
     * @return updated customer
     */
    Single<Customer> update(String id, CustomerRequest request);

    /**
     * Deletes a customer.
     *
     * @param id customer identifier
     * @return completion signal
     */
    Single<Boolean> delete(String id);
}


