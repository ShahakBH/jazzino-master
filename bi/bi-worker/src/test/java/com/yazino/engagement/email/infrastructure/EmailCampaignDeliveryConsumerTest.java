package com.yazino.engagement.email.infrastructure;

import com.yazino.engagement.EmailCampaignDeliverMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailCampaignDeliveryConsumerTest {
    public static final long CAMPAIGN_RUN_ID = 987L;
    public static final String TEMPLATE_ID = "123456";
    public static final String FILTER_120_DAYS = "ON";
    @Mock
    private CampaignCommanderClient campaignDeliveryClient;
    private EmailCampaignDeliveryConsumer underTest;
    @Mock private QueuePublishingService<EmailCampaignDeliverMessage> queue;

    @Before
    public void setUp() throws Exception {
        underTest = new EmailCampaignDeliveryConsumer(campaignDeliveryClient,queue, 1L);
    }

    @Test
    public void handleShouldCheckStatus(){
        underTest.handle(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
        verify(campaignDeliveryClient).getUploadStatus(123L);
    }

    @Test
    public void handleShouldDeliverCampaignWhenReady(){
        when(campaignDeliveryClient.getUploadStatus(123L)).thenReturn(EmailVisionUploadStatus.DONE);
        underTest.handle(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
        verify(campaignDeliveryClient).getUploadStatus(123L);
    }

    @Test
    public void handleShouldReQueueMessageIfStillImporting(){
        when(campaignDeliveryClient.getUploadStatus(123L)).thenReturn(EmailVisionUploadStatus.IMPORTING);
        underTest.handle(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
        verify(campaignDeliveryClient).getUploadStatus(123L);
        verifyNoMoreInteractions(campaignDeliveryClient);
        verify(queue).send(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
    }

    @Test
    public void handleShouldReQueueMessageIfStillQueued(){
        when(campaignDeliveryClient.getUploadStatus(123L)).thenReturn(EmailVisionUploadStatus.QUEUED);
        underTest.handle(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
        verify(campaignDeliveryClient).getUploadStatus(123L);
        verifyNoMoreInteractions(campaignDeliveryClient);
        verify(queue).send(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
    }

    @Test
    public void handleShouldReQueueMessageIfJustStored(){
        when(campaignDeliveryClient.getUploadStatus(123L)).thenReturn(EmailVisionUploadStatus.STORAGE);
        underTest.handle(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
        verify(campaignDeliveryClient).getUploadStatus(123L);
        verifyNoMoreInteractions(campaignDeliveryClient);
        verify(queue).send(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
    }

    @Test
    public void handleShouldReQueueMessageIfJustValidated(){
        when(campaignDeliveryClient.getUploadStatus(123L)).thenReturn(EmailVisionUploadStatus.VALIDATED);
        underTest.handle(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
        verify(campaignDeliveryClient).getUploadStatus(123L);
        verifyNoMoreInteractions(campaignDeliveryClient);
        verify(queue).send(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
    }

    @Test
    public void handleShouldNotRequeueMessageIfError(){
        when(campaignDeliveryClient.getUploadStatus(123L)).thenReturn(EmailVisionUploadStatus.ERROR);
        underTest.handle(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
        verify(campaignDeliveryClient).getUploadStatus(123L);
        verifyNoMoreInteractions(campaignDeliveryClient);
        verifyNoMoreInteractions(queue);
    }

    @Test
    public void handleShouldNotRequeueMessageIfSuccess(){
        when(campaignDeliveryClient.getUploadStatus(123L)).thenReturn(EmailVisionUploadStatus.DONE);
        underTest.handle(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
        verify(campaignDeliveryClient).getUploadStatus(123L);
        verify(campaignDeliveryClient).deliverCampaign(CAMPAIGN_RUN_ID, TEMPLATE_ID, FILTER_120_DAYS);
        verifyNoMoreInteractions(campaignDeliveryClient);
        verifyNoMoreInteractions(queue);
    }

    @Test
    public void handleShouldNotRequeueMessageIfFail(){
        when(campaignDeliveryClient.getUploadStatus(123L)).thenReturn(EmailVisionUploadStatus.FAILURE);
        underTest.handle(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 123L, TEMPLATE_ID, FILTER_120_DAYS));
        verify(campaignDeliveryClient).getUploadStatus(123L);
        verifyNoMoreInteractions(campaignDeliveryClient);
        verifyNoMoreInteractions(queue);
    }

}
