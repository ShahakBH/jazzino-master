package com.yazino.web.util;

import org.springframework.stereotype.Service;

@Service
public class Environment {

    private final boolean development;

    public Environment() {
        final String developmentMode = System.getProperty("strata.development");
        development = developmentMode != null && "true".equalsIgnoreCase(developmentMode.trim());
    }

    public boolean isDevelopment() {
        return development;
    }

}
