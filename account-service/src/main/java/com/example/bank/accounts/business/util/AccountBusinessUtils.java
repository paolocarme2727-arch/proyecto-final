package com.example.bank.accounts.business.util;

import com.example.bank.accounts.config.AccountProperties;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import com.example.bank.accounts.util.enums.CustomerProfileEnum;
import com.example.bank.accounts.util.enums.CustomerTypeEnum;
import java.math.BigDecimal;

/**
 * Utility methods for account business conversions and configurable defaults.
 */
public final class AccountBusinessUtils {

    private AccountBusinessUtils() {
    }

    /**
     * Converts the API customer type to the domain customer type.
     *
     * @param request account request
     * @return domain customer type
     */
    public static CustomerTypeEnum toDomainCustomerType(AccountRequest request) {
        return CustomerTypeEnum.valueOf(request.getCustomerType().getValue());
    }

    /**
     * Converts the API account type to the domain account type.
     *
     * @param request account request
     * @return domain account type
     */
    public static AccountTypeEnum toDomainAccountType(AccountRequest request) {
        return AccountTypeEnum.valueOf(request.getType().getValue());
    }

    /**
     * Converts the API customer profile to the domain customer profile.
     *
     * @param request account request
     * @return domain customer profile
     */
    public static CustomerProfileEnum toDomainCustomerProfile(AccountRequest request) {
        return request.getCustomerProfile() == null
                ? CustomerProfileEnum.REGULAR
                : CustomerProfileEnum.valueOf(request.getCustomerProfile().getValue());
    }

    /**
     * Resolves the minimum opening amount from request or properties.
     *
     * @param request account request
     * @param properties account properties
     * @return minimum opening amount
     */
    public static BigDecimal resolveMinimumOpeningAmount(
            AccountRequest request,
            AccountProperties properties) {
        return request.getMinimumOpeningAmount() == null
                ? properties.minimumOpeningAmount()
                : request.getMinimumOpeningAmount();
    }

    /**
     * Resolves the minimum daily average amount for VIP savings accounts.
     *
     * @param request account request
     * @param properties account properties
     * @return minimum daily average amount
     */
    public static BigDecimal resolveMinimumDailyAverageAmount(
            AccountRequest request,
            AccountProperties properties) {
        return toDomainCustomerProfile(request) == CustomerProfileEnum.VIP
                && toDomainAccountType(request) == AccountTypeEnum.SAVINGS
                ? properties.vipMonthlyMinimumAverageAmount()
                : BigDecimal.ZERO;
    }
}
