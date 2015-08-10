package com.yazino.platform.repository.account;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.error.account.InsufficientFundsException;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.AccountPersistenceRequest;
import com.yazino.platform.model.account.AccountTransaction;
import com.yazino.platform.model.account.AccountTransactionPersistenceRequest;
import com.yazino.platform.persistence.account.AccountDAO;
import com.yazino.platform.persistence.account.AccountLoadType;
import com.yazino.test.ThreadLocalDateTimeUtils;
import net.jini.core.lease.Lease;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaSpacesAccountRepositoryTest {
    private static final BigDecimal LOCAL_ID = BigDecimal.valueOf(1);
    private static final BigDecimal REMOTE_ID = BigDecimal.valueOf(2);

    @Mock
    private GigaSpace localSpace;
    @Mock
    private GigaSpace globalSpace;
    @Mock
    private AccountDAO accountDao;
    @Mock
    private Routing routing;

    private GigaSpacesAccountRepository underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100000);

        when(routing.isRoutedToCurrentPartition(LOCAL_ID)).thenReturn(true);

        underTest = new GigaSpacesAccountRepository(localSpace, globalSpace, accountDao, routing);
    }

    @After
    public void resetDate() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void repositoryCannotBeCreatedWithANullLocalSpace() {
        new GigaSpacesAccountRepository(null, globalSpace, accountDao, routing);
    }

    @Test(expected = NullPointerException.class)
    public void repositoryCannotBeCreatedWithANullGlobalSpace() {
        new GigaSpacesAccountRepository(localSpace, null, accountDao, routing);
    }

    @Test(expected = NullPointerException.class)
    public void repositoryCannotBeCreatedWithANullAccountDAO() {
        new GigaSpacesAccountRepository(localSpace, globalSpace, null, routing);
    }

    @Test(expected = NullPointerException.class)
    public void repositoryCannotBeCreatedWithANullRouting() {
        new GigaSpacesAccountRepository(localSpace, globalSpace, accountDao, null);
    }

    @Test(expected = NullPointerException.class)
    public void findingANullAccountIdThrowsANullPointerException() {
        underTest.findById(null);
    }

    @Test
    public void anAccountIsReadFromTheLocalSpaceWhenTheIdRoutesLocallyAndTheAccountIsInTheGrid() throws InsufficientFundsException {
        when(localSpace.readById(Account.class, LOCAL_ID, LOCAL_ID, 0, ReadModifiers.DIRTY_READ)).thenReturn(anAccountWithId(LOCAL_ID));

        assertThat(underTest.findById(LOCAL_ID), is(equalTo(anAccountWithId(LOCAL_ID))));
        verifyZeroInteractions(globalSpace);
    }

    @Test
    public void anAccountIsReadFromTheDBWhenTheIdRoutesLocallyAndTheAccountIsNotInTheGrid() throws InsufficientFundsException {
        when(accountDao.findById(LOCAL_ID)).thenReturn(anAccountWithId(LOCAL_ID));

        assertThat(underTest.findById(LOCAL_ID), is(equalTo(anAccountWithId(LOCAL_ID))));
        verifyZeroInteractions(globalSpace);
    }

    @Test
    public void anAccountThatDoesNotExistInTheDBIsNotWrittenIntoTheSpace() {
        assertThat(underTest.findById(LOCAL_ID), is(nullValue()));
        verify(localSpace).readById(Account.class, LOCAL_ID, LOCAL_ID, 0, ReadModifiers.DIRTY_READ);
        verifyNoMoreInteractions(localSpace);
        verifyZeroInteractions(globalSpace);
    }

    @Test
    public void anAccountIsWrittenBackToTheLocalSpaceWhenTheIdRoutesLocallyAndTheAccountIsNotInTheGrid() throws InsufficientFundsException {
        when(accountDao.findById(LOCAL_ID)).thenReturn(anAccountWithId(LOCAL_ID));

        assertThat(underTest.findById(LOCAL_ID), is(equalTo(anAccountWithId(LOCAL_ID))));
        verify(localSpace).write(anAccountWithId(LOCAL_ID), Lease.FOREVER, 1000, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void anAccountIsNotWrittenBackToTheLocalSpaceWhenTheIdRoutesLocallyAndTheAccountIsNotInTheGridAndTheLoadTypeIsNullIfAbsent() {
        assertThat(underTest.findById(LOCAL_ID, AccountLoadType.NULL_IF_ABSENT), is(nullValue()));
        verifyZeroInteractions(accountDao);
    }

    @Test
    public void anAccountIsReadFromTheGlobalSpaceWhenTheIdDoesNotRouteLocallyAndTheAccountIsInTheGrid() throws InsufficientFundsException {
        when(globalSpace.readById(Account.class, REMOTE_ID, REMOTE_ID, 0, ReadModifiers.DIRTY_READ)).thenReturn(anAccountWithId(REMOTE_ID));

        assertThat(underTest.findById(REMOTE_ID), is(equalTo(anAccountWithId(REMOTE_ID))));
        verifyZeroInteractions(localSpace);
    }

    @Test
    public void anAccountIsReadFromTheDBWhenTheIdDoesNotRouteLocallyAndTheAccountIsNotInTheGrid() throws InsufficientFundsException {
        when(accountDao.findById(REMOTE_ID)).thenReturn(anAccountWithId(REMOTE_ID));

        assertThat(underTest.findById(REMOTE_ID), is(equalTo(anAccountWithId(REMOTE_ID))));
        verifyZeroInteractions(localSpace);
    }

    @Test
    public void anAccountIsWrittenBackToTheGlobalSpaceWhenTheIdDoesNotRouteLocallyAndTheAccountIsNotInTheGrid() throws InsufficientFundsException {
        when(accountDao.findById(REMOTE_ID)).thenReturn(anAccountWithId(REMOTE_ID));

        assertThat(underTest.findById(REMOTE_ID), is(equalTo(anAccountWithId(REMOTE_ID))));
        verify(globalSpace).write(anAccountWithId(REMOTE_ID), Lease.FOREVER, 1000, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void anAccountIsNotWrittenBackToTheGlobalSpaceWhenTheIdDoesNotRouteLocallyAndTheAccountIsNotInTheGridAndTheLoadTypeIsNullIfAbsent() {
        assertThat(underTest.findById(REMOTE_ID, AccountLoadType.NULL_IF_ABSENT), is(nullValue()));
        verifyZeroInteractions(accountDao);
    }

    @Test(expected = NullPointerException.class)
    public void removingANullAccountIdThrowsANullPointerException() {
        underTest.remove(null);
    }

    @Test
    public void removingAnAccountRoutedToTheLocalSpaceRemovesItFromTheLocalSpace() throws InsufficientFundsException {
        underTest.remove(anAccountWithId(LOCAL_ID));

        verify(localSpace).clear(anAccountWithId(LOCAL_ID));
        verifyZeroInteractions(accountDao, globalSpace);
    }

    @Test
    public void removingAnAccountNotRoutedToTheLocalSpaceRemovesItFromTheGlobalSpace() throws InsufficientFundsException {
        underTest.remove(anAccountWithId(REMOTE_ID));

        verify(globalSpace).clear(anAccountWithId(REMOTE_ID));
        verifyZeroInteractions(accountDao, localSpace);
    }

    @Test(expected = NullPointerException.class)
    public void lockingANullAccountIdThrowsANullPointerException() {
        underTest.lock(null);
    }

    @Test
    public void anAccountFromTheLocalSpaceCanBeLocked() throws InsufficientFundsException {
        when(localSpace.readById(Account.class, LOCAL_ID, LOCAL_ID, 1000, ReadModifiers.EXCLUSIVE_READ_LOCK)).thenReturn(anAccountWithId(LOCAL_ID));

        assertThat(underTest.lock(LOCAL_ID), is(equalTo(anAccountWithId(LOCAL_ID))));
        verifyZeroInteractions(globalSpace);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void lockingAnAccountThatDoesNotExistOrIsLockedInTheLocalSpaceCausesAConcurrentModificationException() {
        underTest.lock(LOCAL_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void lockingAnAccountThatIsNotRoutedLocallyCausesAnIllegalArgumentException() {
        underTest.lock(REMOTE_ID);
    }

    @Test(expected = NullPointerException.class)
    public void savingANullAccountThrowsANullPointerException() {
        underTest.save(null);
    }

    @Test
    public void savingAnAccountRoutedToTheLocalSpaceWritesItAndAPersistenceRequestToTheLocalSpace() throws InsufficientFundsException {
        underTest.save(anAccountWithId(LOCAL_ID));

        final Account savedAccount = anAccountWithId(LOCAL_ID);
        final AccountTransaction accountTx = savedAccount.popNewAccountTransactions().iterator().next();
        verify(localSpace).writeMultiple(new Object[]{savedAccount, new AccountPersistenceRequest(LOCAL_ID), new AccountTransactionPersistenceRequest(accountTx)},
                Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void savingAAccountThatIsNotRoutedToTheLocalSpaceWritesItAndAPersistenceRequestToTheGlobalSpace() throws InsufficientFundsException {
        underTest.save(anAccountWithId(REMOTE_ID));

        final Account savedAccount = anAccountWithId(REMOTE_ID);
        final AccountTransaction accountTx = savedAccount.popNewAccountTransactions().iterator().next();
        verify(globalSpace).writeMultiple(new Object[]{savedAccount, new AccountPersistenceRequest(REMOTE_ID), new AccountTransactionPersistenceRequest(accountTx)},
                Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    private Account anAccountWithId(final BigDecimal id) throws InsufficientFundsException {
        final Account account = new Account(id);
        account.setBalance(BigDecimal.valueOf(100));
        account.postTransaction(anAccountTransaction(id));
        return account;
    }

    private AccountTransaction anAccountTransaction(final BigDecimal id) {
        return new AccountTransaction(id, BigDecimal.TEN, "aType", "aReference");
    }

}
