package com.yazino.platform.model.conversion;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.tournament.*;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;
import java.util.*;

@Service
public class TournamentViewFactory {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("MMMM d yyyy HH:mm z");
    private final TimeSource timeSource;

    @Autowired(required = true)
    public TournamentViewFactory(@Qualifier("timeSource") final TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    public TournamentView create(final Tournament tournament) {
        final TournamentViewDetails.Status status = TournamentViewDetails.Status.valueOf(
                tournament.getTournamentStatus().name());
        final TournamentViewDetails.Builder builder = new TournamentViewDetails.Builder();
        builder.tournamentId(tournament.getTournamentId())
                .name(tournament.getName())
                .description(tournament.getDescription())
                .gameType(tournament.getTournamentVariationTemplate().getGameType())
                .variationTemplateName(tournament.getTournamentVariationTemplate().getTemplateName())
                .startTime(tournament.getStartTimeStamp().toString(DATE_FORMATTER))
                .status(status)
                .playersRegistered(tournament.playerCount())
                .entryFee(tournament.getTournamentVariationTemplate().getEntryFee())
                .prizePool(tournament.calculateUnpaidPrizePool());

        switch (tournament.getTournamentStatus()) {
            case ANNOUNCED:
            case REGISTERING:
                long millisTillStart = tournament.getStartTimeStamp().getMillis() - timeSource.getCurrentTimeStamp();
                final int maxPlayers = tournament.getTournamentVariationTemplate().getMaxPlayers();
                if (millisTillStart < 0) {
                    millisTillStart = 0;
                }
                builder.maxPlayers(maxPlayers)
                        .millisTillStart(millisTillStart);
                break;
            case RUNNING:
            case ON_BREAK:
            case WAITING_FOR_CLIENTS:
                final int playersRemaining = tournament.activePlayerCount();
                final Integer roundIndex = tournament.getCurrentRoundIndex();
                if (roundIndex == null
                        || roundIndex >= tournament.getTournamentVariationTemplate().getTournamentRounds().size()) {
                    throw new IllegalStateException(String.format(
                            "Malformed tournament - current round=%d; available rounds=%d; tournament=%s",
                            roundIndex, tournament.getTournamentVariationTemplate().getTournamentRounds().size(),
                            tournament));
                }
                final TournamentVariationRound currentRound
                        = tournament.getTournamentVariationTemplate().getTournamentRounds().get(roundIndex);
                int nextBreakLengthInMinutes = tournament.getNextBreakLength();
                long nextBreak = 0;
                if (nextBreakLengthInMinutes > 0) {
                    nextBreak = tournament.retrieveNextBreakTime();
                }
                long levelEnds = tournament.retrieveEndTimeOfCurrentRound();
                if (tournament.getTournamentStatus() == TournamentStatus.ON_BREAK) {
                    levelEnds = tournament.getNextRoundStartTime();
                    nextBreak = 0;
                    nextBreakLengthInMinutes = 0;
                } else if (tournament.getTournamentStatus() == TournamentStatus.WAITING_FOR_CLIENTS) {
                    levelEnds = 0;
                    nextBreak = 0;
                    nextBreakLengthInMinutes = 0;
                }
                builder.playersRemaining(playersRemaining)
                        .level(currentRound.getRoundNumber())
                        .nextBreakInMillis(nextBreak - timeSource.getCurrentTimeStamp())
                        .nextBreakLengthInMinutes(nextBreakLengthInMinutes)
                        .levelsEndIn(levelEnds - timeSource.getCurrentTimeStamp())
                        .build();
                break;
            case SETTLED:
            default:
                final BigDecimal prizePot1 = tournament.getSettledPrizePot();
                if (prizePot1 == null) {
                    builder.prizePool(BigDecimal.ZERO);
                } else {
                    builder.prizePool(prizePot1);
                }
        }
        final TournamentViewDetails overview = builder.build();
        final List<TournamentRoundView> rounds = buildRoundViews(tournament);
        final Map<BigDecimal, TournamentRankView> players = retrievePlayersWithoutLeaderboardPosition(tournament);
        final List<TournamentRankView> ranks = buildRanks(tournament, players);
        return new TournamentView(overview, players, ranks, rounds, timeSource.getCurrentTimeStamp());
    }

    private Map<BigDecimal, TournamentRankView> retrievePlayersWithoutLeaderboardPosition(final Tournament tournament) {
        final Map<BigDecimal, TournamentRankView> result = new HashMap<BigDecimal, TournamentRankView>();
        final Set<TournamentPlayer> players = tournament.tournamentPlayers();
        for (TournamentPlayer player : players) {
            if (player.getLeaderboardPosition() == null) {
                final TournamentRankView.Status status;
                if (player.getStatus() != null) {
                    status = TournamentRankView.Status.valueOf(player.getStatus().name());
                } else {
                    status = null;
                }
                result.put(player.getPlayerId(), new TournamentRankView.Builder()
                        .playerName(player.getName())
                        .playerId(player.getPlayerId())
                        .tableId(player.getTableId())
                        .status(status)
                        .build());
            }
        }
        return result;
    }

    private List<TournamentRankView> buildRanks(final Tournament tournament,
                                                final Map<BigDecimal, TournamentRankView> players) {
        final List<TournamentRankView> result = new ArrayList<TournamentRankView>();
        final List<TournamentPlayer> activeLeaderboard = tournament.getPlayers().getByMaxLeaderboard(Integer.MAX_VALUE);
        final Map<TournamentPlayer, BigDecimal> playerPrizes = tournament.getPayoutCalculator().calculatePayouts(
                new HashSet<TournamentPlayer>(activeLeaderboard), tournament.getTournamentVariationTemplate());
        for (TournamentPlayer player : activeLeaderboard) {
            BigDecimal playerPrize = playerPrizes.get(player);
            if (player.getSettledPrize() != null) {
                playerPrize = player.getSettledPrize();
            }
            final TournamentRankView.Status status;
            if (player.getStatus() != null) {
                status = TournamentRankView.Status.valueOf(player.getStatus().name());
            } else {
                status = null;
            }
            final TournamentRankView.EliminationReason eliminationReason;
            if (player.getEliminationReason() != null) {
                eliminationReason = TournamentRankView.EliminationReason.valueOf(player.getEliminationReason().name());
            } else {
                eliminationReason = null;
            }
            final TournamentRankView rankView = new TournamentRankView.Builder()
                    .playerId(player.getPlayerId())
                    .playerName(player.getName())
                    .balance(player.getStack())
                    .tableId(player.getTableId())
                    .rank(player.getLeaderboardPosition())
                    .prize(playerPrize)
                    .status(status)
                    .eliminationReason(eliminationReason)
                    .build();
            result.add(rankView);
            players.put(player.getPlayerId(), rankView);
        }
        final List<BigDecimal> prizes = tournament.getPayoutCalculator().calculatePrizes(
                tournament.playerCount(), tournament.getTournamentVariationTemplate());
        for (int i = activeLeaderboard.size(); i < prizes.size(); i++) {
            final TournamentRankView rankView = new TournamentRankView.Builder()
                    .rank(i + 1)
                    .prize(prizes.get(i))
                    .build();
            result.add(rankView);
        }
        return result;
    }

    private List<TournamentRoundView> buildRoundViews(final Tournament tournament) {
        final List<TournamentRoundView> result = new ArrayList<TournamentRoundView>();
        final TournamentVariationTemplate template = tournament.getTournamentVariationTemplate();
        final List<TournamentVariationRound> rounds = template.getTournamentRounds();
        for (TournamentVariationRound round : rounds) {
            final TournamentRoundView roundView = new TournamentRoundView.Builder()
                    .level(round.getRoundNumber())
                    .description(round.getDescription())
                    .minStake(round.getMinimumBalance())
                    .clientFile(round.getClientPropertiesId())
                    .minutes(new Duration(round.getRoundLength()).toPeriod().getMinutes())
                    .build();
            result.add(roundView);
        }
        return result;
    }
}
