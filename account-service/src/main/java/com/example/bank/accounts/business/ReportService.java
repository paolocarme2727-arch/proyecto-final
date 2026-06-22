package com.example.bank.accounts.business;

import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.expose.model.AccountProductReportResponse;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.OffsetDateTime;

/**
 * Account reporting use cases.
 */
public interface ReportService {

    /**
     * Builds a product report in the requested time range.
     */
    Single<AccountProductReportResponse> getProductReport(
            com.example.bank.accounts.expose.model.AccountType type,
            OffsetDateTime from,
            OffsetDateTime to);

    /**
     * Lists the last 10 debit card movements for an account.
     */
    Flowable<AccountMovement> findRecentDebitCardMovements(String id);
}
