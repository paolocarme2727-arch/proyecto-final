package com.example.bank.customers.mappers;

import com.example.bank.customers.expose.model.Customer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * Maps customer domain documents to OpenAPI response models.
 */
@Component
public class CustomerMapper {

    /**
     * Converts a domain customer into the generated API model.
     */
    public Customer toApiCustomer(com.example.bank.customers.domain.Customer customer) {
        return new Customer()
                .id(customer.getId())
                .type(com.example.bank.customers.expose.model.CustomerType.fromValue(customer.getType().name()))
                .profile(com.example.bank.customers.expose.model.CustomerProfile.fromValue(
                        customer.getProfile() == null ? "REGULAR" : customer.getProfile().name()))
                .documentNumber(customer.getDocumentNumber())
                .legalName(customer.getLegalName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .createdAt(toOffsetDateTime(customer.getCreatedAt()))
                .updatedAt(toOffsetDateTime(customer.getUpdatedAt()));
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}

