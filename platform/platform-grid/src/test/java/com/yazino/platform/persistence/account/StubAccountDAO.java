package com.yazino.platform.persistence.account;

import com.yazino.platform.model.account.Account;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class StubAccountDAO implements AccountDAO {

    private final Set<Account> accounts = new HashSet<Account>();

    @Override
    public void saveAccount(Account account) {
        accounts.add(account);
    }

    @Override
    public Account findById(BigDecimal accountId) {
        for (Account account : accounts) {
            if (account.getAccountId().equals(accountId)) {
                return account;
            }
        }
        return null;
    }
}
