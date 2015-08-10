package com.yazino.mobile.ws.config;

import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.Map;

public class TapjoyConfig {

    private Map<String, String> mEarnChipsURLs = Collections.emptyMap();

    public Map<String, String> getEarnChipsURLs() {
        return mEarnChipsURLs;
    }

    public void setEarnChipsURLs(Map<String, String> earnChipsURLs) {
        Validate.notNull(earnChipsURLs);
        mEarnChipsURLs = earnChipsURLs;
    }
}
