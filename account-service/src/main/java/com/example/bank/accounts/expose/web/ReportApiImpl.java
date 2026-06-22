package com.example.bank.accounts.expose.web;

import static com.example.bank.accounts.util.ResponseUtils.resolve;

import com.example.bank.accounts.business.ReportService;
import com.example.bank.accounts.expose.model.AccountMovement;
import com.example.bank.accounts.expose.model.AccountProductReportResponse;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.mappers.BankAccountMapper;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated report contract.
 */
@RestController
@RequiredArgsConstructor
public class ReportApiImpl implements ReportApi {

    private final ReportService reportService;
    private final BankAccountMapper accountMapper;

    /**
     * Returns a product report in a user-provided time range.
     */
    @Override
    public ResponseEntity<AccountProductReportResponse> getAccountProductReport(
            AccountType type,
            OffsetDateTime from,
            OffsetDateTime to) {
        return resolve(() -> reportService.getProductReport(type, from, to)
                .map(ResponseEntity::ok));
    }

    /**
     * Lists the last 10 movements associated with a debit-card account.
     */
    @Override
    public ResponseEntity<List<AccountMovement>> findRecentDebitCardMovements(String id) {
        return resolve(() -> reportService.findRecentDebitCardMovements(id)
                .map(accountMapper::toApiMovement)
                .toList()
                .map(ResponseEntity::ok));
    }
}
