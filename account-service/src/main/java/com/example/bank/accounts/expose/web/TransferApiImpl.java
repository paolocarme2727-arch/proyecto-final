package com.example.bank.accounts.expose.web;

import static com.example.bank.accounts.util.ResponseUtils.resolve;

import com.example.bank.accounts.business.TransferService;
import com.example.bank.accounts.expose.model.TransferRequest;
import com.example.bank.accounts.expose.model.TransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated transfer contract.
 */
@RestController
@RequiredArgsConstructor
public class TransferApiImpl implements TransferApi {

    private final TransferService transferService;

    /**
     * Transfers money between accounts from the same bank.
     */
    @Override
    public ResponseEntity<TransferResponse> transferBetweenAccounts(@Valid TransferRequest transferRequest) {
        return resolve(() -> transferService.transfer(transferRequest)
                .map(ResponseEntity::ok));
    }
}
