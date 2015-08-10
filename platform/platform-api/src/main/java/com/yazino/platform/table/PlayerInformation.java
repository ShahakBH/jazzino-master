package com.yazino.platform.table;

import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.ObservableContext;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerInformation implements Serializable {
    private static final long serialVersionUID = -1265042449672692589L;

    private static final String DEFAULT_NAME_FOR_TEMPORARY_HACK = "Unknown";

    private final BigDecimal playerId;

    private String name = DEFAULT_NAME_FOR_TEMPORARY_HACK;
    private BigDecimal accountId;
    private BigDecimal cachedBalance;
    private BigDecimal sessionId;
    private long acknowledgedIncrement;

    public PlayerInformation(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        this.playerId = playerId;
    }

    public PlayerInformation(final BigDecimal playerId,
                             final String name,
                             final BigDecimal accountId,
                             final BigDecimal sessionId,
                             final BigDecimal cachedBalance) {

        notNull(playerId, "playerId may not be null");
        notNull(ensureNotNullAsTemporaryWorkaround(name), "name may not be null");
        notNull(accountId, "accountId may not be null");
        notNull(cachedBalance, "cachedBalance may not be null");

        this.playerId = playerId;
        this.name = ensureNotNullAsTemporaryWorkaround(name);
        this.accountId = accountId;
        this.cachedBalance = cachedBalance;
        this.sessionId = sessionId;
    }

    // Currently, and incorrectly, neither the database nor the service layer prevents a player's name
    // from being null.
    //
    // IllegalArgumentException were being thrown when GameHost tried to execute commands for
    // players with null names.
    //
    // This workaround ensures that GameHost will not receive a null name, even if the player's
    // name is null in the database.
    //
    // A proper solution will consist of the following:
    //  - ensure the database does not accept null names
    //  - ensure that the service layer rejects null names
    //  - ensure that all client applications refrain from creating players with null names
    //  The first step will obviously be sufficient; the others will ensure that the system
    // detects problems at the earliest point
    //
    // The database/service layers will shortly be replaced in the move to the
    // non-Gigaspaces Community server - this hack will tide us over for the short-term.
    private String ensureNotNullAsTemporaryWorkaround(final String nameToTest) {
        if (nameToTest == null) {
            return "Unknown";
        }
        return nameToTest;
    }

    public BigDecimal getCachedBalance() {
        return cachedBalance;
    }

    public void setCachedBalance(final BigDecimal cachedBalance) {
        notNull(cachedBalance, "balance may not be null");
        this.cachedBalance = cachedBalance;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        notNull(ensureNotNullAsTemporaryWorkaround(name), "name may not be null");
        this.name = ensureNotNullAsTemporaryWorkaround(name);
    }

    public BigDecimal getAccountId() {
        if (accountId == null) {
            throw new IllegalStateException("Account ID has not been set or has been released");
        }
        return accountId;
    }

    public void setAccountId(final BigDecimal accountId) {
        notNull(accountId, "accountId may not be null");

        if (this.accountId != null && this.accountId.equals(accountId)) {
            return;
        }

        this.accountId = accountId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public long getAcknowledgedIncrement() {
        return acknowledgedIncrement;
    }

    public void setAcknowledgedIncrement(final long acknowledgedIncrement) {
        notNull(acknowledgedIncrement, "acknowledgedIncrement may not be null");
        this.acknowledgedIncrement = acknowledgedIncrement;
    }

    public GamePlayer toGamePlayer() {
        return new GamePlayer(playerId, sessionId, name);
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public void setSessionId(final BigDecimal sessionId) {
        this.sessionId = sessionId;
    }

    public ObservableContext toObservableContext() {
        return new ObservableContext(toGamePlayer(), cachedBalance);
    }

    public ObservableContext toObservableContext(final boolean skipIfPossible,
                                                 final long startIncrement) {
        return new ObservableContext(toGamePlayer(), cachedBalance, skipIfPossible, startIncrement);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final PlayerInformation rhs = (PlayerInformation) obj;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(cachedBalance, rhs.cachedBalance)
                .append(acknowledgedIncrement, rhs.acknowledgedIncrement)
                .append(sessionId, rhs.sessionId)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(accountId, rhs.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(name)
                .append(BigDecimals.strip(accountId))
                .append(cachedBalance)
                .append(acknowledgedIncrement)
                .append(sessionId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(name)
                .append(accountId)
                .append(cachedBalance)
                .append(acknowledgedIncrement)
                .append(sessionId)
                .toString();
    }
}
