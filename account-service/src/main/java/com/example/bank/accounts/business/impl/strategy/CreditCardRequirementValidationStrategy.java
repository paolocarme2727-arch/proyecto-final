package com.example.bank.accounts.business.impl.strategy;

import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.proxy.CreditProductProxy;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import com.example.bank.accounts.util.enums.CustomerProfileEnum;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates product rules that require an existing credit card.
 */
@Component
@RequiredArgsConstructor
public class CreditCardRequirementValidationStrategy implements AccountCreationValidationStrategy {

    private final CreditProductProxy creditProductProxy;

    /**
     * Validates that VIP savings and PYME checking accounts have a credit card.
     *
     * @param request account request
     * @return validation completion
     */
    @Override
    public Completable validate(AccountRequest request) {
        if (!requiresCreditCard(request)) {
            return Completable.complete();
        }
        return creditProductProxy.customerHasCreditCard(request.getCustomerId())
                .flatMapCompletable(hasCreditCard -> hasCreditCard
                        ? Completable.complete()
                        : Completable.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "El cliente debe tener una tarjeta de crédito para abrir esta cuenta")));
    }

    private boolean requiresCreditCard(AccountRequest request) {
        return (AccountValidationSupport.customerProfile(request) == CustomerProfileEnum.VIP
                && AccountValidationSupport.accountType(request) == AccountTypeEnum.SAVINGS)
                || (AccountValidationSupport.customerProfile(request) == CustomerProfileEnum.PYME
                && AccountValidationSupport.accountType(request) == AccountTypeEnum.CHECKING);
    }
}
