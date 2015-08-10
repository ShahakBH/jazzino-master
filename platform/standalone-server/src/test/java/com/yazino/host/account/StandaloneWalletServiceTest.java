package com.yazino.host.account;

import com.yazino.host.community.StandalonePlayerSource;
import com.yazino.model.StandalonePlayer;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StandaloneWalletServiceTest {

    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(123);
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(12);

    private StandalonePlayerSource playerSource;

    private StandaloneWalletService underTest;
    private StandalonePlayer aPlayer;

    @Before
    public void setUp() {
        playerSource = mock(StandalonePlayerSource.class);
        aPlayer = new StandalonePlayer(PLAYER_ID, "aPlayer", INITIAL_BALANCE);
        when(playerSource.findById(PLAYER_ID)).thenReturn(aPlayer);
        underTest = new StandaloneWalletService(playerSource);
    }

    @Test
    public void shouldGetBalanceFromAccount() throws WalletServiceException {
        assertEquals(INITIAL_BALANCE, underTest.getBalance(PLAYER_ID));
    }

    @Test
    public void shouldPostTransactionAndUpdatePlayer() throws WalletServiceException {
        final BigDecimal expected = INITIAL_BALANCE.add(BigDecimal.ONE);
        final BigDecimal actual = underTest.postTransaction(PLAYER_ID, BigDecimal.ONE, "stake", "ref",
                transactionContext().withGameId(123l).withTableId(BigDecimal.TEN).withSessionId(BigDecimal.TEN).build());
        assertEquals(expected, actual);
        assertEquals(expected, aPlayer.getChips());
    }

    @Test
    public void shouldPostTransactionWithoutAuditing() throws WalletServiceException {
        final BigDecimal expected = INITIAL_BALANCE.add(BigDecimal.ONE);
        final BigDecimal actual = underTest.postTransaction(PLAYER_ID, BigDecimal.ONE, "stake", "ref", TransactionContext.EMPTY);
        assertEquals(expected, actual);
    }

    @Test(expected = WalletServiceException.class)
    public void shouldThrowWalletExceptionIfTransactionCannotBeProcessed() throws WalletServiceException {
        final BigDecimal txAmount = BigDecimal.ZERO.subtract(INITIAL_BALANCE).subtract(BigDecimal.ONE);
        underTest.postTransaction(PLAYER_ID, txAmount, "stake", "ref", TransactionContext.EMPTY);
    }
}
