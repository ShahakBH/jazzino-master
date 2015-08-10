package com.yazino.platform.processor.statistic.opengraph;

import com.google.common.base.Function;
import com.yazino.game.api.facebook.OpenGraphActionProvider;
import com.yazino.platform.opengraph.OpenGraphAction;
import com.yazino.platform.opengraph.OpenGraphObject;
import com.yazino.platform.playerstatistic.StatisticEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DefaultStatisticToActionTransformer implements Function<StatisticEvent, OpenGraphAction> {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultStatisticToActionTransformer.class);

    private final Map<String, OpenGraphAction> map = new HashMap<>();

    @Override
    public OpenGraphAction apply(final StatisticEvent statisticEvent) {
        final OpenGraphAction openGraphAction = getMap().get(statisticEvent.getEvent());
        LOG.debug("Transformed {} into action {}", statisticEvent, openGraphAction);
        return openGraphAction;
    }

    public Map<String, OpenGraphAction> getMap() {
        return map;
    }

    private void addAction(final String statisticEventName,
                           final String actionName,
                           final String objectType,
                           final String objectId) {
        map.put(statisticEventName, createAction(actionName, objectType, objectId));
    }

    private void removeAction(final String statisticEventName) {
        map.remove(statisticEventName);
    }

    private OpenGraphAction createAction(final String actionName,
                                         final String objectType,
                                         final String objectId) {
        return new OpenGraphAction(actionName, new OpenGraphObject(objectType, objectId));
    }

    public void registerActions(final OpenGraphActionProvider actionProvider) {
        if (actionProvider == null) {
            LOG.warn("Received null action provider, ignoring");
            return;
        }

        for (com.yazino.game.api.facebook.OpenGraphAction openGraphAction : actionProvider.getOpenGraphActions()) {
            LOG.debug("Registering action: {}", openGraphAction);
            addAction(openGraphAction.getStatisticEventName(), openGraphAction.getActionName(),
                    openGraphAction.getObjectType(), openGraphAction.getObjectId());
        }
    }

    public void unregisterActions(final OpenGraphActionProvider actionProvider) {
        if (actionProvider == null) {
            LOG.warn("Received null action provider, ignoring");
            return;
        }

        for (com.yazino.game.api.facebook.OpenGraphAction openGraphAction : actionProvider.getOpenGraphActions()) {
            LOG.debug("Unregistering action: {}", openGraphAction);
            removeAction(openGraphAction.getStatisticEventName());
        }
    }
}
