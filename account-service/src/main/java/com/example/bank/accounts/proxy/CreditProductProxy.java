package com.example.bank.accounts.proxy;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client used by account rules to verify credit card ownership.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditProductProxy {

    private final RestClient.Builder restClientBuilder;

    @Value("${banking.services.credit-url:http://localhost:8083}")
    private String creditServiceUrl;

    /**
     * Checks whether a customer has any credit card product.
     *
     * @param customerId customer identifier
     * @return true when the customer has a credit card
     */
    @CircuitBreaker(name = "creditService", fallbackMethod = "creditCardFallback")
    public Single<Boolean> customerHasCreditCard(String customerId) {
        return Single.fromCallable(() -> restClientBuilder.build()
                        .get()
                        .uri(creditServiceUrl + "/api/v1/credits/customers/{customerId}/credit-card-exists", customerId)
                        .retrieve()
                        .body(CreditCardExistenceResponse.class))
                .map(response -> response != null && response.hasCreditCard())
                .timeout(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(throwable -> creditCardFallback(customerId, throwable));
    }

    private Single<Boolean> creditCardFallback(String customerId, Throwable throwable) {
        log.warn("Falló la validación de tarjeta de crédito para el cliente {}", customerId, throwable);
        return Single.just(Boolean.FALSE);
    }

    private record CreditCardExistenceResponse(boolean hasCreditCard) {
    }
}
