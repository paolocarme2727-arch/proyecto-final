package com.example.bank.accounts.business.impl.strategy;

import com.example.bank.accounts.events.CreditDebtStatusCache;
import com.example.bank.accounts.expose.model.AccountRequest;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rejects account creation when the customer has overdue credit debt.
 */
@Component
@RequiredArgsConstructor
public class NoOverdueDebtValidationStrategy implements AccountCreationValidationStrategy {

    private final CreditDebtStatusCache creditDebtStatusCache;

    /**
     * Validates that the customer has no overdue debt.
     *
     * @param request account request
     * @return validation completion
     */
    @Override
    public Completable validate(AccountRequest request) {
        return creditDebtStatusCache.hasOverdueDebt(request.getCustomerId())
                .flatMapCompletable(hasOverdueDebt -> hasOverdueDebt
                        ? Completable.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "El cliente tiene deuda vencida en un producto de crédito"))
                        : Completable.complete());
    }
}
