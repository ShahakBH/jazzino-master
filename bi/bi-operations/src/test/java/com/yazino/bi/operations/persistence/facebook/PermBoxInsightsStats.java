package com.yazino.bi.operations.persistence.facebook;

import com.restfb.Facebook;

public class PermBoxInsightsStats {
    @Facebook("permissions_impression_email")
    private Long mailImpressions;

    private Long installations;

    public Long getMailImpressions() {
        return mailImpressions;
    }

    public void setMailImpressions(final Long mailImpressions) {
        this.mailImpressions = mailImpressions;
    }

    public Long getInstallations() {
        return installations;
    }

    public void setInstallations(final Long installations) {
        this.installations = installations;
    }
}
