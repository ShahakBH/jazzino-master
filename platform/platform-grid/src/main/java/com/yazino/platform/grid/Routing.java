package com.yazino.platform.grid;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.google.common.collect.Maps;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoContext;
import org.openspaces.core.space.mode.PostBackup;
import org.openspaces.core.space.mode.PostPrimary;
import org.openspaces.core.space.mode.PreBackup;
import org.openspaces.core.space.mode.PrePrimary;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class Routing {
    private final Set<Class> simpleClasses = new HashSet<>();
    private final Map<Class, Method> routingMethods = new ConcurrentHashMap<>();

    @ClusterInfoContext
    private ClusterInfo clusterInfo;

    private SpaceState state;

    {
        simpleClasses.add(BigDecimal.class);
        simpleClasses.add(BigInteger.class);
        simpleClasses.add(String.class);
        simpleClasses.add(Integer.class);
        simpleClasses.add(Long.class);
        simpleClasses.add(Short.class);
    }

    @PrePrimary
    public void prePrimary() {
        state = SpaceState.PRE_PRIMARY;
    }

    @PreBackup
    public void preBackup() {
        state = SpaceState.PRE_BACKUP;
    }

    @PostPrimary
    public void postPrimary() {
        state = SpaceState.PRIMARY;
    }

    @PostBackup
    public void postBackup() {
        state = SpaceState.BACKUP;
    }

    void setClusterInfo(final ClusterInfo clusterInfo) {
        this.clusterInfo = clusterInfo;
    }

    public boolean isRoutedToCurrentPartition(final Object spaceObject) {
        notNull(spaceObject, "spaceObject may not be null");

        return partitionNumberFor(spaceIdFor(spaceObject)) == partitionId();
    }

    public boolean isBackup() {
        // cluterInfo.getBackupId becomes inaccurate when you're running multiple app
        // in a single GSC with backups.
        return state != null && state == SpaceState.BACKUP;
    }

    public Object spaceIdFor(final Object spaceObject) {
        notNull(spaceObject, "spaceObject may not be null");

        // From http://wiki.gigaspaces.com/wiki/display/XAP9/Routing+In+Partitioned+Spaces
        //  The routing property can be explicitly set using the @SpaceRouting annotation for POJO entries or
        //  via the SpaceTypeDescriptorBuilder for document entries. If the routing property is not explicitly set,
        //  the space id property is used for routing. If the space id property is not defined, the first indexed
        //  property (alphabetically) is used for routing, otherwise the first property (alphabetically) is used for routing.

        if (simpleClasses.contains(spaceObject.getClass())) {
            return spaceObject;
        }

        if (routingMethods.containsKey(spaceObject.getClass())) {
            return invoke(spaceObject, routingMethods.get(spaceObject.getClass()));
        }

        Method spaceRoutingMethod = null;
        Method spaceIdMethod = null;
        final SortedMap<String, Method> indexedProperties = Maps.newTreeMap();
        final SortedMap<String, Method> properties = Maps.newTreeMap();
        for (Method method : spaceObject.getClass().getMethods()) {
            if (isPropertyGetter(method)) {
                for (Annotation annotation : method.getAnnotations()) {
                    if (annotation.annotationType().equals(SpaceRouting.class)) {
                        spaceRoutingMethod = method;
                    } else if (annotation.annotationType().equals(SpaceId.class)) {
                        spaceIdMethod = method;
                    } else if (annotation.annotationType().equals(SpaceIndex.class)) {
                        indexedProperties.put(method.getName().substring(3), method);
                    }
                }

                if (spaceRoutingMethod != null) {
                    break;
                }

                properties.put(method.getName().substring(3), method);
            }
        }

        final Method method = preferredMethod(spaceObject, spaceRoutingMethod, spaceIdMethod, indexedProperties, properties);
        routingMethods.put(spaceObject.getClass(), method);
        return invoke(spaceObject, method);
    }

    private boolean isPropertyGetter(final Method method) {
        return method.getName().startsWith("get")
                && !method.getName().equals("getClass")
                && method.getParameterTypes().length == 0
                && Modifier.isPublic(method.getModifiers());
    }

    private Method preferredMethod(final Object spaceObject,
                                   final Method spaceRoutingMethod,
                                   final Method spaceIdMethod,
                                   final SortedMap<String, Method> indexedProperties,
                                   final SortedMap<String, Method> properties) {
        if (spaceRoutingMethod != null) {
            return spaceRoutingMethod;

        } else if (spaceIdMethod != null) {
            return spaceIdMethod;

        } else if (!indexedProperties.isEmpty()) {
            return indexedProperties.get(indexedProperties.firstKey());

        } else if (!properties.isEmpty()) {
            return properties.get(properties.firstKey());
        }

        throw new IllegalStateException("Couldn't find routing ID in " + spaceObject.getClass().getName());
    }

    private int partitionNumberFor(final Object spaceId) {
        notNull(spaceId, "spaceId may not be null");

        // from http://wiki.gigaspaces.com/wiki/display/XAP9/Routing+In+Partitioned+Spaces
        return (safeABS(spaceId.hashCode()) % partitionCount()) + 1; // SpaceEngine makes this 0 based, and the ID 1 based.
    }

    public int partitionId() {
        if (clusterInfo != null && clusterInfo.getInstanceId() != null) {
            return clusterInfo.getInstanceId();
        }
        return 1;
    }

    public int partitionCount() {
        if (clusterInfo != null && clusterInfo.getNumberOfInstances() != null) {
            return clusterInfo.getNumberOfInstances();
        }
        return 1;
    }

    private Object invoke(final Object spaceObject, final Method method) {
        try {
            return method.invoke(spaceObject);
        } catch (Exception e) {
            throw new RuntimeException("Could not invoke " + spaceObject.getClass().getName()
                    + "." + method.getName());
        }
    }

    private int safeABS(int value) {
        if (value == Integer.MIN_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(value);
    }

    private enum SpaceState {
        PRE_PRIMARY,
        PRIMARY,
        PRE_BACKUP,
        BACKUP
    }
}
