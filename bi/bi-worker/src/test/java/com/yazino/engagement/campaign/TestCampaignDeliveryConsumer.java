package com.yazino.engagement.campaign;

import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.campaign.application.ChannelCampaignDeliveryAdapter;
import com.yazino.engagement.campaign.consumers.CampaignDeliveryConsumer;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class TestCampaignDeliveryConsumer extends CampaignDeliveryConsumer {

    private volatile CampaignDeliverMessage campaignDeliverMessage;

    private AtomicInteger countMessages = new AtomicInteger(0);

    public TestCampaignDeliveryConsumer() {
        super(new ArrayList<ChannelCampaignDeliveryAdapter>());
    }

    @Override
    public void handle(final CampaignDeliverMessage message) {
        this.campaignDeliverMessage = message;
        countMessages.getAndIncrement();

    }

    public AtomicInteger getCountMessages() {
        return countMessages;
    }

    public CampaignDeliverMessage getCampaignDeliverMessage() {
        return campaignDeliverMessage;
    }
}
