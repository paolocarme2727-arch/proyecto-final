package com.example.bank.customers.business.impl.strategy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bank.customers.domain.Customer;
import com.example.bank.customers.expose.model.CustomerRequest;
import com.example.bank.customers.expose.model.CustomerType;
import com.example.bank.customers.repository.CustomerRepository;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for customer document uniqueness rules.
 */
class UniqueDocumentValidationStrategyTest {

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final UniqueDocumentValidationStrategy strategy =
            new UniqueDocumentValidationStrategy(customerRepository);

    @Test
    void givenExistingDocument_whenValidate_thenRejectsCustomer() {
        CustomerRequest request = new CustomerRequest(CustomerType.PERSONAL, "12345678", "Cliente");

        when(customerRepository.findByDocumentNumber("12345678"))
                .thenReturn(Optional.of(Customer.builder().documentNumber("12345678").build()));

        TestObserver<Void> observer = strategy.validate(request).test();

        observer.awaitDone(5, TimeUnit.SECONDS)
                .assertError(ResponseStatusException.class);
    }
}
