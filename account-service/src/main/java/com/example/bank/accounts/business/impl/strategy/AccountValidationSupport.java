package com.example.bank.accounts.business.impl.strategy;

import com.example.bank.accounts.config.AccountProperties;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import com.example.bank.accounts.util.enums.CustomerProfileEnum;
import com.example.bank.accounts.util.enums.CustomerTypeEnum;
import java.math.BigDecimal;

/**
 * Shared conversion helpers for account validation strategies.
 */
final class AccountValidationSupport {

    private AccountValidationSupport() {
    }

    static BigDecimal initialBalance(AccountRequest request) {
        return request.getInitialBalance() == null ? BigDecimal.ZERO : request.getInitialBalance();
    }

    static BigDecimal minimumOpeningAmount(AccountRequest request, AccountProperties properties) {
        return request.getMinimumOpeningAmount() == null
                ? properties.minimumOpeningAmount()
                : request.getMinimumOpeningAmount();
    }

    static CustomerTypeEnum customerType(AccountRequest request) {
        return CustomerTypeEnum.valueOf(request.getCustomerType().getValue());
    }

    static AccountTypeEnum accountType(AccountRequest request) {
        return AccountTypeEnum.valueOf(request.getType().getValue());
    }

    static CustomerProfileEnum customerProfile(AccountRequest request) {
        return request.getCustomerProfile() == null
                ? CustomerProfileEnum.REGULAR
                : CustomerProfileEnum.valueOf(request.getCustomerProfile().getValue());
    }
}
