package com.yazino.engagement.campaign.domain;

import org.joda.time.DateTime;

public class CampaignRun {
    private final Long campaignRunId;
    private final Long campaignId;
    private final DateTime runTimestamp;

    public CampaignRun(final Long campaignRunId, final Long campaignId, final DateTime runTimestamp) {
        this.campaignRunId = campaignRunId;
        this.campaignId = campaignId;
        this.runTimestamp = runTimestamp;
    }

    public Long getCampaignRunId() {
        return campaignRunId;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public DateTime getRunTimestamp() {
        return runTimestamp;
    }
}
