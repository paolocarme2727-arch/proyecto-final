package com.example.bank.customers.business.impl;

import com.example.bank.customers.business.CustomerService;
import com.example.bank.customers.business.domain.CustomerDomainService;
import com.example.bank.customers.business.impl.strategy.CustomerValidationStrategy;
import com.example.bank.customers.business.util.CustomerBusinessUtils;
import com.example.bank.customers.domain.Customer;
import com.example.bank.customers.expose.model.CustomerRequest;
import com.example.bank.customers.repository.CustomerRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implements customer CRUD operations and validation rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerDomainService customerDomainService;
    private final List<CustomerValidationStrategy> customerValidationStrategies;

    /**
     * Creates a customer when its document number is unique.
     */
    @Override
    public Single<Customer> create(CustomerRequest request) {
        return validateCustomer(request)
                .andThen(Single.fromCallable(() -> customerRepository.save(toNewCustomer(request))))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(customer -> log.info("Cliente creado {}", customer.getId()));
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
        return customerDomainService.findExistingCustomerReactive(id);
    }

    /**
     * Updates mutable customer fields.
     */
    @Override
    public Single<Customer> update(String id, CustomerRequest request) {
        return customerDomainService.findExistingCustomerReactive(id)
                .flatMap(customer -> Single.fromCallable(() -> customerRepository.save(
                        applyCustomerUpdate(customer, request))))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(customer -> log.info("Cliente actualizado {}", customer.getId()));
    }

    /**
     * Deletes a customer if it exists.
     */
    @Override
    public Single<Boolean> delete(String id) {
        return customerDomainService.findExistingCustomerReactive(id)
                .flatMap(customer -> Single.fromCallable(() -> {
                    customerRepository.delete(customer);
                    return Boolean.TRUE;
                }))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(ignored -> log.info("Cliente eliminado {}", id));
    }

    private Completable validateCustomer(CustomerRequest request) {
        return Completable.merge(customerValidationStrategies.stream()
                .map(strategy -> strategy.validate(request))
                .toList());
    }

    private Customer toNewCustomer(CustomerRequest request) {
        LocalDateTime now = LocalDateTime.now();
        return Customer.builder()
                .type(CustomerBusinessUtils.toDomainType(request))
                .profile(CustomerBusinessUtils.toDomainProfile(request))
                .documentNumber(request.getDocumentNumber())
                .legalName(request.getLegalName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Customer applyCustomerUpdate(Customer customer, CustomerRequest request) {
        customer.setType(CustomerBusinessUtils.toDomainType(request));
        customer.setProfile(CustomerBusinessUtils.toDomainProfile(request));
        customer.setDocumentNumber(request.getDocumentNumber());
        customer.setLegalName(request.getLegalName());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setUpdatedAt(LocalDateTime.now());
        return customer;
    }
}
