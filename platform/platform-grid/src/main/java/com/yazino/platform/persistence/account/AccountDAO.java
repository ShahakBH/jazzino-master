package com.yazino.platform.persistence.account;

import com.yazino.platform.model.account.Account;

import java.math.BigDecimal;

public interface AccountDAO {

    void saveAccount(Account account);

    /**
     * Find an account by ID.
     *
     * @param accountId the ID to find.
     * @return the account or null if not found.
     */
    Account findById(BigDecimal accountId);

}
