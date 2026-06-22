package com.example.bank.accounts.business.impl.strategy;

import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import com.example.bank.accounts.util.enums.CustomerTypeEnum;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates the account types allowed for business customers.
 */
@Component
public class BusinessAccountTypeValidationStrategy implements AccountValidationStrategy {

    /**
     * Applies the business account type validation.
     */
    @Override
    public void validate(AccountRequest request) {
        if (AccountValidationSupport.customerType(request) == CustomerTypeEnum.BUSINESS
                && AccountValidationSupport.accountType(request) != AccountTypeEnum.CHECKING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los clientes empresariales solo pueden tener cuentas corrientes");
        }
    }
}
