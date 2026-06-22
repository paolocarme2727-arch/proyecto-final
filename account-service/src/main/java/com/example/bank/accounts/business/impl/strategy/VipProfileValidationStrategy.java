package com.example.bank.accounts.business.impl.strategy;

import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.util.enums.CustomerProfileEnum;
import com.example.bank.accounts.util.enums.CustomerTypeEnum;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates that the VIP profile is only used by personal customers.
 */
@Component
public class VipProfileValidationStrategy implements AccountValidationStrategy {

    /**
     * Applies the VIP profile validation.
     */
    @Override
    public void validate(AccountRequest request) {
        if (AccountValidationSupport.customerProfile(request) == CustomerProfileEnum.VIP
                && AccountValidationSupport.customerType(request) != CustomerTypeEnum.PERSONAL) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El perfil VIP está disponible solo para clientes personales");
        }
    }
}
