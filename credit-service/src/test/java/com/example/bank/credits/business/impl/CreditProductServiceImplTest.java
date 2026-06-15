package com.example.bank.credits.business.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bank.credits.events.CreditDebtStatusPublisher;
import com.example.bank.credits.repository.CreditMovementRepository;
import com.example.bank.credits.repository.CreditProductRepository;
import com.example.bank.credits.expose.model.CreditCardExistenceResponse;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for credit product rules.
 */
class CreditProductServiceImplTest {

    private final CreditProductRepository productRepository = mock(CreditProductRepository.class);
    private final CreditMovementRepository movementRepository = mock(CreditMovementRepository.class);
    private final CreditDebtStatusPublisher debtStatusPublisher = mock(CreditDebtStatusPublisher.class);
    private final CreditProductServiceImpl creditProductService = new CreditProductServiceImpl(productRepository, movementRepository, debtStatusPublisher);

    @BeforeEach
    void useImmediateScheduler() {
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    @AfterEach
    void resetSchedulers() {
        RxJavaPlugins.reset();
    }

    /**
     * Verifies that credit card existence is resolved through repository-derived methods.
     */
    @Test
    void givenCustomerWithCreditCard_whenCheckingCardExistence_thenReturnsRepositoryResult() {
        when(productRepository.existsByCustomerIdAndTypeIn(anyString(), anyCollection())).thenReturn(Boolean.TRUE);

        TestObserver<CreditCardExistenceResponse> observer = creditProductService.customerHasCreditCard("customer-1").test();

        observer.awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(response -> {
                    assertThat(response.getCustomerId()).isEqualTo("customer-1");
                    assertThat(response.getHasCreditCard()).isTrue();
                    return true;
                });
    }
}

