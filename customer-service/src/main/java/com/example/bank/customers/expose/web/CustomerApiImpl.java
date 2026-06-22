package com.example.bank.customers.expose.web;

import static com.example.bank.customers.util.ResponseUtils.resolve;

import com.example.bank.customers.business.CustomerService;
import com.example.bank.customers.mappers.CustomerMapper;
import com.example.bank.customers.expose.model.Customer;
import com.example.bank.customers.expose.model.CustomerRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated customer REST contract.
 */
@RestController
@RequiredArgsConstructor
public class CustomerApiImpl implements CustomerApi {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    /**
     * Creates a customer.
     */
    @Override
    public ResponseEntity<Customer> createCustomer(@Valid CustomerRequest customerRequest) {
        return resolve(() -> customerService.create(customerRequest)
                .map(customerMapper::toApiCustomer)
                .map(customer -> ResponseEntity.status(HttpStatus.CREATED).body(customer)));
    }

    /**
     * Lists customers.
     */
    @Override
    public ResponseEntity<List<Customer>> findAllCustomers() {
        return resolve(() -> customerService.findAll()
                .map(customerMapper::toApiCustomer)
                .toList()
                .map(ResponseEntity::ok));
    }

    /**
     * Finds one customer.
     */
    @Override
    public ResponseEntity<Customer> findCustomerById(String id) {
        return resolve(() -> customerService.findById(id)
                .map(customerMapper::toApiCustomer)
                .map(ResponseEntity::ok));
    }

    /**
     * Updates a customer.
     */
    @Override
    public ResponseEntity<Customer> updateCustomer(String id, @Valid CustomerRequest customerRequest) {
        return resolve(() -> customerService.update(id, customerRequest)
                .map(customerMapper::toApiCustomer)
                .map(ResponseEntity::ok));
    }

    /**
     * Deletes a customer.
     */
    @Override
    public ResponseEntity<Void> deleteCustomer(String id) {
        return resolve(() -> customerService.delete(id)
                .map(ignored -> ResponseEntity.noContent().build()));
    }
}

