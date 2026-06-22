package com.example.bank.credits.business.impl.strategy;

import static com.example.bank.credits.business.util.CreditBusinessUtils.toDomainCustomerType;
import static com.example.bank.credits.business.util.CreditBusinessUtils.toDomainProductType;

import com.example.bank.credits.expose.model.CreditProductRequest;
import com.example.bank.credits.repository.CreditProductRepository;
import com.example.bank.credits.util.enums.CreditProductType;
import com.example.bank.credits.util.enums.CustomerType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Allows only one personal loan per personal customer.
 */
@Component
@RequiredArgsConstructor
public class PersonalLoanAvailabilityValidationStrategy implements CreditProductCreationValidationStrategy {

    private final CreditProductRepository productRepository;

    /**
     * Validates personal loan availability.
     */
    @Override
    public Completable validate(CreditProductRequest request) {
        if (toDomainCustomerType(request) != CustomerType.PERSONAL
                || toDomainProductType(request) != CreditProductType.PERSONAL_LOAN) {
            return Completable.complete();
        }
        return Single.fromCallable(() -> productRepository.existsByCustomerIdAndType(
                        request.getCustomerId(),
                        CreditProductType.PERSONAL_LOAN))
                .flatMapCompletable(exists -> exists
                        ? Completable.error(new ResponseStatusException(HttpStatus.CONFLICT,
                                "El cliente personal ya tiene un crédito personal"))
                        : Completable.complete());
    }
}
