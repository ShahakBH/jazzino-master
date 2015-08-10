package com.yazino.platform.plugin;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.framework.FrameworkFactory;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.management.VMManagement;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * The packages specified under the FRAMEWORK_SYSTEMPACKAGES_EXTRA property will be included in the MANIFEST file
 * under the 'Export-Package' heading
 */
public final class PluginFramework {
    private static final Logger LOG = LoggerFactory.getLogger(PluginFramework.class);

    private static final AtomicInteger INSTANCE_ID_SOURCE = new AtomicInteger();

    private final String pluginDirectory;
    private final String cacheDirectory;
    private final String[] exposedPackages;
    private final BundleActivator[] activators;
    private final String factoryId;
    private final int instanceId;

    public PluginFramework(final String pluginDirectory,
                           final String cacheDirectory,
                           final String[] exposedPackages,
                           final BundleActivator... activators) throws Exception {
        notEmpty(pluginDirectory, "pluginDirectory cannot be empty");
        notNull(exposedPackages, "exposedPackages cannot be null");

        this.pluginDirectory = pluginDirectory;
        this.cacheDirectory = cacheDirectory;
        this.exposedPackages = exposedPackages;
        this.activators = activators;

        instanceId = INSTANCE_ID_SOURCE.getAndIncrement();
        factoryId = super.toString().substring(super.toString().indexOf("@") + 1);
    }

    public PluginLoader init() throws BundleException {
        LOG.debug("Initialising plugin loader with plugin dir {}; cache dir {}, exposed packages {}",
                pluginDirectory, cacheDirectory, exposedPackages);

        final Map<String, Object> configMap = new HashMap<String, Object>();
        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, StringUtils.join(exposedPackages, ","));
        configMap.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, Arrays.asList(activators));
        configMap.put(FelixConstants.LOG_LEVEL_PROP, String.valueOf(org.apache.felix.framework.Logger.LOG_INFO));
        configMap.put(FelixConstants.LOG_LOGGER_PROP, new OSGIPluginLogger());
        configMap.put(FelixConstants.SERVICE_URLHANDLERS_PROP, "false");
        configMap.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);

        if (cacheDirectory != null && !cacheDirectory.isEmpty()) {
            configMap.put(Constants.FRAMEWORK_STORAGE, addPidAndInstanceIdTo(cacheDirectory));
        } else {
            configMap.put(Constants.FRAMEWORK_STORAGE, defaultCacheDirectory());
        }

        final FrameworkFactory factory = new FrameworkFactory();
        final Framework framework = factory.newFramework(configMap);
        framework.init();
        framework.start();

        LOG.info("Framework started, cache directory is " + configMap.get(Constants.FRAMEWORK_STORAGE));

        return new PluginLoader(pluginDirectory, framework.getBundleContext());
    }

    private String defaultCacheDirectory() {
        return String.format("felix-cache/%d-%s-%d", getPid(), factoryId, instanceId);
    }

    private String addPidAndInstanceIdTo(final String directory) {
        return String.format("%s/%d-%s-%d", directory, getPid(), factoryId, instanceId);
    }

    private int getPid() {
        try {
            final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            final Field jvm = runtime.getClass().getDeclaredField("jvm");
            jvm.setAccessible(true);
            final VMManagement management = (VMManagement) jvm.get(runtime);
            java.lang.reflect.Method pidMethod = management.getClass().getDeclaredMethod("getProcessId");
            pidMethod.setAccessible(true);
            return (Integer) pidMethod.invoke(management);

        } catch (Exception e) {
            LOG.error("Failed to find PID", e);
            return -1;
        }
    }
}
