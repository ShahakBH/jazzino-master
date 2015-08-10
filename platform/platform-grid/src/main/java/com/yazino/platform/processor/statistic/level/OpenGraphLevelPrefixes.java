package com.yazino.platform.processor.statistic.level;

import java.util.Map;

import static org.apache.commons.lang.Validate.notNull;

public class OpenGraphLevelPrefixes {

    private Map<String, String> resourceMap;

    public OpenGraphLevelPrefixes(final Map<String, String> openGraphLevelPrefixMap) {
        notNull(openGraphLevelPrefixMap, "openGraphLevelPrefixMap is null");
        this.resourceMap = openGraphLevelPrefixMap;
    }

    public String getLevelPrefix(final String objectId) {
        return resourceMap.get(objectId);
    }
}
