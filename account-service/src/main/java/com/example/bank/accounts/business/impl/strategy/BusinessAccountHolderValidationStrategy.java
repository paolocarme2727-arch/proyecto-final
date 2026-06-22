package com.example.bank.accounts.business.impl.strategy;

import static com.example.bank.accounts.util.CommonUtils.defaultList;

import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.util.enums.CustomerTypeEnum;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates the holder requirement for business accounts.
 */
@Component
public class BusinessAccountHolderValidationStrategy implements AccountValidationStrategy {

    /**
     * Applies the business account holder validation.
     */
    @Override
    public void validate(AccountRequest request) {
        if (AccountValidationSupport.customerType(request) == CustomerTypeEnum.BUSINESS
                && defaultList(request.getHolders()).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Las cuentas empresariales requieren al menos un titular");
        }
    }
}
