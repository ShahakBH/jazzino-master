package senet.server.tournament.trophy;

import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardPlayerUpdateRequest;
import com.yazino.platform.processor.tournament.TrophyLeaderboardResultContext;
import com.yazino.platform.repository.community.TrophyRepository;
import com.yazino.platform.repository.session.InboxMessageRepository;
import com.yazino.platform.repository.tournament.TrophyLeaderboardResultRepository;
import com.yazino.platform.service.audit.AuditLabelFactory;
import com.yazino.platform.service.tournament.AwardTrophyService;
import com.yazino.platform.test.InMemoryInternalWalletService;
import com.yazino.platform.test.InMemoryPlayerDetailsService;
import com.yazino.platform.test.InMemoryPlayerRepository;
import com.yazino.platform.test.InMemoryWalletService;
import com.yazino.platform.tournament.TrophyLeaderboardPlayer;
import com.yazino.platform.tournament.TrophyLeaderboardPlayers;
import com.yazino.platform.tournament.TrophyLeaderboardPosition;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class TrophyLeaderboardIntegrationContext {
    public static final String GAME_TYPE = "getGameType";
    private static final Period LEADERBOARD_PERIOD = new Period(Days.days(3));
    private static final DateTime START_TIME = new DateTime(2009, 1, 23, 12, 6, 0, 0);

    private static final DateTime END_TIME = START_TIME.plus(LEADERBOARD_PERIOD).plus(LEADERBOARD_PERIOD);

    private final TrophyLeaderboard trophyLeaderboard = new TrophyLeaderboard(BigDecimal.ONE, "test leaderboard",
            GAME_TYPE, new Interval(START_TIME, END_TIME), LEADERBOARD_PERIOD.toStandardDuration());
    private InMemoryWalletService walletService = new InMemoryWalletService();
    private InMemoryInternalWalletService internalWalletService = new InMemoryInternalWalletService(walletService);
    private final AwardTrophyService awardTrophyService = mock(AwardTrophyService.class);
    private final TrophyRepository trophyRepository = mock(TrophyRepository.class);
    private final InboxMessageRepository inboxMessageRepository = mock(InboxMessageRepository.class);

    private InMemoryPlayerRepository playerRepository = new InMemoryPlayerRepository();
    private InMemoryPlayerDetailsService playerDetailsService = new InMemoryPlayerDetailsService();

    public TrophyLeaderboardIntegrationContext() {
        trophyLeaderboard.setPositionData(new HashMap<Integer, TrophyLeaderboardPosition>());
        trophyLeaderboard.setPlayers(new TrophyLeaderboardPlayers());

        playerDetailsService.setPlayerRepository(playerRepository);
    }

    public void createPlayer(int playerId, String name) {
        final BigDecimal id = BigDecimal.valueOf(playerId);
        walletService.createAccountIfRequired(id);
        playerDetailsService.save(new BasicProfileInformation(id, name, "pictureUrl", id));
    }

    public void setBonusPointsForAmountOfPlayer(long points) {
        trophyLeaderboard.setPointBonusPerPlayer(points);
    }

    public void setPointsPerRank(int rank, long points) {
        Map<Integer, TrophyLeaderboardPosition> positionMap = trophyLeaderboard.getPositionData();
        long payout = positionMap.get(rank) != null ? positionMap.get(rank).getAwardPayout() : 0;
        positionMap.put(rank, new TrophyLeaderboardPosition(rank, points, payout));
    }

    public void setPayoutPerRank(int rank, long payout) {
        Map<Integer, TrophyLeaderboardPosition> positionMap = trophyLeaderboard.getPositionData();
        long points = positionMap.get(rank) != null ? positionMap.get(rank).getAwardPoints() : 0;
        positionMap.put(rank, new TrophyLeaderboardPosition(rank, points, payout));
    }

    public void updateTrophyLeaderboard(TrophyLeaderboardPlayerUpdateRequest playerUpdateRequest) {
        trophyLeaderboard.update(playerUpdateRequest);
    }

    public List<TrophyLeaderboardPlayer> getCurrentPlayerRank() {
        return trophyLeaderboard.getOrderedByPosition();
    }

    public void setTrophyLeaderboardPlayer(int rank, BigDecimal playerId, String playerName, long points) {
        final TrophyLeaderboardPlayer leaderboardPlayer = new TrophyLeaderboardPlayer(rank, playerId, playerName, points, "picture" + playerId);
        TrophyLeaderboardPlayers players = (TrophyLeaderboardPlayers) ReflectionTestUtils.getField(trophyLeaderboard, "players");
        players.addPlayer(leaderboardPlayer);
        players.updatePlayersPositions();
    }

    public BigDecimal getBalanceFor(BigDecimal playerId) {
        return walletService.getBalance(playerDetailsService.getAccountId(playerId));
    }

    public void resultTrophyLeaderboard() {
        AuditLabelFactory auditor = mock(AuditLabelFactory.class);
        final TrophyLeaderboardResultRepository trophyLeaderboardResultRepository = mock(TrophyLeaderboardResultRepository.class);
        try {
            TrophyLeaderboardResultContext trophyLeaderboardResultContext = new TrophyLeaderboardResultContext(
                    trophyLeaderboardResultRepository, internalWalletService, playerRepository, awardTrophyService,
                    inboxMessageRepository, trophyRepository, auditor, new SettableTimeSource(START_TIME.plus(LEADERBOARD_PERIOD).getMillis()));
            trophyLeaderboard.result(trophyLeaderboardResultContext);
        } catch (WalletServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isTrophyLeaderboardReset() {
        return trophyLeaderboard.getOrderedByPosition().size() == 0;
    }
}
