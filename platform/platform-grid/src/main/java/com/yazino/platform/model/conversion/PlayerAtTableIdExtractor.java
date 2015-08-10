package com.yazino.platform.model.conversion;

import com.google.common.base.Function;
import com.yazino.game.api.PlayerAtTableInformation;

import java.math.BigDecimal;

public class PlayerAtTableIdExtractor implements Function<PlayerAtTableInformation, BigDecimal> {
    @Override
    public BigDecimal apply(final PlayerAtTableInformation playerAtTableInformation) {
        return playerAtTableInformation.getPlayer().getId();
    }
}
