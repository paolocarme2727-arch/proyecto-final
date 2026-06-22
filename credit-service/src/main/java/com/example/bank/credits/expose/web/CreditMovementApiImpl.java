package com.example.bank.credits.expose.web;

import static com.example.bank.credits.util.ResponseUtils.resolve;

import com.example.bank.credits.business.CreditProductService;
import com.example.bank.credits.expose.model.CreditMovement;
import com.example.bank.credits.expose.model.MoneyRequest;
import com.example.bank.credits.mappers.CreditProductMapper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated credit movement REST contract.
 */
@RestController
@RequiredArgsConstructor
public class CreditMovementApiImpl implements CreditMovementApi {

    private final CreditProductService creditProductService;
    private final CreditProductMapper creditProductMapper;

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
     * Lists the last 10 movements of a credit card product.
     */
    @Override
    public ResponseEntity<List<CreditMovement>> findRecentCreditCardMovements(String id) {
        return resolve(() -> creditProductService.findRecentCreditCardMovements(id)
                .map(creditProductMapper::toApiMovement)
                .toList()
                .map(ResponseEntity::ok));
    }
}
