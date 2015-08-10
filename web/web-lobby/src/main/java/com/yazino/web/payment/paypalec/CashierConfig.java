package com.yazino.web.payment.paypalec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("paypalEcCashierConfig")
public class CashierConfig {
    private String assetUrl;
    private String paypalApiEnvironment;

    @Value("${strata.server.lobby.ssl.content}")
    public void setAssetUrl(final String assetUrl) {
        this.assetUrl = assetUrl;
    }

    @Value("${paypal.api.environment}")
    public void setPaypalApiEnvironment(final String paypalApiEnvironment) {
        this.paypalApiEnvironment = paypalApiEnvironment;
    }

    public String getPaypalApiEnvironment() {
        return paypalApiEnvironment;
    }

    public String getAssetUrl() {
        return assetUrl;
    }
}
