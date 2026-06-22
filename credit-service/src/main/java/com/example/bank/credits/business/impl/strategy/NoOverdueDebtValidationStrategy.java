package com.example.bank.credits.business.impl.strategy;

import com.example.bank.credits.expose.model.CreditProductRequest;
import com.example.bank.credits.repository.CreditProductRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rejects new products for customers with overdue debt.
 */
@Component
@RequiredArgsConstructor
public class NoOverdueDebtValidationStrategy implements CreditProductCreationValidationStrategy {

    private final CreditProductRepository productRepository;

    /**
     * Validates that the customer has no overdue debt.
     */
    @Override
    public Completable validate(CreditProductRequest request) {
        return Single.fromCallable(() -> productRepository.existsByCustomerIdAndOverdueDebtTrue(
                        request.getCustomerId()))
                .flatMapCompletable(hasDebt -> hasDebt
                        ? Completable.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "El cliente tiene deuda vencida en un producto de crédito"))
                        : Completable.complete());
    }
}
