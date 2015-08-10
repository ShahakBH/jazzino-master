package com.yazino.platform.gamehost.wallet;

import com.yazino.platform.account.GameHostWallet;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.PlayerInformation;
import com.yazino.platform.util.UUIDSource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.GameException;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.TransactionType;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TableBoundGamePlayerWalletFactoryTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);
    private static final BigDecimal PLAYER_BALANCE = BigDecimal.valueOf(100);
    private static final BigDecimal PLAYER_ACCOUNT_ID = BigDecimal.valueOf(200);
    private static final String PLAYER_NAME = "aName";
    private static final String NEW_UUID = "aUuid";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(1000);
    private static final String REFERENCE = "aReference";

    private static final String AUDIT_LABEL = "anAuditLabel";
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(10);
    private static final Long GAME_ID = 11L;
    private static final GamePlayer GAME_PLAYER = new GamePlayer(PLAYER_ID, null, PLAYER_NAME);

    private final GameHostWallet gameHostWallet = mock(GameHostWallet.class);
    private final UUIDSource uuidSource = mock(UUIDSource.class);

    private PlayerInformation playerInfo;

    private TableBoundGamePlayerWalletFactory underTest;
    private Table table;

    @Before
    public void setUp() throws WalletServiceException {
        MockitoAnnotations.initMocks(this);

        playerInfo = new PlayerInformation(PLAYER_ID, PLAYER_NAME, PLAYER_ACCOUNT_ID, BigDecimal.TEN, PLAYER_BALANCE);

        table = new Table();
        table.setTableId(TABLE_ID);
        table.setGameId(GAME_ID);
        table.addPlayerToTable(playerInfo);

        when(uuidSource.getNewUUID()).thenReturn(NEW_UUID).thenReturn("anotherUuid");

        underTest = new TableBoundGamePlayerWalletFactory(table, gameHostWallet, AUDIT_LABEL, uuidSource);
    }

    @Test(expected = IllegalStateException.class)
    public void anIllegalStateExceptionShouldBeThrownWhenThePlayerIsNotAtTheTable() throws GameException {
        table.removeAllPlayers();

        underTest.forPlayer(GAME_PLAYER).increaseBalanceBy(AMOUNT, REFERENCE, null);
    }

    @Test
    public void anAsyncTransferIsTriggeredWhenWeTransferToThePlayersAccount()
            throws GameException, WalletServiceException {
        underTest.forPlayer(GAME_PLAYER).increaseBalanceBy(AMOUNT, AUDIT_LABEL, REFERENCE);

        verify(gameHostWallet).post(TABLE_ID, GAME_ID, playerInfo, AMOUNT,
                TransactionType.Return, AUDIT_LABEL, REFERENCE, NEW_UUID);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test(expected = GameException.class)
    public void failedTransferFromThePlayerShouldThrowAGameException() throws WalletServiceException, GameException {
        doThrow(new WalletServiceException("aTestException")).when(gameHostWallet).post(TABLE_ID, GAME_ID,
                playerInfo, BigDecimal.ZERO.subtract(AMOUNT), TransactionType.Stake, AUDIT_LABEL, REFERENCE, NEW_UUID);

        underTest.forPlayer(GAME_PLAYER).decreaseBalanceBy(AMOUNT, AUDIT_LABEL, REFERENCE);
    }

    @Test
    public void anAsyncTransferIsTriggeredWhenWeTransferFromThePlayersAccount()
            throws GameException, WalletServiceException {
        underTest.forPlayer(GAME_PLAYER).decreaseBalanceBy(AMOUNT, AUDIT_LABEL, REFERENCE);

        verify(gameHostWallet).post(TABLE_ID, GAME_ID, playerInfo, BigDecimal.ZERO.subtract(AMOUNT),
                TransactionType.Stake, AUDIT_LABEL, REFERENCE, NEW_UUID);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test(expected = GameException.class)
    public void failedTransferToThePlayerShouldThrowAGameException() throws GameException, WalletServiceException {
        doThrow(new WalletServiceException("aTestException")).when(gameHostWallet).post(TABLE_ID, GAME_ID,
                playerInfo, BigDecimal.ZERO.subtract(AMOUNT), TransactionType.Stake, AUDIT_LABEL, REFERENCE, NEW_UUID);

        underTest.forPlayer(GAME_PLAYER).decreaseBalanceBy(AMOUNT, AUDIT_LABEL, REFERENCE);
    }

    @Test
    @Ignore("To be re-enabled once tx results update the balance")
    public void balanceQueriesShouldUseTheCachedBalance() throws GameException {
        // TODO TX-RETURN: this should be enabled once update on tx-return is implemented
        assertThat(underTest.forPlayer(GAME_PLAYER).getBalance(), is(equalTo(PLAYER_BALANCE)));

        verifyZeroInteractions(gameHostWallet);
    }

}
