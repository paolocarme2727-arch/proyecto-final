package com.example.bank.customers.business.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bank.customers.domain.Customer;
import com.example.bank.customers.repository.CustomerRepository;
import com.example.bank.customers.expose.model.CustomerProfile;
import com.example.bank.customers.expose.model.CustomerRequest;
import com.example.bank.customers.expose.model.CustomerType;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for customer business rules.
 */
class CustomerServiceImplTest {

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final CustomerServiceImpl customerService = new CustomerServiceImpl(customerRepository);

    @BeforeEach
    void useImmediateScheduler() {
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    @AfterEach
    void resetSchedulers() {
        RxJavaPlugins.reset();
    }

    /**
     * Verifies that a unique customer is persisted with its profile.
     */
    @Test
    void givenUniqueCustomer_whenCreate_thenPersistsCustomerProfile() {
        CustomerRequest request = new CustomerRequest(CustomerType.PERSONAL, "12345678", "Ada Lovelace")
                .profile(CustomerProfile.VIP)
                .email("ada@example.com");

        when(customerRepository.findByDocumentNumber("12345678")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestObserver<Customer> observer = customerService.create(request).test();

        observer.awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(created -> {
                    assertThat(created.getProfile()).isEqualTo(com.example.bank.customers.domain.CustomerProfile.VIP);
                    assertThat(created.getDocumentNumber()).isEqualTo("12345678");
                    return true;
                });
    }
}

