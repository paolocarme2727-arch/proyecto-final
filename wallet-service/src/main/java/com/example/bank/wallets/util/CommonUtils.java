package com.example.bank.wallets.util;

import lombok.experimental.UtilityClass;

/**
 * Common utility methods.
 */
@UtilityClass
public class CommonUtils {

    /**
     * Checks if a value has text.
     *
     * @param value text value
     * @return true when non-null and non-blank
     */
    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
