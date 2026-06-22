package com.example.bank.accounts.business.impl.strategy;

import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.util.enums.CustomerProfileEnum;
import com.example.bank.accounts.util.enums.CustomerTypeEnum;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates that the PYME profile is only used by business customers.
 */
@Component
public class PymeProfileValidationStrategy implements AccountValidationStrategy {

    /**
     * Applies the PYME profile validation.
     */
    @Override
    public void validate(AccountRequest request) {
        if (AccountValidationSupport.customerProfile(request) == CustomerProfileEnum.PYME
                && AccountValidationSupport.customerType(request) != CustomerTypeEnum.BUSINESS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El perfil PYME está disponible solo para clientes empresariales");
        }
    }
}
