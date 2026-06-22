package com.example.bank.credits.expose.web;

import static com.example.bank.credits.util.ResponseUtils.resolve;

import com.example.bank.credits.business.CreditProductService;
import com.example.bank.credits.mappers.CreditProductMapper;
import com.example.bank.credits.expose.model.CreditBalanceResponse;
import com.example.bank.credits.expose.model.CreditCardExistenceResponse;
import com.example.bank.credits.expose.model.CreditProduct;
import com.example.bank.credits.expose.model.CreditProductRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

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
     * Returns credit balance.
     */
    @Override
    public ResponseEntity<CreditBalanceResponse> getCreditBalance(String id) {
        return resolve(() -> creditProductService.getBalance(id)
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

}

