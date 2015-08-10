package com.yazino.platform.repository.account;

import com.yazino.platform.model.account.Account;
import com.yazino.platform.persistence.account.AccountLoadType;

import java.math.BigDecimal;

public interface AccountRepository {

    Account findById(BigDecimal accountId);

    Account findById(BigDecimal accountId, AccountLoadType loadType);

    void save(Account account);

    void remove(Account account);

    Account lock(BigDecimal accountId);

}
