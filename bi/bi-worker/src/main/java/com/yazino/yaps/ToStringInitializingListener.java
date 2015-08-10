package com.yazino.yaps;

import org.apache.commons.lang3.builder.StandardToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Initialized {@link ToStringBuilder} with a custom style.
 */
public class ToStringInitializingListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(ToStringInitializingListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        StandardToStringStyle style = new StandardToStringStyle();
        style.setUseShortClassName(true);
        ToStringBuilder.setDefaultStyle(style);
        LOG.info("contextInitialized: setup ToStringBuilder.defaultStyle");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
