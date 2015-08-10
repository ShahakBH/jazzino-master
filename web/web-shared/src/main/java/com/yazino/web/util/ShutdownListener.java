package com.yazino.web.util;

import com.gigaspaces.lrmi.classloading.protocol.lrmi.LRMIConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.Map;

public class ShutdownListener implements ServletContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(ShutdownListener.class);
    private static final int QUARTZ_SHUTDOWN_DELAY = 1000;

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        final BeanFactory bf = currentApplicationContext();
        if (bf instanceof ConfigurableApplicationContext) {
            final ConfigurableApplicationContext context = (ConfigurableApplicationContext) bf;

            shutdownQuartz(context);
            shutdownGigaSpace();

            context.close();

            deregisterDrivers();
        }
    }

    private void shutdownGigaSpace() {
        try {
            LRMIConnection.clearConnection();
        } catch (Exception e) {
            LOG.error("Failed to shutdown LRMI Connection", e);
        }
    }

    private void deregisterDrivers() {
        final Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            final Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
            } catch (Exception e) {
                LOG.error("Failed to de-register driver: " + driver, e);
            }
        }
    }

    private void shutdownQuartz(final ConfigurableApplicationContext context) {
        // work-around for https://jira.terracotta.org/jira/browse/QTZ-192

        final Map<String, SchedulerFactoryBean> schedulerFactories
                = context.getBeansOfType(SchedulerFactoryBean.class);
        for (SchedulerFactoryBean schedulerFactoryBean : schedulerFactories.values()) {
            try {
                schedulerFactoryBean.getScheduler().shutdown(true);

            } catch (Exception e) {
                LOG.error("Scheduler shutdown failed", e);
            }
        }

        try {
            Thread.sleep(QUARTZ_SHUTDOWN_DELAY);
        } catch (InterruptedException e) {
            // ignored
        }
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
    }

    WebApplicationContext currentApplicationContext() {
        return ContextLoader.getCurrentWebApplicationContext();
    }
}
