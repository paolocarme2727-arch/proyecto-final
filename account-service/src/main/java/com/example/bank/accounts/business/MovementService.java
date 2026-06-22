package com.example.bank.accounts.business;

import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.expose.model.MoneyRequest;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * Account movement use cases.
 */
public interface MovementService {

    /**
     * Registers a deposit.
     */
    Single<AccountMovement> deposit(String id, MoneyRequest request);

    /**
     * Registers a withdrawal.
     */
    Single<AccountMovement> withdraw(String id, MoneyRequest request);

    /**
     * Lists account movements.
     */
    Flowable<AccountMovement> findMovements(String id);
}
