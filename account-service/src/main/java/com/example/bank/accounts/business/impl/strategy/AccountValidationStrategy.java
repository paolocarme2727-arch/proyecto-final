package com.example.bank.accounts.business.impl.strategy;

import com.example.bank.accounts.expose.model.AccountRequest;

/**
 * Validates one account creation or update rule.
 */
public interface AccountValidationStrategy {

    /**
     * Applies one account validation rule.
     *
     * @param request account request
     */
    void validate(AccountRequest request);
}
