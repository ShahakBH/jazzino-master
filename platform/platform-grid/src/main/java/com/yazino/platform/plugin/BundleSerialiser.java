package com.yazino.platform.plugin;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;

public class BundleSerialiser {
    private static final Logger LOG = LoggerFactory.getLogger(BundleSerialiser.class);

    private final BundleContext bundleContext;

    public BundleSerialiser(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public byte[] serialise(final Object object) {
        if (object == null) {
            return null;
        }

        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(object);
            return out.toByteArray();

        } catch (IOException e) {
            LOG.error("Failed to serialise {}", object, e);
            return null;
        }
    }

    public Object deserialise(final byte[] serialisedObject) {
        if (serialisedObject == null) {
            return null;
        }
        try {
            return new BundleObjectInputStream(new ByteArrayInputStream(serialisedObject)).readObject();

        } catch (Exception e) {
            LOG.error("Failed to deserialise object", e);
            return null;
        }
    }

    private class BundleObjectInputStream extends ObjectInputStream {
        private Bundle preferredBundle;
        private Collection<String> preferredBundleResources;

        public BundleObjectInputStream(final InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            final String resourceName = resourceNameOf(desc);

            if (preferredBundle != null && preferredBundleResources != null) {
                if (preferredBundleResources.contains(resourceName)) {
                    try {
                        LOG.debug("Loading {} from preferred bundle {}", desc.getName(), preferredBundle.getSymbolicName());
                        return loadClassFrom(preferredBundle, desc.getName());

                    } catch (ClassNotFoundException e) {
                        LOG.warn("Load from preferred bundle {} failed for class {}", preferredBundle.getSymbolicName(), desc.getName());
                    }
                }
            }

            if (bundleContext != null) {
                final Bundle[] installedBundles = bundleContext.getBundles();
                for (Bundle installedBundle : installedBundles) {
                    if (installedBundle.getBundleId() == 0) {
                        continue;
                    }

                    final BundleWiring bundleWiring = (BundleWiring) installedBundle.adapt(BundleWiring.class);
                    final Collection<String> resources = bundleWiring.listResources("/", "*.class",
                            BundleWiring.LISTRESOURCES_LOCAL | BundleWiring.LISTRESOURCES_RECURSE);
                    if (resources.contains(resourceName)) {
                        try {
                            LOG.debug("Loading {} from bundle {}", desc.getName(), installedBundle.getSymbolicName());
                            final Class<?> theClass = loadClassFrom(installedBundle, desc.getName());
                            preferredBundle = installedBundle;
                            preferredBundleResources = resources;
                            return theClass;

                        } catch (ClassNotFoundException ignored) {
                            LOG.warn("Load from bundle {} failed for class {}", installedBundle.getSymbolicName(), desc.getName());
                        }
                    }
                }
            }

            LOG.debug("Loading {} from GS class-loader", desc.getName());
            return super.resolveClass(desc);
        }

        private String resourceNameOf(final ObjectStreamClass desc) {
            String className = desc.getName();
            if (className.startsWith("[")) {
                final String arrayClass = arrayClassNameOf(className);
                if (arrayClass != null) {
                    className = arrayClass;
                }
            }
            return className.replace('.', '/') + ".class";
        }

        private Class<?> loadClassFrom(final Bundle bundle, final String className) throws ClassNotFoundException {
            // derived from http://stackoverflow.com/questions/3475352/deserialization-of-arrays-of-custom-type-in-osgi
            if (className.startsWith("[")) {
                final String arrayClass = arrayClassNameOf(className);
                if (arrayClass != null) {
                    final Class<?> klass = bundle.loadClass(arrayClass);
                    return Class.forName(className, false, klass.getClassLoader());
                }
            }

            return bundle.loadClass(className);
        }

        private String arrayClassNameOf(final String className) {
            String arrayClass = null;
            int pos = 1;
            while (pos < className.length() && className.charAt(pos) == '[') {
                ++pos;
            }

            if (pos < className.length()
                    && className.charAt(pos) == 'L'
                    && className.charAt(className.length() - 1) == ';') {
                arrayClass = className.substring(pos + 1, className.length() - 1);
            }

            return arrayClass;
        }
    }
}
