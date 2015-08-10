package com.yazino.web.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ViewAspects implements Serializable {
    private static final long serialVersionUID = -7197658800904512280L;
    private final Map<String, ViewAspectConfiguration> viewAspectConfiguration;
    private final String gameType;
    private final String partnerId;

    public ViewAspects(final Collection<ViewAspectConfiguration> configurations,
                       final String gameType,
                       final String partnerId) {
        this.viewAspectConfiguration = new HashMap<String, ViewAspectConfiguration>();
        for (ViewAspectConfiguration configuration : configurations) {
            viewAspectConfiguration.put(configuration.getAspectName(), configuration);
        }
        this.gameType = gameType;
        this.partnerId = partnerId;
    }

    public boolean supports(final String aspect) {
        return supports(aspect, gameType);
    }

    public boolean supports(final String queryAspect,
                            final String queryGameType) {
        final boolean aspectExists = viewAspectConfiguration.containsKey(queryAspect);
        if (aspectExists) {
            final ViewAspectConfiguration aspectDefinition = viewAspectConfiguration.get(queryAspect);
            final boolean supportedPartner = aspectDefinition.getRequiredPartner() == null
                    || partnerId.equals(aspectDefinition.getRequiredPartner());
            final boolean supportedGameType = aspectSupported(queryGameType, aspectDefinition.getRequiredGameTypes());
            return supportedPartner && supportedGameType;
        }
        return true;
    }

    private boolean aspectSupported(final String match, final Collection<String> requirements) {
        return requirements.size() == 0 || requirements.contains(match);
    }

    public String linkFor(final String aspect, final String linkGameType) {
        return viewAspectConfiguration.get(aspect).getLinkFor(linkGameType);
    }
}
