package com.example.bank.accounts.business.impl.strategy;

import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import com.example.bank.accounts.util.enums.CustomerTypeEnum;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates that personal customers do not duplicate savings or checking accounts.
 */
@Component
@RequiredArgsConstructor
public class PersonalAccountTypeAvailabilityValidationStrategy implements AccountCreationValidationStrategy {

    private final BankAccountRepository accountRepository;

    /**
     * Validates personal account type availability.
     *
     * @param request account request
     * @return validation completion
     */
    @Override
    public Completable validate(AccountRequest request) {
        if (AccountValidationSupport.customerType(request) != CustomerTypeEnum.PERSONAL
                || AccountValidationSupport.accountType(request) == AccountTypeEnum.FIXED_TERM) {
            return Completable.complete();
        }
        return Single.fromCallable(() -> accountRepository.existsByCustomerIdAndType(
                        request.getCustomerId(),
                        AccountValidationSupport.accountType(request)))
                .flatMapCompletable(exists -> exists
                        ? Completable.error(new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "El cliente personal ya tiene este tipo de cuenta"))
                        : Completable.complete());
    }
}
