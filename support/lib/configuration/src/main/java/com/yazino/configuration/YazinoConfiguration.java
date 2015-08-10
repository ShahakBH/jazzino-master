package com.yazino.configuration;

import org.apache.commons.configuration.*;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * A container for Yazino configuration, loaded using the same file order as the Spring context.
 * <p/>
 * To listen for property changes you should implement
 * {@link org.apache.commons.configuration.event.ConfigurationListener} and add your implementation
 * as a listener to the Spring instance of this class, <code>yazinoConfiguration</code>.
 * <p/>
 * Alternately, you can make use of {@link FilteringConfigurationListener} which handles some of the
 * commons configuration complexities.
 * <p/>
 * File changes are scanned for once every {@link #FILE_REFRESH_DELAY} milliseconds.
 *
 * @see "WEB-3406 - commons configuration can't interpolate list values"
 */
public class YazinoConfiguration extends CompositeConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(YazinoConfiguration.class);

    private static final int FILE_REFRESH_DELAY = 30000;

    private static final String OVERRIDE_FILE = "/etc/senet/environment.properties";
    private static final String OVERRIDE_GENERATED_FILE = "/etc/senet/environment.generated.properties";
    private static final String CLASSPATH_FILE = "environment.properties";
    private static final int ONE_SECOND = 1000;

    private final Set<AbstractFileConfiguration> fileConfigurations = new HashSet<AbstractFileConfiguration>();
    private final ConfigurationFileChangeChecker configurationFileChangeChecker;

    public YazinoConfiguration() {
        LOG.trace("Configuration load starting");

        setDefaultListDelimiter('§');

        addFileConfiguration(OVERRIDE_GENERATED_FILE);
        addFileConfiguration(OVERRIDE_FILE);
        addNestedClasspathConfiguration(CLASSPATH_FILE);

        configurationFileChangeChecker = new ConfigurationFileChangeChecker();
        configurationFileChangeChecker.start();

        LOG.trace("Configuration load complete");
    }

    public void shutdown() {
        configurationFileChangeChecker.shutdown();
    }

    private void addNestedClasspathConfiguration(final String fileName) {
        addNestedClasspathConfiguration(fileName, ClassUtils.getDefaultClassLoader());
    }

    private void addNestedClasspathConfiguration(final String resourceName,
                                                 final ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }

        try {
            final URL resourceUrl = classLoader.getResource(resourceName);
            if (resourceUrl != null) {
                LOG.debug("Loading configuration from classpath resource {} with loader {}",
                        resourceName, classLoader);
                addFileConfiguration(new PropertiesConfiguration(resourceUrl));

            } else {
                LOG.debug("Not loading configuration from non-existent classpath resource {} with loader {}",
                        resourceName, classLoader);
            }

        } catch (Exception e) {
            LOG.error("Failed to load configuration from classpath resource: {} with loader: {}",
                    new Object[]{resourceName, classLoader, e});
        }

        if (classLoader.getParent() != null) {
            addNestedClasspathConfiguration(resourceName, classLoader.getParent());
        }
    }

    @Override
    public String[] getStringArray(final String key) {
        final String propertyValue = getString(key);
        if (propertyValue != null) {
            return propertyValue.split(",");
        }
        return null;
    }

    @Override
    public List<Object> getList(final String key) {
        return getList(key, null);
    }

    @Override
    public List<Object> getList(final String key, final List<Object> defaultValue) {
        final String propertyValue = getString(key);
        if (propertyValue != null) {
            final List<Object> result = new ArrayList<Object>();
            Collections.addAll(result, propertyValue.split(","));
            return result;
        }
        return defaultValue;
    }

    private void addFileConfiguration(final String fileName) {
        try {
            final File configurationFile = new File(fileName);
            if (configurationFile.exists()) {
                LOG.debug("Loading configuration from file {}", fileName);
                addFileConfiguration(reloadingPropertiesFrom(configurationFile));

            } else {
                LOG.debug("Not loading configuration from non-existent file {}", fileName);
            }

        } catch (Exception e) {
            LOG.error("Failed to load configuration from file: {}", fileName, e);
        }
    }

    private void addFileConfiguration(final AbstractFileConfiguration configuration) {
        super.addConfiguration(configuration, false);
        fileConfigurations.add(configuration);
    }

    private PropertiesConfiguration reloadingPropertiesFrom(final File configurationFile)
            throws ConfigurationException {
        final FileChangedReloadingStrategy reloadingStrategy = new FileChangedReloadingStrategy();
        reloadingStrategy.setRefreshDelay(FILE_REFRESH_DELAY - ONE_SECOND);

        final PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration(configurationFile);
        propertiesConfiguration.setReloadingStrategy(reloadingStrategy);
        return propertiesConfiguration;
    }

    @Override
    public void addConfiguration(final Configuration config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addConfiguration(final Configuration config, final boolean asInMemory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeConfiguration(final Configuration config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addConfigurationListener(final ConfigurationListener listener) {
        for (AbstractFileConfiguration fileConfiguration : fileConfigurations) {
            fileConfiguration.addConfigurationListener(listener);
        }
        super.addConfigurationListener(listener);
    }

    @Override
    public boolean removeConfigurationListener(final ConfigurationListener listener) {
        for (AbstractFileConfiguration fileConfiguration : fileConfigurations) {
            fileConfiguration.removeConfigurationListener(listener);
        }
        return super.removeConfigurationListener(listener);
    }

    /**
     * This thread will poll the configuration every so often by attempting
     * to retrieve a property. This will trigger the file change check and
     * reload the properties file when required.
     */
    private final class ConfigurationFileChangeChecker extends Thread {
        private volatile boolean running = true;

        private ConfigurationFileChangeChecker() {
            super("configurationFileChangeChecker");
        }

        @Override
        public void run() {
            do {
                sleepFor(FILE_REFRESH_DELAY);
                getString("test.property");
            } while (running);
        }

        private void sleepFor(final int fileRefreshDelay) {
            try {
                Thread.sleep(fileRefreshDelay);
            } catch (InterruptedException e) {
                // ignored
            }
        }

        public void shutdown() {
            running = false;
        }
    }
}
