package com.yazino.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Partner {
    YAZINO, TANGO;
    private static final Logger LOG = LoggerFactory.getLogger(Partner.class);
    public static Partner parse(String partnerId){
        try {
            return valueOf(partnerId);
        } catch (Exception e) {
            LOG.warn("Invalid partnerId: {}",partnerId);
            return YAZINO;
        }
    }
}
