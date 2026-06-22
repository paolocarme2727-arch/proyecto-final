package com.example.bank.wallets.business.impl.strategy;

import com.example.bank.wallets.business.util.WalletBusinessUtils;
import com.example.bank.wallets.expose.model.WalletRequest;
import com.example.bank.wallets.service.DocumentTypeCatalog;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates supported wallet document types.
 */
@Component
@RequiredArgsConstructor
public class SupportedDocumentTypeValidationStrategy implements WalletCreationValidationStrategy {

    private final DocumentTypeCatalog documentTypeCatalog;

    /**
     * Validates supported document type.
     */
    @Override
    public Completable validate(WalletRequest request) {
        return documentTypeCatalog.isSupported(WalletBusinessUtils.toDomainDocumentType(request))
                .flatMapCompletable(supported -> supported
                        ? Completable.complete()
                        : Completable.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Tipo de documento no soportado")));
    }
}
