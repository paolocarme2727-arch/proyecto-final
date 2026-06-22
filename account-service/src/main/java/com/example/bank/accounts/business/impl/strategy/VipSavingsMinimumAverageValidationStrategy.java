package com.example.bank.accounts.business.impl.strategy;

import com.example.bank.accounts.config.AccountProperties;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import com.example.bank.accounts.util.enums.CustomerProfileEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates the required opening amount for VIP savings accounts.
 */
@Component
@RequiredArgsConstructor
public class VipSavingsMinimumAverageValidationStrategy implements AccountValidationStrategy {

    private final AccountProperties properties;

    /**
     * Applies the VIP savings minimum average validation.
     */
    @Override
    public void validate(AccountRequest request) {
        boolean vipSavings = AccountValidationSupport.customerProfile(request) == CustomerProfileEnum.VIP
                && AccountValidationSupport.accountType(request) == AccountTypeEnum.SAVINGS;
        if (vipSavings
                && AccountValidationSupport.initialBalance(request)
                        .compareTo(properties.vipMonthlyMinimumAverageAmount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cuenta de ahorro VIP requiere el monto mínimo de promedio diario al abrirse");
        }
    }
}
