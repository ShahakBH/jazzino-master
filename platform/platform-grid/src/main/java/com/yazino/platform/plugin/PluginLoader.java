package com.yazino.platform.plugin;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

public class PluginLoader implements PluginManager {
    private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);

    private final String pluginDirectory;
    private final BundleContext bundleContext;
    private final BundleSerialiser bundleSerialiser;

    public PluginLoader(final String pluginDirectory,
                        final BundleContext bundleContext) {
        notEmpty(pluginDirectory, "pluginDirectory cannot be empty");
        notNull(bundleContext, "bundleContext cannot be null");

        this.pluginDirectory = pluginDirectory;
        this.bundleContext = bundleContext;
        this.bundleSerialiser = new BundleSerialiser(bundleContext);
    }

    @Override
    public byte[] serialise(final Object object) {
        return bundleSerialiser.serialise(object);
    }

    @Override
    public Object deserialise(final byte[] serialisedObject) {
        return bundleSerialiser.deserialise(serialisedObject);
    }

    @Override
    public String getPluginLocation() {
        return this.pluginDirectory;
    }

    @Override
    public void syncPlugins() throws IOException {
        final Set<String> jarLocations = getPluginLocations(pluginDirectory);
        uninstallRemovedPlugins(jarLocations, bundleContext);
        installPlugins(jarLocations, bundleContext);
    }

    @Override
    public void syncPlugin(final String filename) throws IOException {
        final Set<String> jarLocations = getPluginLocations(pluginDirectory);
        for (String jarLocation : jarLocations) {
            if (jarLocation.contains(filename)) {
                uninstallPlugin(jarLocation, bundleContext);
                installPlugin(jarLocation, bundleContext);
            }
        }
    }

    private static Set<String> getPluginLocations(final String path) {
        LOG.debug("Looking for jars in {}", path);
        final File pluginsDir = new File(path);
        if (!pluginsDir.exists() && !pluginsDir.isDirectory()) {
            LOG.warn("Directory {} does not exist!", path);
            return Collections.emptySet();
        }
        final Set<String> jarLocations = new HashSet<String>();
        final File[] pluginCandidates = pluginsDir.listFiles();
        if (pluginCandidates == null) {
            return Collections.emptySet();
        }
        for (File candidate : pluginCandidates) {
            if (candidate.getName().endsWith(".jar")) {
                jarLocations.add(candidate.toURI().toString());
            }
        }
        LOG.debug("Found {}", jarLocations);
        return jarLocations;
    }

    private static void uninstallRemovedPlugins(final Set<String> jarLocations, final BundleContext bundleContext) {
        final Bundle[] installedBundles = bundleContext.getBundles();
        for (Bundle installedBundle : installedBundles) {
            if (installedBundle.getBundleId() == 0) {
                continue;
            }
            final String location = installedBundle.getLocation();
            if (!jarLocations.contains(location)) {
                LOG.debug("Plugin {} was removed. Removing...", location);
                uninstallPlugin(location, bundleContext);
            }
        }
    }

    private static void installPlugins(final Set<String> jarLocations, final BundleContext bundleContext)
            throws IOException {
        for (String location : jarLocations) {
            uninstallPlugin(location, bundleContext);
            installPlugin(location, bundleContext);
        }
    }

    private static void installPlugin(final String location, final BundleContext bundleContext) throws IOException {
        try {
            LOG.debug("Installing and starting {}", location);
            final Bundle bundle = bundleContext.installBundle(location);
            bundle.start();

        } catch (Exception e) {
            LOG.error("Error installing {}", location, e);
            throw new IOException(e);
        }
    }

    private static void uninstallPlugin(final String location, final BundleContext bundleContext) {
        try {
            final Bundle bundle = bundleContext.getBundle(location);
            if (bundle != null) {
                LOG.debug("Uninstalling {}", location);
                bundle.uninstall();
            }
        } catch (Exception e) {
            LOG.error("Error uninstalling {}", location, e);
        }
    }

}
