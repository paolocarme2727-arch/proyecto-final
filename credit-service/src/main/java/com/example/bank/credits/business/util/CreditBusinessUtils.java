package com.example.bank.credits.business.util;

import com.example.bank.credits.expose.model.CreditProductRequest;
import com.example.bank.credits.util.enums.CreditProductType;
import com.example.bank.credits.util.enums.CustomerType;
import java.math.BigDecimal;

/**
 * Utility methods for credit business conversions.
 */
public final class CreditBusinessUtils {

    private CreditBusinessUtils() {
    }

    /**
     * Converts the API customer type to the domain customer type.
     *
     * @param request credit product request
     * @return domain customer type
     */
    public static CustomerType toDomainCustomerType(CreditProductRequest request) {
        return CustomerType.valueOf(request.getCustomerType().getValue());
    }

    /**
     * Converts the API product type to the domain product type.
     *
     * @param request credit product request
     * @return domain credit product type
     */
    public static CreditProductType toDomainProductType(CreditProductRequest request) {
        return CreditProductType.valueOf(request.getType().getValue());
    }

    /**
     * Resolves the initial debt from request or zero.
     *
     * @param request credit product request
     * @return initial debt
     */
    public static BigDecimal initialDebt(CreditProductRequest request) {
        return request.getInitialDebt() == null ? BigDecimal.ZERO : request.getInitialDebt();
    }

    /**
     * Checks if a credit product type is a credit card.
     *
     * @param type credit product type
     * @return true when it is a card
     */
    public static boolean isCard(CreditProductType type) {
        return type == CreditProductType.PERSONAL_CREDIT_CARD
                || type == CreditProductType.BUSINESS_CREDIT_CARD;
    }
}
