package com.yazino.bi.opengraph;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruMap<T, K> extends LinkedHashMap<T, K> {
    private static final long serialVersionUID = -1178148586296550490L;

    private static final boolean REMOVE_BY_LAST_ACCESSED_ORDER = true;
    private final int maxCapacity;

    public LruMap(final int maxCapacity) {
        super(maxCapacity, 0.75f, REMOVE_BY_LAST_ACCESSED_ORDER);
        this.maxCapacity = maxCapacity;
    }

    @Override
    protected boolean removeEldestEntry(final Map.Entry eldest) {
        return size() > maxCapacity;
    }
}
