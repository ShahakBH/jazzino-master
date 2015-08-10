package com.yazino.bi.operations.engagement;

import java.math.BigDecimal;

public class AppRequestTargetBuilder {
    private Integer id;
    private Integer appRequestId;
    private BigDecimal playerId;
    private String externalId;
    private String gameType;

    public AppRequestTargetBuilder withId(final Integer value) {
        this.id = value;
        return this;
    }

    public AppRequestTargetBuilder withAppRequestId(final Integer value) {
        this.appRequestId = value;
        return this;
    }

    public AppRequestTargetBuilder withPlayerId(final BigDecimal value) {
        this.playerId = value;
        return this;
    }

    public AppRequestTargetBuilder withExternalId(final String value) {
        this.externalId = value;
        return this;
    }

    public AppRequestTargetBuilder withGameType(final String value) {
        this.gameType = value;
        return this;
    }

    public AppRequestTarget build() {
        return new AppRequestTarget(id, appRequestId, playerId, externalId, gameType);
    }
}
