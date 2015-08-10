package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.Validate.notNull;

public class ExecutionContext {
    private final String auditLabel;
    private final GamePlayerWalletFactory gamePlayerWalletFactory;
    private final GameInformation gameInfo;
    private final ExternalGameService externalGameService;

    public ExecutionContext(final GameInformation info,
                            final GamePlayerWalletFactory gamePlayerWalletFactory,
                            final ExternalGameService externalGameService,
                            final String auditLabel) {
        notNull(info, "Game information is required");
        notNull(gamePlayerWalletFactory, "Game Player Wallet Factory is required");
        notNull(externalGameService, "External Service is required");
        notNull(auditLabel, "Audit label is required");
        this.gameInfo = info;
        this.gamePlayerWalletFactory = gamePlayerWalletFactory;
        this.auditLabel = auditLabel;
        this.externalGameService = externalGameService;
    }

    public String getAuditLabel() {
        return auditLabel;
    }

    public GameStatus getGameStatus() {
        return gameInfo.getCurrentGame();
    }

    public Long getGameId() {
        return gameInfo.getGameId();
    }

    public GamePlayerWalletFactory getGamePlayerWalletFactory() {
        return gamePlayerWalletFactory;
    }

    public ExternalGameService getExternalGameService() {
        return externalGameService;
    }

    public long getNextIncrement() {
        if (gameInfo.getIncrement() == null) {
            return 1;
        }
        return gameInfo.getIncrement() + 1;
    }

    public boolean isAddingPlayersPossible() {
        return gameInfo.isAddingPlayersPossible();
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
        final ExecutionContext rhs = (ExecutionContext) obj;
        return new EqualsBuilder()
                .append(auditLabel, rhs.auditLabel)
                .append(gameInfo, rhs.gameInfo)
                .append(gamePlayerWalletFactory, rhs.gamePlayerWalletFactory)
                .append(externalGameService, rhs.externalGameService)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(auditLabel)
                .append(gamePlayerWalletFactory)
                .append(gameInfo)
                .append(externalGameService)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("auditLabel", auditLabel)
                .append("gamePlayerWalletFactory", gamePlayerWalletFactory)
                .append("gameInfo", gameInfo)
                .append("externalService", externalGameService)
                .toString();
    }
}
