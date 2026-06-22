package com.example.bank.wallets.business.impl.strategy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bank.wallets.domain.Wallet;
import com.example.bank.wallets.expose.model.WalletPaymentRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for wallet balance rules.
 */
class WalletBalanceValidationStrategyTest {

    private final WalletBalanceValidationStrategy strategy = new WalletBalanceValidationStrategy();

    @Test
    void givenUnlinkedWalletWithoutBalance_whenValidate_thenRejectsPayment() {
        Wallet source = Wallet.builder().id("source").balance(BigDecimal.ONE).build();
        Wallet target = Wallet.builder().id("target").balance(BigDecimal.ZERO).build();
        WalletPaymentRequest request = new WalletPaymentRequest("900", "901", BigDecimal.TEN);

        assertThatThrownBy(() -> strategy.validate(source, target, request))
                .isInstanceOf(ResponseStatusException.class);
    }
}
