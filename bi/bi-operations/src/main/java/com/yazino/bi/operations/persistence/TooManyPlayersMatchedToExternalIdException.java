package com.yazino.bi.operations.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access exception thrown when an external id was mapped to several player ids.
 * We expect a one-to-one relationship.
 */
public class TooManyPlayersMatchedToExternalIdException extends RuntimeException {
    private static final long serialVersionUID = 1;

    private final String provider;
    private final String externalId;
    private final List<BigDecimal> playerIdsMatched;

    public TooManyPlayersMatchedToExternalIdException(final String provider,
                                                      final String externalId,
                                                      final List<BigDecimal> playerIdsMatched) {
        this.provider = provider;
        this.externalId = externalId;
        this.playerIdsMatched = new ArrayList<BigDecimal>(playerIdsMatched);
    }

    public String getProvider() {
        return provider;
    }

    public String getExternalId() {
        return externalId;
    }

    public List<BigDecimal> getPlayerIdsMatched() {
        return playerIdsMatched;
    }
}
