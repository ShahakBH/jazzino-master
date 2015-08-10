package com.yazino.web.security;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class ProtectedResourceClassifier {

    private final Set<Domain> publicDomains;

    public ProtectedResourceClassifier(Set<Domain> publicDomains) {
        checkNotNull(publicDomains);
        this.publicDomains = new HashSet<Domain>(publicDomains);
    }

    public boolean requiresAuthorisation(String url) {
        for (Domain domain : publicDomains) {
            if (domain.includesUrl(url)) {
                return false;
            }
        }
        return true;
    }

}
