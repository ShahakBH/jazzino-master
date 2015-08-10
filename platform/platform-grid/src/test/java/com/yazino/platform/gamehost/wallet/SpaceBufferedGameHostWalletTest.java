package com.yazino.platform.gamehost.wallet;

import com.yazino.game.api.TransactionType;
import com.yazino.platform.account.GameHostWallet;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.table.PostTransactionAtTable;
import com.yazino.platform.model.table.TableTransactionRequest;
import com.yazino.platform.table.PlayerInformation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class SpaceBufferedGameHostWalletTest {
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(100);
    private static final BigDecimal ACCOUNT_BALANCE = BigDecimal.valueOf(20034);
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(24345);
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(555);
    private static final long GAME_ID = 100L;
    private static final String COMMAND_REFERENCE = "aCommandReference";
    private static final BigDecimal SESSION_ID = BigDecimal.TEN;
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);

    @Mock
    private GameHostWallet gameHostWallet;
    @Mock
    private GigaSpace tableGigaSpace;

    private SpaceBufferedGameHostWallet underTest;

    @Before
    public void setUp() throws WalletServiceException {
        MockitoAnnotations.initMocks(this);

        underTest = new SpaceBufferedGameHostWallet(tableGigaSpace, gameHostWallet, TABLE_ID, COMMAND_REFERENCE);

        when(gameHostWallet.getBalance(ACCOUNT_ID)).thenReturn(ACCOUNT_BALANCE);
    }

    @Test
    public void transactionRequestsAreNotSentImmediately() throws WalletServiceException {
        postTo(underTest, tx(AMOUNT));

        verifyZeroInteractions(gameHostWallet);
    }

    @Test
    public void transactionRequestsAreSentOnFlush() throws WalletServiceException {
        postTo(underTest, tx(AMOUNT));
        postTo(underTest, tx(BigDecimal.ZERO.subtract(AMOUNT)));

        underTest.flush();

        verify(tableGigaSpace).write(request(tx(AMOUNT), tx(BigDecimal.ZERO.subtract(AMOUNT))));
    }

    @Test
    public void transactionRequestsAreClearedAfterFlush() throws WalletServiceException {
        postTo(underTest, tx(AMOUNT));
        postTo(underTest, tx(BigDecimal.ZERO.subtract(AMOUNT)));

        underTest.flush();

        underTest.flush();

        verify(tableGigaSpace, times(1)).write(request(tx(AMOUNT), tx(BigDecimal.ZERO.subtract(AMOUNT))));
    }

    @Test
    public void balanceRequestsAreDelegatedToTheDelegate() throws WalletServiceException {
        final BigDecimal balance = underTest.getBalance(ACCOUNT_ID);

        assertThat(balance, is(equalTo(ACCOUNT_BALANCE)));
    }

    @Test(expected = WalletServiceException.class)
    public void balanceRequestFailuresArePropagatedFromTheDelegate() throws WalletServiceException {
        reset(gameHostWallet);
        when(gameHostWallet.getBalance(ACCOUNT_ID)).thenThrow(new WalletServiceException("aTestException"));

        underTest.getBalance(ACCOUNT_ID);
    }

    private TableTransactionRequest request(final Object... objs) {
        final List<PostTransactionAtTable> txs = new ArrayList<>();
        final Set<BigDecimal> releaseRequests = new HashSet<>();

        for (Object obj : objs) {
            if (obj instanceof PostTransactionAtTable) {
                txs.add((PostTransactionAtTable) obj);
            } else if (obj instanceof BigDecimal) {
                releaseRequests.add((BigDecimal) obj);
            } else {
                throw new IllegalArgumentException("Invalid type: " + obj.getClass().getName());
            }
        }

        return new TableTransactionRequest(TABLE_ID, txs, releaseRequests, COMMAND_REFERENCE);
    }

    private PostTransactionAtTable tx(final BigDecimal amount) {
        return new PostTransactionAtTable(ACCOUNT_ID, ACCOUNT_ID, amount, TransactionType.Return.toString(), "aReference", "aUniqueId", aContext());
    }

    private void postTo(final GameHostWallet gameHostWallet, final PostTransactionAtTable tx) throws WalletServiceException {
        final PlayerInformation playerInformation = new PlayerInformation(PLAYER_ID,
                "aName",
                tx.getAccountId(),
                SESSION_ID, BigDecimal.ZERO);
        gameHostWallet.post(tx.getTransactionContext().getTableId(),
                tx.getTransactionContext().getGameId(),
                playerInformation,
                tx.getAmount(),
                TransactionType.valueOf(tx.getTransactionType()),
                COMMAND_REFERENCE,
                tx.getReference(),
                tx.getUniqueId());
    }

    private TransactionContext aContext() {
        return transactionContext()
                .withGameId(GAME_ID)
                .withTableId(TABLE_ID)
                .withSessionId(SESSION_ID)
                .withPlayerId(PLAYER_ID)
                .build();
    }

}
