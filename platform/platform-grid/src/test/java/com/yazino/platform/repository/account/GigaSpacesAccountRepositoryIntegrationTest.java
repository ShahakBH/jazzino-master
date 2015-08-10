package com.yazino.platform.repository.account;

import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.AccountPersistenceRequest;
import com.yazino.platform.model.account.AccountTransaction;
import com.yazino.platform.model.account.AccountTransactionPersistenceRequest;
import com.yazino.platform.persistence.account.AccountDAO;
import com.yazino.platform.persistence.account.StubAccountDAO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.yazino.platform.persistence.account.AccountLoadType.NULL_IF_ABSENT;
import static com.yazino.platform.persistence.account.AccountLoadType.READ_THROUGH;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaSpacesAccountRepositoryIntegrationTest {

    @Autowired
    private GigaSpace localGigaSpace;
    @Autowired
    private GigaSpace globalGigaSpace;
    @Autowired
    private Routing routing;

    private AccountDAO accountDAO = new StubAccountDAO();
    private GigaSpacesAccountRepository accountRepository;

    private long accountIdSource = 1;
    static final int TIMEOUT = 1001;


    @Before
    public void setUp() {
        accountRepository = new GigaSpacesAccountRepository(localGigaSpace, globalGigaSpace, accountDAO, routing);
    }

    @Test
    @Transactional
    public void shouldPostTransactionsToGigaSpaceOnSave() throws Exception {
        Account account = createAccount(BigDecimal.valueOf(1000));
        AccountTransaction transaction = new AccountTransaction(account.getAccountId(), BigDecimal.TEN, "Stake", "ref");
        account.postTransaction(transaction);
        accountRepository.save(account);
        Account readAccount = localGigaSpace.take(account, TIMEOUT);
        assertEquals(readAccount, account);
        AccountTransactionPersistenceRequest persistenceRequest = localGigaSpace.take(new AccountTransactionPersistenceRequest(), TIMEOUT);
        assertEquals(transaction, new AccountTransaction(persistenceRequest.getAccountTransaction(), null, null));
        AccountPersistenceRequest accountPersistenceRequest = localGigaSpace.take(new AccountPersistenceRequest(), TIMEOUT);
        assertEquals(account.getAccountId(), accountPersistenceRequest.getObjectId());
    }

    @Test
    @Transactional
    public void shouldUseDAOIfAccountNotFound() {
        final Account account = new Account(bd(1001), "Account from DB", bd(5000));
        accountDAO.saveAccount(account);

        accountRepository.findById(account.getAccountId());

        final Account spaceAccount = localGigaSpace.read(new Account(account.getAccountId()));
        assertThat(spaceAccount, is(equalTo(account)));
    }

    @Test
    @Transactional
    public void shouldUseDAOIfAccountNotFoundAndLoadTypeIsReadThrough() {
        final Account account = new Account(bd(1001), "Account from DB", bd(5000));
        accountDAO.saveAccount(account);

        accountRepository.findById(account.getAccountId(), READ_THROUGH);

        final Account spaceAccount = localGigaSpace.read(new Account(account.getAccountId()));
        assertThat(spaceAccount, is(equalTo(account)));
    }

    @Test
    @Transactional
    public void shouldNotUseDAOIfAccountNotFoundAndLoadTypeIsNullIfAbsent() {
        final Account account = new Account(bd(1001), "Account from DB", bd(5000));
        accountDAO.saveAccount(account);

        accountRepository.findById(account.getAccountId(), NULL_IF_ABSENT);

        final Account spaceAccount = localGigaSpace.read(new Account(account.getAccountId()));
        assertThat(spaceAccount, is(nullValue()));
    }

    @Test
    @Transactional
    public void shouldReturnNullIfAccountNotFoundInSpaceAndDao() {
        Account account = accountRepository.findById(bd(23));
        assertThat(account, is(nullValue()));
    }

    private Account createAccount(final BigDecimal balance) {
        final long accountId = accountIdSource++;
        final Account account = new Account(bd(accountId), "Account:" + accountId, balance);
        localGigaSpace.write(account);
        return account;
    }

    private BigDecimal bd(final long value) {
        return BigDecimal.valueOf(value);
    }
}
