package com.yazino.platform.gamehost.wallet;

import com.yazino.game.api.TransactionType;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.table.PostTransactionAtTable;
import com.yazino.platform.model.table.TableTransactionRequest;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.table.PlayerInformation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class SpaceGameHostWalletTest {
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(100);
    private static final BigDecimal ACCOUNT_BALANCE = BigDecimal.valueOf(553);
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(885);
    private static final long GAME_ID = 44L;
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(77);
    private static final String REPLY_LABEL = "aReplyLabel";
    private static final String REFERENCE = "aReference";
    private static final String AUDIT_LABEL = "anAuditLabel";
    private static final TransactionType TX_TYPE = TransactionType.Return;
    private static final BigDecimal SESSION_ID = BigDecimal.TEN;
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(898);

    @Mock
    private InternalWalletService internalWalletService;
    @Mock
    private GigaSpace tableGigaSpace;

    private SpaceGameHostWallet underTest;

    @Before
    public void setUp() throws WalletServiceException {
        MockitoAnnotations.initMocks(this);

        when(internalWalletService.getBalance(ACCOUNT_ID)).thenReturn(ACCOUNT_BALANCE);

        underTest = new SpaceGameHostWallet(tableGigaSpace, internalWalletService);
    }

    @Test
    public void transactionsArePostedToTheSpaceImmediately() throws WalletServiceException {
        PlayerInformation playerInformation = playerInformation(ACCOUNT_ID);
        underTest.post(TABLE_ID, GAME_ID, playerInformation, AMOUNT,
                TX_TYPE, AUDIT_LABEL, REFERENCE, REPLY_LABEL);

        final PostTransactionAtTable expectedPost = new PostTransactionAtTable(playerInformation.getPlayerId(), ACCOUNT_ID, AMOUNT,
                TX_TYPE.toString(), REFERENCE, REPLY_LABEL, aContext());
        verify(tableGigaSpace).write(new TableTransactionRequest(TABLE_ID, Arrays.asList(expectedPost),
                Collections.<BigDecimal>emptySet(), AUDIT_LABEL));
    }

    @Test
    public void balanceRequestsAreDelegatedToTheWalletService() throws WalletServiceException {
        final BigDecimal balance = underTest.getBalance(ACCOUNT_ID);

        assertThat(balance, is(equalTo(ACCOUNT_BALANCE)));
    }

    @Test(expected = WalletServiceException.class)
    public void balanceRequestFailuresArePropagatedFromTheWalletService() throws WalletServiceException {
        reset(internalWalletService);
        when(internalWalletService.getBalance(ACCOUNT_ID)).thenThrow(new WalletServiceException("aTestException"));

        underTest.getBalance(ACCOUNT_ID);
    }

    private PlayerInformation playerInformation(final BigDecimal accountId) {
        return new PlayerInformation(PLAYER_ID, "aPlayer", accountId, SESSION_ID, ACCOUNT_BALANCE);
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
