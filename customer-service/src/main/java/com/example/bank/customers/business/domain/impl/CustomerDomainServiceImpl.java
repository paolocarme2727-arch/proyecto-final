package com.example.bank.customers.business.domain.impl;

import com.example.bank.customers.business.domain.CustomerDomainService;
import com.example.bank.customers.domain.Customer;
import com.example.bank.customers.repository.CustomerRepository;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provides shared customer domain lookups.
 */
@Service
@RequiredArgsConstructor
public class CustomerDomainServiceImpl implements CustomerDomainService {

    private final CustomerRepository customerRepository;

    /**
     * Finds an existing customer or raises a not-found error.
     */
    @Override
    public Customer findExistingCustomer(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    /**
     * Finds an existing customer reactively.
     */
    @Override
    public Single<Customer> findExistingCustomerReactive(String id) {
        return Single.fromCallable(() -> findExistingCustomer(id))
                .subscribeOn(Schedulers.io());
    }
}
