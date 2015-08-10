package com.yazino.game.api;

import com.yazino.game.api.facebook.OpenGraphActionProvider;
import com.yazino.game.api.statistic.GameStatisticConsumer;
import com.yazino.game.api.statistic.GameStatisticProducer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public abstract class GenericGameRulesActivator implements BundleActivator {

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        bundleContext.registerService(GameRules.class.getName(), getGameRules(), null);

        final GameStatisticProducer gameStatisticProducer = getGameStatisticsProducer();
        if (gameStatisticProducer != null) {
            bundleContext.registerService(GameStatisticProducer.class.getName(), gameStatisticProducer, null);
        }

        final GameStatisticConsumer gameStatisticConsumer = getGameStatisticsConsumer();
        if (gameStatisticConsumer != null) {
            bundleContext.registerService(GameStatisticConsumer.class.getName(), gameStatisticConsumer, null);
        }

        final OpenGraphActionProvider openGraphActionProvider = getOpenGraphActionProvider();
        if (openGraphActionProvider != null) {
            bundleContext.registerService(OpenGraphActionProvider.class.getName(), openGraphActionProvider, null);
        }
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
    }

    public abstract GameRules getGameRules();

    public abstract GameStatisticProducer getGameStatisticsProducer();

    public abstract GameStatisticConsumer getGameStatisticsConsumer();

    public OpenGraphActionProvider getOpenGraphActionProvider() {
        return null;
    }
}
