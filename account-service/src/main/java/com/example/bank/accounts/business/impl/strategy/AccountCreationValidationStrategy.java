package com.example.bank.accounts.business.impl.strategy;

import com.example.bank.accounts.expose.model.AccountRequest;
import io.reactivex.rxjava3.core.Completable;

/**
 * Validates one account creation rule that may depend on repositories or external services.
 */
public interface AccountCreationValidationStrategy {

    /**
     * Applies one account creation validation rule.
     *
     * @param request account request
     * @return validation completion
     */
    Completable validate(AccountRequest request);
}
