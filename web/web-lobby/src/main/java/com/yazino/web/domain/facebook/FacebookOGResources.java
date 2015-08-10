package com.yazino.web.domain.facebook;

import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class FacebookOGResources {
    private Map<String, FacebookOGResource> resourceMap;

    public FacebookOGResources(final Map<String, FacebookOGResource> fbObjectResources) {
        notNull(fbObjectResources, "fbObjectResources is null");
        this.resourceMap = fbObjectResources;
    }

    public String getTitle(final String objectId) {

        final FacebookOGResource resource = resourceMap.get(objectId);
        if (resource != null) {
            return resource.getTitle();
        }
        return "";
    }

    public String getArticle(final String objectId) {
        final FacebookOGResource resource = resourceMap.get(objectId);
        if (resource != null) {
            return resource.getArticle();
        }
        return "";
    }

    public String getDescription(final String objectId) {
        final FacebookOGResource resource = resourceMap.get(objectId);
        if (resource != null) {
            return resource.getDescription();
        }
        return "";
    }
}
