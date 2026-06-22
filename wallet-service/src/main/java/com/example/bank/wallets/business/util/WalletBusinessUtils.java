package com.example.bank.wallets.business.util;

import com.example.bank.wallets.expose.model.WalletRequest;
import com.example.bank.wallets.util.enums.DocumentType;

/**
 * Utility methods for wallet business conversions.
 */
public final class WalletBusinessUtils {

    private WalletBusinessUtils() {
    }

    /**
     * Converts the API document type to the domain document type.
     *
     * @param request wallet request
     * @return domain document type
     */
    public static DocumentType toDomainDocumentType(WalletRequest request) {
        return DocumentType.valueOf(request.getDocumentType().getValue());
    }
}
