package com.example.bank.credits.expose.web;

import static com.example.bank.credits.util.ResponseUtils.resolve;

import com.example.bank.credits.business.CreditProductService;
import com.example.bank.credits.expose.model.CreditProductReportResponse;
import com.example.bank.credits.expose.model.CreditProductType;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated credit report REST contract.
 */
@RestController
@RequiredArgsConstructor
public class CreditReportApiImpl implements CreditReportApi {

    private final CreditProductService creditProductService;

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
}
