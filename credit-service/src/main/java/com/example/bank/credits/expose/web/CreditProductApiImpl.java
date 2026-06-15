package com.example.bank.credits.expose.web;

import com.example.bank.credits.business.CreditProductService;
import com.example.bank.credits.mappers.CreditProductMapper;
import com.example.bank.credits.expose.model.CreditBalanceResponse;
import com.example.bank.credits.expose.model.CreditCardExistenceResponse;
import com.example.bank.credits.expose.model.CreditMovement;
import com.example.bank.credits.expose.model.CreditProduct;
import com.example.bank.credits.expose.model.CreditProductReportResponse;
import com.example.bank.credits.expose.model.CreditProductRequest;
import com.example.bank.credits.expose.model.CreditProductType;
import com.example.bank.credits.expose.model.MoneyRequest;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implements the OpenAPI-generated credit REST contract.
 */
@RestController
@RequiredArgsConstructor
public class CreditProductApiImpl implements CreditProductApi {

    private final CreditProductService creditProductService;
    private final CreditProductMapper creditProductMapper;

    /**
     * Creates a credit product.
     */
    @Override
    public ResponseEntity<CreditProduct> createCreditProduct(@Valid CreditProductRequest creditProductRequest) {
        return resolve(() -> creditProductService.create(creditProductRequest)
                .map(creditProductMapper::toApiProduct)
                .map(product -> ResponseEntity.status(HttpStatus.CREATED).body(product)));
    }

    /**
     * Lists credit products.
     */
    @Override
    public ResponseEntity<List<CreditProduct>> findAllCreditProducts() {
        return resolve(() -> creditProductService.findAll()
                .map(creditProductMapper::toApiProduct)
                .toList()
                .map(ResponseEntity::ok));
    }

    /**
     * Finds one credit product.
     */
    @Override
    public ResponseEntity<CreditProduct> findCreditProductById(String id) {
        return resolve(() -> creditProductService.findById(id)
                .map(creditProductMapper::toApiProduct)
                .map(ResponseEntity::ok));
    }

    /**
     * Updates product data.
     */
    @Override
    public ResponseEntity<CreditProduct> updateCreditProduct(
            String id,
            @Valid CreditProductRequest creditProductRequest) {
        return resolve(() -> creditProductService.update(id, creditProductRequest)
                .map(creditProductMapper::toApiProduct)
                .map(ResponseEntity::ok));
    }

    /**
     * Deletes a credit product.
     */
    @Override
    public ResponseEntity<Void> deleteCreditProduct(String id) {
        return resolve(() -> creditProductService.delete(id)
                .map(ignored -> ResponseEntity.noContent().build()));
    }

    /**
     * Registers a credit payment.
     */
    @Override
    public ResponseEntity<CreditMovement> payCreditProduct(String id, @Valid MoneyRequest moneyRequest) {
        return resolve(() -> creditProductService.pay(id, moneyRequest)
                .map(creditProductMapper::toApiMovement)
                .map(ResponseEntity::ok));
    }

    /**
     * Charges a credit card.
     */
    @Override
    public ResponseEntity<CreditMovement> chargeCreditCard(String id, @Valid MoneyRequest moneyRequest) {
        return resolve(() -> creditProductService.charge(id, moneyRequest)
                .map(creditProductMapper::toApiMovement)
                .map(ResponseEntity::ok));
    }

    /**
     * Returns credit balance.
     */
    @Override
    public ResponseEntity<CreditBalanceResponse> getCreditBalance(String id) {
        return resolve(() -> creditProductService.getBalance(id)
                .map(ResponseEntity::ok));
    }

    /**
     * Lists credit movements.
     */
    @Override
    public ResponseEntity<List<CreditMovement>> findCreditMovements(String id) {
        return resolve(() -> creditProductService.findMovements(id)
                .map(creditProductMapper::toApiMovement)
                .toList()
                .map(ResponseEntity::ok));
    }

    /**
     * Checks if a customer has an active credit card product.
     */
    @Override
    public ResponseEntity<CreditCardExistenceResponse> customerHasCreditCard(String customerId) {
        return resolve(() -> creditProductService.customerHasCreditCard(customerId)
                .map(ResponseEntity::ok));
    }

    /**
     * Returns a credit product report in a user-provided time range.
     */
    @Override
    public ResponseEntity<CreditProductReportResponse> getCreditProductReport(
            CreditProductType type,
            OffsetDateTime from,
            OffsetDateTime to) {
        return resolve(() -> creditProductService.getProductReport(type, from, to)
                .map(ResponseEntity::ok));
    }

    /**
     * Lists the last 10 movements of a credit card product.
     */
    @Override
    public ResponseEntity<List<CreditMovement>> findRecentCreditCardMovements(String id) {
        return resolve(() -> creditProductService.findRecentCreditCardMovements(id)
                .map(creditProductMapper::toApiMovement)
                .toList()
                .map(ResponseEntity::ok));
    }

    private static <T> ResponseEntity<T> resolve(Supplier<Single<ResponseEntity<T>>> responseSupplier) {
        try {
            return responseSupplier.get().blockingGet();
        } catch (RuntimeException error) {
            ResponseStatusException exception = findStatusException(error);
            if (exception != null) {
                return ResponseEntity.status(exception.getStatusCode()).build();
            }
            throw error;
        }
    }

    private static ResponseStatusException findStatusException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ResponseStatusException exception) {
                return exception;
            }
            current = current.getCause();
        }
        return null;
    }
}

