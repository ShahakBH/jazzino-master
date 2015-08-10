package com.yazino.platform.test;

import com.yazino.game.api.*;

import java.math.BigDecimal;
import java.util.*;

public class PrintlnRules implements GameRules {
    public static final String GAME_TYPE = "PRINTLN";
    private static final GameMetaData META_DATA = new GameMetaDataBuilder().build();
    private static final long ONE_SECOND = 1000L;

    public String getGameType() {
        return GAME_TYPE;
    }

    @Override
    public GameMetaData getMetaData() {
        return META_DATA;
    }

    public ExecutionResult execute(final ExecutionContext context,
                                   final Command c)
            throws GameException {
        System.out.println(c);
        if (c.getType().equals("SCHEDULE")) {
            return new ExecutionResult.Builder(this, context.getGameStatus()).scheduledEvents(Arrays.asList(
                    new PrintLnEvent(ONE_SECOND, null, "from command " + c).toScheduledEvent())).build();
        } else {
            return new ExecutionResult.Builder(this, context.getGameStatus()).build();
        }
    }

    public ExecutionResult execute(final ExecutionContext executionContext,
                                   final ScheduledEvent evt)
            throws GameException, IllegalAccessException, ClassNotFoundException, InstantiationException {
        if ("player".equals(evt.getEventClassName())) {
            final PrintlnStatus status = new PrintlnStatus(executionContext.getGameStatus());
            final List<PlayerAtTableInformation> players
                    = new ArrayList<PlayerAtTableInformation>(status.getPlayerInformation());
            if (evt.getProperties().get("add") != null) {

                final GamePlayer player = new GamePlayer(new BigDecimal(evt.getProperties().get("add")),
                        null, evt.getProperties().get("add"));
                final PlayerAtTableInformation playerAtTableInformation
                        = new PlayerAtTableInformation(player, Collections.<String, String>emptyMap());
                players.add(playerAtTableInformation);
            }
            if (evt.getProperties().get("remove") != null) {
                final BigDecimal playerId = new BigDecimal(evt.getProperties().get("remove"));
                for (PlayerAtTableInformation p : new ArrayList<PlayerAtTableInformation>(players)) {
                    if (p.getPlayer().getId().compareTo(playerId) == 0) {
                        players.remove(p);
                    }
                }
            }
            final PrintlnStatus status2 = new PrintlnStatus();
            status2.setName(status.getName());
            status2.setId(status.getId());
            status2.setPlayersAtTable(players);
            return new ExecutionResult.Builder(this, new GameStatus(status2)).build();
        }
        return new ExecutionResult.Builder(this, executionContext.getGameStatus()).build();
    }

    public ExecutionResult startNewGame(final GameCreationContext creationContext) {
        final PrintlnStatus status = new PrintlnStatus();

        status.setPlayersAtTable(creationContext.getPlayersAtTableInformation());
        return new ExecutionResult.Builder(this, new GameStatus(status)).build();
    }

    public ExecutionResult startNextGame(final ExecutionContext context) {
        return new ExecutionResult.Builder(this, context.getGameStatus()).build();
    }

    public ExecutionResult processTransactionResult(final ExecutionContext context,
                                                    final TransactionResult result) {
        return new ExecutionResult.Builder(this, context.getGameStatus()).build();
    }

    @Override
    public ExecutionResult processExternalCallResult(ExecutionContext context, ExternalCallResult result) throws GameException {
        return new ExecutionResult.Builder(this, context.getGameStatus()).build();
    }

    @Override
    public Collection<PlayerAtTableInformation> getPlayerInformation(final GameStatus gameStatus) {
        return new PrintlnStatus(gameStatus).getPlayerInformation();
    }

    @Override
    public boolean isAPlayer(final GameStatus gameStatus, final GamePlayer player) {
        return false;
    }

    @Override
    public boolean isComplete(final GameStatus gameStatus) {
        return false;
    }

    @Override
    public boolean isAvailableForPlayerJoining(final GameStatus gameStatus) {
        return true;
    }

    @Override
    public boolean canBeClosed(final GameStatus gameStatus) {
        return false;
    }

    @Override
    public ObservableStatus getObservableStatus(final GameStatus gameStatus, final ObservableContext context) {
        return null;
    }

    @Override
    public int getNumberOfSeatsTaken(final GameStatus gameStatus) {
        final Collection<PlayerAtTableInformation> playersAtTable = new PrintlnStatus(gameStatus).getPlayerInformation();
        if (playersAtTable != null) {
            return playersAtTable.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getJoiningDesirability(final GameStatus gameStatus) {
        return new PrintlnStatus(gameStatus).getJoiningDesirability();
    }

    @Override
    public String toAuditString(final GameStatus gameStatus) {
        return "";
    }
}
