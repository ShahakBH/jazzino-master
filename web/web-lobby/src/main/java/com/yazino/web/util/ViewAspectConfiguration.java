package com.yazino.web.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ViewAspectConfiguration {
    private final String aspectName;
    private Collection<String> requiredGameTypes = new HashSet<String>();
    private String requiredPartner;

    private Map<String, String> linksFor = new HashMap<String, String>();

    public Map<String, String> getLinksFor() {
        return linksFor;
    }

    public void setLinksFor(final Map<String, String> linksFor) {
        this.linksFor = linksFor;
    }


    public ViewAspectConfiguration(final String aspectName) {
        this.aspectName = aspectName;
    }

    public void setRequiredGameTypes(final Collection<String> requiredGameTypes) {
        this.requiredGameTypes = requiredGameTypes;
    }

    public void setRequiredPartner(final String requiredPartner) {
        this.requiredPartner = requiredPartner;
    }

    public String getAspectName() {
        return aspectName;
    }

    public Collection<String> getRequiredGameTypes() {
        return requiredGameTypes;
    }

    public String getRequiredPartner() {
        return requiredPartner;
    }

    public String getLinkFor(final String gameType) {
        return linksFor.get(gameType);
    }
}
