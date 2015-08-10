package com.yazino.engagement.campaign;

import com.yazino.engagement.campaign.consumers.CampaignRunConsumer;
import com.yazino.engagement.campaign.domain.CampaignRunMessage;

import java.util.concurrent.atomic.AtomicInteger;

public class TestRunCampaignRequestConsumer extends CampaignRunConsumer {

    private volatile CampaignRunMessage campaignRunMessage;

    private AtomicInteger countMessages = new AtomicInteger(0);

    public TestRunCampaignRequestConsumer() {
        super(null);
    }

    @Override
    public void handle(final CampaignRunMessage message) {
        this.campaignRunMessage = message;
        countMessages.getAndIncrement();
    }

    public CampaignRunMessage getCampaignRunMessage() {
        return campaignRunMessage;
    }

    public AtomicInteger getCountMessages() {
        return countMessages;
    }
}