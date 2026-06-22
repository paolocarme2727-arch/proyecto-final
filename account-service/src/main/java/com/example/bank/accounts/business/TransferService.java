package com.example.bank.accounts.business;

import com.example.bank.accounts.expose.model.TransferRequest;
import com.example.bank.accounts.expose.model.TransferResponse;
import io.reactivex.rxjava3.core.Single;

/**
 * Account transfer use cases.
 */
public interface TransferService {

    /**
     * Transfers money between two bank accounts in the same bank.
     */
    Single<TransferResponse> transfer(TransferRequest request);
}
