package com.example.bank.accounts.business.impl;

import com.example.bank.accounts.config.AccountProperties;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;

/**
 * Shared fixtures for account business unit tests.
 */
final class AccountBusinessTestSupport {

    private AccountBusinessTestSupport() {
    }

    static AccountProperties properties() {
        return new AccountProperties(
                20,
                BigDecimal.valueOf(15),
                BigDecimal.ZERO,
                20,
                BigDecimal.valueOf(3.5),
                BigDecimal.valueOf(500));
    }

    static void useImmediateScheduler() {
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    static void resetSchedulers() {
        RxJavaPlugins.reset();
    }
}
