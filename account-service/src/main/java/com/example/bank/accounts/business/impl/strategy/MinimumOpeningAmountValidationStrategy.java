package com.example.bank.accounts.business.impl.strategy;

import com.example.bank.accounts.config.AccountProperties;
import com.example.bank.accounts.expose.model.AccountRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates that the opening balance covers the required minimum amount.
 */
@Component
@RequiredArgsConstructor
    public class MinimumOpeningAmountValidationStrategy implements AccountValidationStrategy {

    private final AccountProperties properties;

    /**
     * Applies the minimum opening amount validation.
     */
    @Override
    public void validate(AccountRequest request) {
        if (AccountValidationSupport.initialBalance(request)
                .compareTo(AccountValidationSupport.minimumOpeningAmount(request, properties)) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El saldo inicial es menor que el monto mínimo de apertura");
        }
    }
}
