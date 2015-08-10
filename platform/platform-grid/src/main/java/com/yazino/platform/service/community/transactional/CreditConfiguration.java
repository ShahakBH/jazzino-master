package com.yazino.platform.service.community.transactional;

import java.math.BigDecimal;

public class CreditConfiguration {

    private final BigDecimal initialBalance;
    private final BigDecimal referralAmount;

    public CreditConfiguration(final BigDecimal initialBalance,
                               final BigDecimal referralAmount) {
        this.initialBalance = initialBalance;
        this.referralAmount = referralAmount;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public BigDecimal getReferralAmount() {
        return referralAmount;
    }
}
