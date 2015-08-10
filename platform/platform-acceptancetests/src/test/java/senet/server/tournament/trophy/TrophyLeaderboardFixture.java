package senet.server.tournament.trophy;

import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.model.tournament.TrophyLeaderboardPlayerUpdateRequest;
import com.yazino.platform.tournament.TournamentPlayerSummary;
import com.yazino.platform.tournament.TrophyLeaderboardPlayer;
import fit.Fixture;
import fitlibrary.SetUpFixture;
import fitlibrary.SubsetFixture;
import senet.server.WiredDoFixture;

import java.math.BigDecimal;
import java.util.*;

import static senet.server.tournament.trophy.TrophyLeaderboardIntegrationContext.GAME_TYPE;

public class TrophyLeaderboardFixture extends WiredDoFixture {

    private static final BigDecimal TOURNAMENT_ID = BigDecimal.ONE;

    final List<String> players = new LinkedList<String>();

    final TrophyLeaderboardIntegrationContext context = new TrophyLeaderboardIntegrationContext();
    final TournamentSummary tournamentSummary = new TournamentSummary();
    private int numberOfPlayers;
    private static final Comparator<TrophyLeaderboardPlayer> LEADERBOARD_COMPARATOR = new Comparator<TrophyLeaderboardPlayer>() {

        @Override
        public int compare(TrophyLeaderboardPlayer o1, TrophyLeaderboardPlayer o2) {
            return new Long(o2.getPoints()).compareTo(o1.getPoints());
        }
    };


    public TrophyLeaderboardFixture() {
        tournamentSummary.setTournamentId(TOURNAMENT_ID);
        tournamentSummary.setPlayers(new LinkedList<TournamentPlayerSummary>());
        tournamentSummary.setGameType(GAME_TYPE);
    }

    public void playersGetExtraPointsPerPlayerInTournament(long points) {
        context.setBonusPointsForAmountOfPlayer(points);
    }

    public Fixture pointsAreGivenFollowing() {
        return new PointsSetUp();
    }

    public Fixture gameLeaderboardPayoutStructure() {
        return new PayoutSetUp();
    }

    public Fixture resultAfterATournamentWithPlayers(int numberOfPlayers) {
        this.numberOfPlayers = numberOfPlayers;
        tournamentSummary.getPlayers().clear();
        return new TournamentResultSetUp();
    }

    private void setPlayerRank(int rank, String player) {
        final List<TournamentPlayerSummary> playerResults = tournamentSummary.getPlayers();
        final TournamentPlayerSummary result = new TournamentPlayerSummary(
                getPlayerId(player), rank, player, BigDecimal.ZERO, "picture" + getPlayerId(player));
        playerResults.add(result);
    }

    public Fixture gameLeaderboardIs() {
        final ArrayList<LeaderboardPositionInfo> result = new ArrayList<LeaderboardPositionInfo>();
        for (int i = tournamentSummary.getPlayers().size(); i < numberOfPlayers; i++) {
            setPlayerRank(i, "player " + i);
        }
        for (TournamentPlayerSummary player : tournamentSummary.getPlayers()) {
            context.updateTrophyLeaderboard(new TrophyLeaderboardPlayerUpdateRequest(BigDecimal.ONE, tournamentSummary.getTournamentId(),
                    player.getId(), player.getName(), player.getPictureUrl(), player.getLeaderboardPosition(), tournamentSummary.getPlayers().size()));
        }
        final List<TrophyLeaderboardPlayer> players = new ArrayList<TrophyLeaderboardPlayer>(context.getCurrentPlayerRank());
        Collections.sort(players, LEADERBOARD_COMPARATOR);
        for (TrophyLeaderboardPlayer player : players) {
            result.add(new LeaderboardPositionInfo(player.getLeaderboardPosition(), player.getPlayerName(), player.getPoints()));
        }
        return new SubsetFixture(result);
    }

    public Fixture finalGameLeaderboardIs() {
        return new FinalLeaderboardSetUp();
    }

    public Fixture chipBalanceForPlayers() {
        context.resultTrophyLeaderboard();
        List<PlayerBalanceInfo> result = new LinkedList<PlayerBalanceInfo>();
        for (String player : players) {
            final BigDecimal balance = context.getBalanceFor(getPlayerId(player));
            result.add(new PlayerBalanceInfo(player, balance.longValue()));
        }
        return new SubsetFixture(result);

    }

    public String gameLeaderboardIsReset() {
        return context.isTrophyLeaderboardReset() ? "yes" : "no";
    }


    private BigDecimal getPlayerId(String player) {
        if (!players.contains(player)) {
            players.add(player);
            context.createPlayer(players.indexOf(player), player);
        }
        return BigDecimal.valueOf(players.indexOf(player));
    }

    public class LeaderboardPositionInfo {
        public int rank;
        public String player;
        public long points;

        public LeaderboardPositionInfo(int rank, String player, long points) {
            this.rank = rank;
            this.player = player;
            this.points = points;
        }
    }

    public class PlayerBalanceInfo {
        public String player;
        public long balance;

        private PlayerBalanceInfo(String player, long balance) {
            this.player = player;
            this.balance = balance;
        }
    }

    public class PointsSetUp extends SetUpFixture {
        public void rankPoints(int rank, long points) {
            context.setPointsPerRank(rank, points);
        }
    }

    public class PayoutSetUp extends SetUpFixture {
        public void rankChips(int rank, long chips) {
            context.setPayoutPerRank(rank, chips);
        }
    }

    public class FinalLeaderboardSetUp extends SetUpFixture {
        public void rankPlayerPoints(int rank, String player, long points) {
            context.setTrophyLeaderboardPlayer(rank, getPlayerId(player), player, points);
        }
    }

    public class TournamentResultSetUp extends SetUpFixture {
        public void rankPlayer(int rank, String player) {
            setPlayerRank(rank, player);
        }
    }
}
