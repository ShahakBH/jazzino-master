package com.yazino.game.api;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public interface GameRules {
    String AUDIT_STRING_DELIMITER_MINOR = "\t";
    String AUDIT_STRING_DELIMITER_MAJOR = "\n";
    String AUDIT_STRING_DELIMITER_SECTION = "\n----\n";

    String getGameType();

    GameMetaData getMetaData();

    ExecutionResult execute(ExecutionContext executionContext,
                            Command c)
            throws GameException;

    ExecutionResult execute(ExecutionContext executionContext,
                            ScheduledEvent evt)
            throws GameException,
            IllegalAccessException,
            ClassNotFoundException,
            InstantiationException,
            NoSuchMethodException,
            InvocationTargetException;

    ExecutionResult startNewGame(GameCreationContext creationContext);

    ExecutionResult startNextGame(ExecutionContext executionContext);

    ExecutionResult processTransactionResult(ExecutionContext context,
                                             TransactionResult result)
            throws GameException;

    ExecutionResult processExternalCallResult(ExecutionContext context,
                                              ExternalCallResult result)
        throws GameException;

    // players at table - used to determine who gets messages (observers?)
    // nb - need to keep leavers for an extra turn so that they receive leave message
    Collection<PlayerAtTableInformation> getPlayerInformation(GameStatus gameStatus);

    // convenience
    boolean isAPlayer(GameStatus gameStatus, GamePlayer player);

    // indicates startNextGame required - triggers auditing
    boolean isComplete(GameStatus gameStatus);

    // host looking for seat
    boolean isAvailableForPlayerJoining(GameStatus gameStatus);

    boolean canBeClosed(GameStatus gameStatus);

    // player-specific (null player means observer)
    ObservableStatus getObservableStatus(GameStatus gameStatus, ObservableContext context);


    int getNumberOfSeatsTaken(GameStatus gameStatus);

    /**
     * Factor indicating whether new players should join this game. The higher the factor the more desirable this game
     * is.
     * <p></p>
     * The table allocation algorithm gives this factor a higher priority over the  number of free seats when deciding
     * which table to assign a player to.
     *
     * For games where joining is solely free seat based, simply implement this method returning a constant.
     */
    int getJoiningDesirability(GameStatus gameStatus);

    // for audit table
    String toAuditString(GameStatus gameStatus);
}
