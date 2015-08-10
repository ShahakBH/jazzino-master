package com.yazino.platform.repository.account;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.AccountPersistenceRequest;
import com.yazino.platform.model.account.AccountTransaction;
import com.yazino.platform.model.account.AccountTransactionPersistenceRequest;
import com.yazino.platform.persistence.account.AccountDAO;
import com.yazino.platform.persistence.account.AccountLoadType;
import net.jini.core.lease.Lease;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;

import static com.yazino.platform.persistence.account.AccountLoadType.READ_THROUGH;
import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaSpacesAccountRepository implements AccountRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaSpacesAccountRepository.class);
    private static final int ONE_SECOND = 1000;

    private int timeout = ONE_SECOND;

    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final AccountDAO accountDao;
    private final Routing routing;

    @Autowired
    public GigaSpacesAccountRepository(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                                       @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                       final AccountDAO accountDao,
                                       final Routing routing) {
        notNull(localGigaSpace, "localGigaSpace may not be null");
        notNull(globalGigaSpace, "globalGigaSpace may not be null");
        notNull(accountDao, "accountDao may not be null");
        notNull(routing, "routing may not be null");

        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
        this.accountDao = accountDao;
        this.routing = routing;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    private GigaSpace spaceFor(final BigDecimal accountId) {
        if (routing.isRoutedToCurrentPartition(accountId)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }

    public Account findById(final BigDecimal accountId) {
        return findById(accountId, READ_THROUGH);
    }

    @Override
    public Account findById(final BigDecimal accountId,
                            final AccountLoadType loadType) {
        notNull(accountId, "accountId may not be null");
        notNull(loadType, "loadType may not be null");

        final GigaSpace spaceReference = spaceFor(accountId);
        Account account = spaceReference.readById(Account.class, accountId, accountId, 0, ReadModifiers.DIRTY_READ);
        if (account == null && loadType == READ_THROUGH) {
            account = accountDao.findById(accountId);
            if (account != null) {
                spaceReference.write(account, Lease.FOREVER, timeout, WriteModifiers.UPDATE_OR_WRITE);
            }
        }

        return account;
    }

    public void save(final Account account) {
        LOG.debug("Saving {}", account);

        final List<Object> objectsToWrite = new ArrayList<Object>();
        objectsToWrite.add(account);
        objectsToWrite.add(new AccountPersistenceRequest(account.getAccountId()));

        final Collection<AccountTransaction> transactions = account.popNewAccountTransactions();
        for (AccountTransaction transaction : transactions) {
            objectsToWrite.add(new AccountTransactionPersistenceRequest(transaction));
        }

        spaceFor(account.getAccountId()).writeMultiple(objectsToWrite.toArray(new Object[objectsToWrite.size()]),
                Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    public void remove(final Account account) {
        notNull(account, "account may not be null");

        LOG.debug("Deleting {}", account);

        spaceFor(account.getAccountId()).clear(account);
    }

    @Transactional("spaceTransactionManager")
    public Account lock(final BigDecimal accountId) {
        notNull(accountId, "accountId may not be null");

        LOG.debug("Locking {} ", accountId);

        if (!routing.isRoutedToCurrentPartition(accountId)) {
            throw new IllegalArgumentException("You cannot lock an account on another partition: ID = " + accountId);
        }

        final Account account = localGigaSpace.readById(Account.class, accountId, accountId, timeout, ReadModifiers.EXCLUSIVE_READ_LOCK);
        if (account == null) {
            throw new ConcurrentModificationException("Cannot obtain lock for account ID " + accountId);
        }
        return account;
    }

}
