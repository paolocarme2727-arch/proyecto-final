package com.example.bank.customers.business.impl;

import com.example.bank.customers.business.CustomerService;
import com.example.bank.customers.domain.Customer;
import com.example.bank.customers.domain.CustomerProfile;
import com.example.bank.customers.repository.CustomerRepository;
import com.example.bank.customers.expose.model.CustomerRequest;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implements customer CRUD operations and validation rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * Creates a customer when its document number is unique.
     */
    @Override
    public Single<Customer> create(CustomerRequest request) {
        return Single.fromCallable(() -> {
                    if (customerRepository.findByDocumentNumber(request.getDocumentNumber()).isPresent()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer document already exists");
                    }
                    return customerRepository.save(toNewCustomer(request));
                })
                .subscribeOn(Schedulers.io())
                .doOnSuccess(customer -> log.info("Created customer {}", customer.getId()));
    }

    /**
     * Returns every customer document.
     */
    @Override
    public Flowable<Customer> findAll() {
        return Single.fromCallable(customerRepository::findAll)
                .subscribeOn(Schedulers.io())
                .flattenAsFlowable(customers -> customers);
    }

    /**
     * Returns a customer or raises a 404 error.
     */
    @Override
    public Single<Customer> findById(String id) {
        return findExisting(id);
    }

    /**
     * Updates mutable customer fields.
     */
    @Override
    public Single<Customer> update(String id, CustomerRequest request) {
        return findExisting(id)
                .flatMap(customer -> {
                    customer.setType(toDomainType(request));
                    customer.setProfile(toDomainProfile(request));
                    customer.setDocumentNumber(request.getDocumentNumber());
                    customer.setLegalName(request.getLegalName());
                    customer.setEmail(request.getEmail());
                    customer.setPhoneNumber(request.getPhoneNumber());
                    customer.setUpdatedAt(LocalDateTime.now());
                    return Single.fromCallable(() -> customerRepository.save(customer));
                })
                .subscribeOn(Schedulers.io())
                .doOnSuccess(customer -> log.info("Updated customer {}", customer.getId()));
    }

    /**
     * Deletes a customer if it exists.
     */
    @Override
    public Single<Boolean> delete(String id) {
        return findExisting(id)
                .map(customer -> {
                    customerRepository.delete(customer);
                    return Boolean.TRUE;
                })
                .subscribeOn(Schedulers.io())
                .doOnSuccess(ignored -> log.info("Deleted customer {}", id));
    }

    private Single<Customer> findExisting(String id) {
        return Single.fromCallable(() -> customerRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found")))
                .subscribeOn(Schedulers.io());
    }

    private Customer toNewCustomer(CustomerRequest request) {
        LocalDateTime now = LocalDateTime.now();
        return Customer.builder()
                .type(toDomainType(request))
                .profile(toDomainProfile(request))
                .documentNumber(request.getDocumentNumber())
                .legalName(request.getLegalName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private com.example.bank.customers.domain.CustomerType toDomainType(CustomerRequest request) {
        return com.example.bank.customers.domain.CustomerType.valueOf(request.getType().getValue());
    }

    private CustomerProfile toDomainProfile(CustomerRequest request) {
        return request.getProfile() == null ? CustomerProfile.REGULAR : CustomerProfile.valueOf(request.getProfile().getValue());
    }
}


