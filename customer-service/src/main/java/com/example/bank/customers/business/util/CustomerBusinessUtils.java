package com.example.bank.customers.business.util;

import com.example.bank.customers.expose.model.CustomerRequest;
import com.example.bank.customers.util.enums.CustomerProfile;
import com.example.bank.customers.util.enums.CustomerType;

/**
 * Utility methods for customer business conversions.
 */
public final class CustomerBusinessUtils {

    private CustomerBusinessUtils() {
    }

    /**
     * Converts the API customer type to the domain customer type.
     *
     * @param request customer request
     * @return domain customer type
     */
    public static CustomerType toDomainType(CustomerRequest request) {
        return CustomerType.valueOf(request.getType().getValue());
    }

    /**
     * Converts the API customer profile to the domain customer profile.
     *
     * @param request customer request
     * @return domain customer profile
     */
    public static CustomerProfile toDomainProfile(CustomerRequest request) {
        return request.getProfile() == null
                ? CustomerProfile.REGULAR
                : CustomerProfile.valueOf(request.getProfile().getValue());
    }
}
