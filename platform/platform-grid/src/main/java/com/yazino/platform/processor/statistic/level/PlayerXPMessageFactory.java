package com.yazino.platform.processor.statistic.level;

import com.yazino.platform.model.statistic.LevelDefinition;
import com.yazino.platform.model.statistic.PlayerLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;


@Component("playerXPMessageFactory")
public class PlayerXPMessageFactory {

    private static final String FORMAT = "{\"gameType\":\"%s\",\"level\":%s,\"points\":%s,\"toNextLevel\":%s}";

    public String create(final String gameType,
                         final PlayerLevel playerLevel,
                         final LevelDefinition definition) {
        notBlank(gameType, "gameType is blank");
        notNull(playerLevel, "playerLevel is null");
        notNull(definition, "definition is null");
        final int level = playerLevel.getLevel();
        final BigDecimal points = playerLevel.getExperience().subtract(definition.getMinimumPoints());
        final BigDecimal toNextLevel = definition.getMaximumPoints().subtract(definition.getMinimumPoints());
        return String.format(FORMAT, gameType, level, points, toNextLevel);
    }
}
