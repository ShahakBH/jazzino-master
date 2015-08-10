package strata.server.lobby.promotion.service;

import strata.server.lobby.promotion.domain.ExternalCredentials;

import java.math.BigDecimal;

public interface ExternalCredentialsProvider {

    ExternalCredentials lookupByPlayerId(BigDecimal playerId);

}
