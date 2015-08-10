package com.yazino.platform.processor.tournament;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.audit.AuditLabelFactory;
import com.yazino.platform.tournament.TrophyLeaderboardPlayer;
import com.yazino.platform.tournament.TrophyLeaderboardPlayers;
import com.yazino.platform.tournament.TrophyLeaderboardPosition;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TrophyLeaderboardPayoutCalculatorTest {

    private static final BigDecimal LEADERBOARD_ID = BigDecimal.valueOf(17);
    private static final DateTime PERIOD_END = new DateTime();
    private static final BigDecimal LEADERBOARD_ACCOUNT_ID = BigDecimal.valueOf(999);
    private static final String AUDIT_LABEL = "anAuditLabel";
    private static final SimpleDateFormat ACCOUNT_DATE_FORMAT = new SimpleDateFormat("ddMMyyyy'T'HHmmss");


    private final TrophyLeaderboardPayoutCalculator calculator = new TrophyLeaderboardPayoutCalculator();
    private final InternalWalletService internalWalletService = mock(InternalWalletService.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final AuditLabelFactory auditor = mock(AuditLabelFactory.class);
    static final String TRANSACTION_TYPE = "Trophy Leaderboard Award";

    @Before
    public void setUp() throws WalletServiceException {
        final String endTimeStamp = ACCOUNT_DATE_FORMAT.format(PERIOD_END.toDate());
        final String accountName = "TROPHYLEADERBOARD:" + LEADERBOARD_ID + ":" + endTimeStamp;
        when(internalWalletService.createAccount(eq(accountName))).thenReturn(LEADERBOARD_ACCOUNT_ID);

        when(playerRepository.findById(BigDecimal.valueOf(10))).thenReturn(new Player(BigDecimal.valueOf(10), "aName", BigDecimal.valueOf(100), null, null, null, null));
        when(playerRepository.findById(BigDecimal.valueOf(20))).thenReturn(new Player(BigDecimal.valueOf(10), "aName", BigDecimal.valueOf(200), null, null, null, null));
        when(playerRepository.findById(BigDecimal.valueOf(30))).thenReturn(new Player(BigDecimal.valueOf(10), "aName", BigDecimal.valueOf(300), null, null, null, null));
        when(playerRepository.findById(BigDecimal.valueOf(40))).thenReturn(new Player(BigDecimal.valueOf(10), "aName", BigDecimal.valueOf(400), null, null, null, null));

        when(auditor.newLabel()).thenReturn(AUDIT_LABEL);
    }

    @Test
    public void shouldBeSerializable() throws Exception {
        testSerializationRoundTrip(calculator, false);
    }

    @Test
    public void shouldPayoutAccountTransactionsForTop3Players() throws Exception {
        TrophyLeaderboardPlayers players = new TrophyLeaderboardPlayers();
        players.addPlayer(createPlayer(1, 10, 100));
        players.addPlayer(createPlayer(1, 20, 50));
        players.addPlayer(createPlayer(1, 30, 50));
        players.addPlayer(createPlayer(1, 40, 20));
        players.addPlayer(createPlayer(1, 50, 0));
        players.updatePlayersPositions();

        HashMap<Integer, TrophyLeaderboardPosition> positionData = new HashMap<Integer, TrophyLeaderboardPosition>();
        positionData.put(1, new TrophyLeaderboardPosition(1, 100, 1000));
        positionData.put(2, new TrophyLeaderboardPosition(2, 50, 500));
        positionData.put(3, new TrophyLeaderboardPosition(3, 25, 250));

        calculator.payout(LEADERBOARD_ID, players, positionData, internalWalletService, playerRepository, auditor);
        verifyPayoutToAccountAtPosition(BigDecimal.valueOf(100), 1, 1000);
        verifyPayoutToAccountAtPosition(BigDecimal.valueOf(200), 2, 375);
        verifyPayoutToAccountAtPosition(BigDecimal.valueOf(300), 2, 375);

        checkPayout(BigDecimal.valueOf(1000), players.getPlayersOnPosition(1), 1, 1);
        checkPayout(BigDecimal.valueOf(375), players.getPlayersOnPosition(2), 2, 2);
        checkPayout(null, players.getPlayersOnPosition(3), 0, 3);
        checkPayout(null, players.getPlayersOnPosition(4), 1, 4);

    }

    private void checkPayout(BigDecimal payout, Set<TrophyLeaderboardPlayer> winners, int expectedNumberOnPosition, int expectedPosition) {
        assertEquals(expectedNumberOnPosition, winners.size());
        for (TrophyLeaderboardPlayer winner : winners) {
            assertEquals(expectedPosition, winner.getLeaderboardPosition());
            assertEquals(payout, winner.getFinalPayout());
        }
    }

    private void verifyPayoutToAccountAtPosition(
            final BigDecimal destinationAccountId,
            final int position,
            final long amount) throws WalletServiceException {
        final String txDescription = "Payout for position " + position + " from leaderboard " + LEADERBOARD_ID;
        verify(internalWalletService).postTransaction(destinationAccountId,
                BigDecimal.valueOf(amount), TRANSACTION_TYPE, txDescription, TransactionContext.EMPTY);
    }


    private TrophyLeaderboardPlayer createPlayer(int leaderboardPosition, int playerId, long points) {
        return new TrophyLeaderboardPlayer(leaderboardPosition, BigDecimal.valueOf(playerId), "p" + playerId, points, "aPictureUrl");
    }

    private <T> void testSerializationRoundTrip(T object, boolean checkIfEqual) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(os);
        out.writeObject(object);
        byte[] bytes = os.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream oin = new ObjectInputStream(in);
        @SuppressWarnings({"unchecked"}) T after = (T) oin.readObject();
        if (checkIfEqual) {
            Assert.assertEquals(object, after);
        }

    }

}
