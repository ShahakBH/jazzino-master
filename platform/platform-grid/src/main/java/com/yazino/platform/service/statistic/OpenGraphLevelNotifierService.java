package com.yazino.platform.service.statistic;

import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.opengraph.OpenGraphAction;
import com.yazino.platform.opengraph.OpenGraphActionMessage;
import com.yazino.platform.opengraph.OpenGraphObject;
import com.yazino.platform.processor.statistic.level.OpenGraphLevelPrefixes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OpenGraphLevelNotifierService {

    private static final String ACTION_NAME = "gain";
    private static final String OBJECT_TYPE = "level";

    private final OpenGraphLevelPrefixes levelPrefixesMap;

    @Autowired
    public OpenGraphLevelNotifierService(
            @Qualifier("openGraphActionQueuePublishingService")
            final QueuePublishingService<OpenGraphActionMessage> openGraphActionQueuePublishingService,
            final OpenGraphLevelPrefixes levelPrefixesMap) {
        this.openGraphActionQueuePublishingService = openGraphActionQueuePublishingService;
        this.levelPrefixesMap = levelPrefixesMap;
    }

    private final QueuePublishingService<OpenGraphActionMessage> openGraphActionQueuePublishingService;

    public void publishNewLevel(final BigDecimal playerId,
                                final String gameType,
                                final int level) {

        final String levelPrefix = levelPrefixesMap.getLevelPrefix(gameType);
        if (levelPrefix != null) {
            openGraphActionQueuePublishingService.send(
                    new OpenGraphActionMessage(playerId.toBigInteger(),
                            gameType,
                            new OpenGraphAction(ACTION_NAME, new OpenGraphObject(
                                    OBJECT_TYPE, levelPrefix + "_level_" + level))));
        }
    }
}
