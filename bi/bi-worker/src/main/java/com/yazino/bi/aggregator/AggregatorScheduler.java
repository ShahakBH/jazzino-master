package com.yazino.bi.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AggregatorScheduler implements ApplicationContextAware {

    private SchedulerDao schedulerDao;
    private ApplicationContext context;

    private static final Logger LOG = LoggerFactory.getLogger(AggregatorScheduler.class);

    @Autowired
    public AggregatorScheduler(final SchedulerDao schedulerDao) {
        this.schedulerDao = schedulerDao;
    }

    @Scheduled(fixedDelay = 60000) // every ten minutes//:TODO fix this back to 10 minutes
    public void checkForScheduledAggregators() {
        LOG.debug("running manual scheduler for aggregators");
        final List<String> scheduledAggregators = schedulerDao.getScheduledAggregators();
        for (String scheduledAggregator : scheduledAggregators) {
            LOG.info("aggregator scheduled {} so running", scheduledAggregator);
            try {
                final Aggregator aggregator;
                aggregator = context.getBean(scheduledAggregator, Aggregator.class);
                aggregator.update();
            } catch (BeansException e) {
                LOG.error("couldn't load up aggregator with name " + scheduledAggregator, e);
            }
            //carry on
        }
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
