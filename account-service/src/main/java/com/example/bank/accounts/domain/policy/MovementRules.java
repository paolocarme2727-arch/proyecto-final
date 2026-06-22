package com.example.bank.accounts.domain.policy;

import com.example.bank.accounts.config.AccountProperties;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import com.example.bank.accounts.util.enums.CustomerProfileEnum;
import com.example.bank.accounts.util.enums.MovementTypeEnum;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Centralizes movement calculations and validations used by account operations.
 */
@Component
@RequiredArgsConstructor
public class MovementRules {

    private final AccountProperties properties;

    /**
     * Validates whether a movement can be applied to an account.
     */
    public void validateMovement(BankAccount account, BigDecimal amount, MovementTypeEnum movementTypeEnum) {
        BigDecimal fee = calculateTransactionFee(account);
        BigDecimal resultingBalance = calculateResultingBalance(account, amount, movementTypeEnum, fee);
        if (resultingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente");
        }
        if (account.getType() == AccountTypeEnum.FIXED_TERM
                && account.getMovementDay() != null
                && account.getMovementDay() != LocalDate.now().getDayOfMonth()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cuenta a plazo fijo solo puede operar en su día configurado");
        }
        if (account.getType() == AccountTypeEnum.SAVINGS
                && account.getMonthlyMovementLimit() != null
                && account.getMonthlyMovementCount() >= account.getMonthlyMovementLimit()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se superó el límite mensual de movimientos de la cuenta de ahorro");
        }
        if (account.getCustomerProfileEnum() == CustomerProfileEnum.VIP
                && account.getType() == AccountTypeEnum.SAVINGS
                && resultingBalance.compareTo(resolveStoredMinimumDailyAverageAmount(account)) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cuenta de ahorro VIP quedaría por debajo del promedio diario mínimo");
        }
    }

    /**
     * Calculates the fee to charge for the next movement.
     */
    public BigDecimal calculateTransactionFee(BankAccount account) {
        int freeLimit = account.getFreeTransactionLimit() == 0
                ? properties.freeTransactionLimit()
                : account.getFreeTransactionLimit();
        BigDecimal fee = account.getTransactionFee() == null ? properties.transactionFee() : account.getTransactionFee();
        return account.getMonthlyMovementCount() >= freeLimit ? fee : BigDecimal.ZERO;
    }

    /**
     * Calculates the resulting balance after applying amount and fee.
     */
    public BigDecimal calculateResultingBalance(
            BankAccount account,
            BigDecimal amount,
            MovementTypeEnum movementTypeEnum,
            BigDecimal fee) {
        BigDecimal signedAmount = isCreditMovement(movementTypeEnum) ? amount : amount.negate();
        return account.getBalance().add(signedAmount).subtract(fee);
    }

    private BigDecimal resolveStoredMinimumDailyAverageAmount(BankAccount account) {
        return account.getMinimumDailyAverageAmount() == null
                ? properties.vipMonthlyMinimumAverageAmount()
                : account.getMinimumDailyAverageAmount();
    }

    private boolean isCreditMovement(MovementTypeEnum movementTypeEnum) {
        return movementTypeEnum == MovementTypeEnum.DEPOSIT
                || movementTypeEnum == MovementTypeEnum.TRANSFER_IN
                || movementTypeEnum == MovementTypeEnum.WALLET_PAYMENT_IN;
    }
}
