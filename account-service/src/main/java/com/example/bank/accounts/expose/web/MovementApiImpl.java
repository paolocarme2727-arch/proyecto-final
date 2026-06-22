package com.example.bank.accounts.expose.web;

import static com.example.bank.accounts.util.ResponseUtils.resolve;

import com.example.bank.accounts.business.MovementService;
import com.example.bank.accounts.expose.model.AccountMovement;
import com.example.bank.accounts.expose.model.MoneyRequest;
import com.example.bank.accounts.mappers.BankAccountMapper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated movement contract.
 */
@RestController
@RequiredArgsConstructor
public class MovementApiImpl implements MovementApi {

    private final MovementService movementService;
    private final BankAccountMapper accountMapper;

    /**
     * Deposits money.
     */
    @Override
    public ResponseEntity<AccountMovement> depositAccount(String id, @Valid MoneyRequest moneyRequest) {
        return resolve(() -> movementService.deposit(id, moneyRequest)
                .map(accountMapper::toApiMovement)
                .map(ResponseEntity::ok));
    }

    /**
     * Withdraws money.
     */
    @Override
    public ResponseEntity<AccountMovement> withdrawAccount(String id, @Valid MoneyRequest moneyRequest) {
        return resolve(() -> movementService.withdraw(id, moneyRequest)
                .map(accountMapper::toApiMovement)
                .map(ResponseEntity::ok));
    }

    /**
     * Lists account movements.
     */
    @Override
    public ResponseEntity<List<AccountMovement>> findAccountMovements(String id) {
        return resolve(() -> movementService.findMovements(id)
                .map(accountMapper::toApiMovement)
                .toList()
                .map(ResponseEntity::ok));
    }
}
