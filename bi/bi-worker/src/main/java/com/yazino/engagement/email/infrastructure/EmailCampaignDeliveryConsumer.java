package com.yazino.engagement.email.infrastructure;

import com.yazino.engagement.EmailCampaignDeliverMessage;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class EmailCampaignDeliveryConsumer implements QueueMessageConsumer<EmailCampaignDeliverMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(EmailCampaignDeliveryConsumer.class);
    private final CampaignCommanderClient campaignCommanderClient;
    private final QueuePublishingService<EmailCampaignDeliverMessage> campaignDeliverMessageQueuePublishingService;
    private final long howLongtoPauseBeforeRedeliveringMessage;


    @Autowired
    public EmailCampaignDeliveryConsumer(CampaignCommanderClient campaignCommanderClient,
                                         @Qualifier("emailCampaignDeliverMessageQueuePublishingService")
                                         QueuePublishingService<EmailCampaignDeliverMessage> emailCampaignDeliverMessageQueuePublishingService,
                                         final Long howLongtoPauseBeforeRedeliveringMessage) {
        this.campaignDeliverMessageQueuePublishingService = emailCampaignDeliverMessageQueuePublishingService;
        this.howLongtoPauseBeforeRedeliveringMessage = howLongtoPauseBeforeRedeliveringMessage;
        Assert.notNull(campaignCommanderClient);
        this.campaignCommanderClient = campaignCommanderClient;
    }

    @Override
    public void handle(final EmailCampaignDeliverMessage message) {
        try {
            final Long emailVisionUploadId = message.getUploadId();
                LOG.info("Checking status of Email upload {}", emailVisionUploadId);
                final EmailVisionUploadStatus uploadStatus = campaignCommanderClient.getUploadStatus(emailVisionUploadId);
                if (uploadStatus.isSuccess()) {
                    campaignCommanderClient.deliverCampaign(message.getCampaignRunId(), message.getTemplateId(), message.getFilter120days());
                } else if (uploadStatus.isPending()) {
                    waitAndRetry(message);
                } else {
                    LOG.error("error uploading upload with Id:{}", emailVisionUploadId);
                }

        } catch (Exception e) {
            LOG.error("problem with handling EmailCampaignDeliverMessage {}", message, e);
        }
    }

    private void waitAndRetry(final EmailCampaignDeliverMessage message) throws InterruptedException {
        LOG.info("pausing for {}ms before retrying", howLongtoPauseBeforeRedeliveringMessage);
        Thread.sleep(howLongtoPauseBeforeRedeliveringMessage);
        campaignDeliverMessageQueuePublishingService.send(message);
    }
    //TODO need to implement expiry?
}
