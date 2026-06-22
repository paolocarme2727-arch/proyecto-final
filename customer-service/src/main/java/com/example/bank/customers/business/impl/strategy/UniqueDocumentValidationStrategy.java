package com.example.bank.customers.business.impl.strategy;

import com.example.bank.customers.expose.model.CustomerRequest;
import com.example.bank.customers.repository.CustomerRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates that customer document numbers are unique.
 */
@Component
@RequiredArgsConstructor
public class UniqueDocumentValidationStrategy implements CustomerValidationStrategy {

    private final CustomerRepository customerRepository;

    /**
     * Validates document number uniqueness.
     */
    @Override
    public Completable validate(CustomerRequest request) {
        return Single.fromCallable(() -> customerRepository.findByDocumentNumber(request.getDocumentNumber()).isPresent())
                .flatMapCompletable(exists -> exists
                        ? Completable.error(new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "El documento del cliente ya existe"))
                        : Completable.complete());
    }
}
