package com.example.bank.credits.business;

import com.example.bank.credits.domain.CreditMovement;
import com.example.bank.credits.domain.CreditProduct;
import com.example.bank.credits.expose.model.CreditBalanceResponse;
import com.example.bank.credits.expose.model.CreditCardExistenceResponse;
import com.example.bank.credits.expose.model.CreditProductReportResponse;
import com.example.bank.credits.expose.model.CreditProductRequest;
import com.example.bank.credits.expose.model.MoneyRequest;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.OffsetDateTime;

/**
 * Credit product use cases exposed through RxJava types.
 */
public interface CreditProductService {

    /**
     * Creates a credit product.
     */
    Single<CreditProduct> create(CreditProductRequest request);

    /**
     * Lists all credit products.
     */
    Flowable<CreditProduct> findAll();

    /**
     * Finds a credit product by id.
     */
    Single<CreditProduct> findById(String id);

    /**
     * Updates a credit product.
     */
    Single<CreditProduct> update(String id, CreditProductRequest request);

    /**
     * Deletes a credit product.
     */
    Single<Boolean> delete(String id);

    /**
     * Registers a payment.
     */
    Single<CreditMovement> pay(String id, MoneyRequest request);

    /**
     * Charges a credit card.
     */
    Single<CreditMovement> charge(String id, MoneyRequest request);

    /**
     * Returns debt and available credit.
     */
    Single<CreditBalanceResponse> getBalance(String id);

    /**
     * Lists movements for one product.
     */
    Flowable<CreditMovement> findMovements(String id);

    /**
     * Checks whether a customer has a personal or business credit card.
     */
    Single<CreditCardExistenceResponse> customerHasCreditCard(String customerId);

    /**
     * Builds a product report in the requested time range.
     */
    Single<CreditProductReportResponse> getProductReport(
            com.example.bank.credits.expose.model.CreditProductType type,
            OffsetDateTime from,
            OffsetDateTime to);

    /**
     * Lists the last 10 credit card movements.
     */
    Flowable<CreditMovement> findRecentCreditCardMovements(String id);
}


