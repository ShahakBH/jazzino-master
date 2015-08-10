package com.yazino.web.domain;

import com.yazino.platform.Partner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("siteConfiguration")
public class SiteConfiguration {
    private String hostUrl;
    private Partner partnerId;
    private String defaultGameType = "BLACKJACK";
    private String assetUrl;

    public String getAssetUrl() {
        return assetUrl;
    }

    @Value("${senet.web.content}")
    public void setAssetUrl(final String assetUrl) {
        this.assetUrl = assetUrl;
    }

    public Partner getPartnerId() {
        return partnerId;
    }

    @Value("${strata.lobby.partnerid}")
    public void setPartnerId(final Partner partnerId) {
        this.partnerId = partnerId;
    }

    public String getDefaultGameType() {
        return defaultGameType;
    }

    public void setDefaultGameType(final String defaultGameType) {
        this.defaultGameType = defaultGameType;
    }

    public String getHostUrl() {
        return hostUrl;
    }

    @Value("${senet.web.host}")
    public void setHostUrl(final String hostUrl) {
        this.hostUrl = hostUrl;
    }
}
