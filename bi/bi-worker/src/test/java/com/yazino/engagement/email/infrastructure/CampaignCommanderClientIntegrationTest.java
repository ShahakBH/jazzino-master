package com.yazino.engagement.email.infrastructure;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.EmailCampaignDeliverMessage;
import com.yazino.engagement.EmailTarget;
import com.yazino.engagement.campaign.application.CampaignContentService;
import com.yazino.engagement.campaign.application.EmailCampaignUploaderAdapter;
import com.yazino.engagement.campaign.dao.CampaignNotificationDao;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignCommanderClientIntegrationTest {
    public static final String TEMPLATE_ID = "987655";
    public static final String FILTER_120_DAYS = "ON";
    private static final int LAPSED_4D = 78463;
    private static final int LAPSED_7D = 74607;
    private static final int LAPSED_14D = 74572;
    private static final int LAPSED_28D = 74648;
    private static final int BUYER_4D = 74653;
    private static final int BUYER_7D = 74681;
    private static final int BUYER_14D = 74689;
    private static final int BUYER_28D = 74701;

    // DO NOT run this in a test suite. it uploads real data (with my name on it) to emailvision annd pounds away at their API.
    // DO use this to troubleshoot and build new shiz. the end to end below dumps a message on the queue and mocks out the DAOs
    // so it can upload the real data.

    @Autowired
    CampaignCommanderClient client;

    @Autowired
    EmailCampaignDeliveryConsumer consumer;

    @Mock
    private CampaignNotificationDao campaignNotificationDao;

    @Mock
    private CampaignContentService campaignContentService;

    @Mock
    QueuePublishingService<EmailCampaignDeliverMessage> campaignDeliverMessageQueuePublishingService;

    EmailCampaignUploaderAdapter emailCampaignUploaderAdapter;
    @Autowired
    private YazinoConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        emailCampaignUploaderAdapter = new EmailCampaignUploaderAdapter(campaignNotificationDao,
                client,
                campaignContentService,
                campaignDeliverMessageQueuePublishingService, configuration);
    }

    @Test
    @Ignore

    public void addEmailAddressesShouldUploadEmails() throws IOException, InterruptedException {

        final Long uploadId = client.addEmailAddresses(newArrayList(new EmailTarget("hchahine@yazino.com", "YazinoMan", null)),
                99L,
                "Campaign Title");
        System.out.println("uploaded " + uploadId);
        while (client.getUploadStatus(uploadId) != EmailVisionUploadStatus.DONE) {
            System.out.println("sleepy");
            Thread.sleep(10000);
        }
        System.out.println("processed " + uploadId);
        client.deliverCampaign(uploadId, TEMPLATE_ID, FILTER_120_DAYS);
        fail();
    }

    @Test
    @Ignore
    public void consumerShouldHandleMessage() throws InterruptedException {
        when(campaignNotificationDao.getEligibleEmailTargets(123L)).thenReturn(newArrayList(new EmailTarget("jrae@yazino.com", "Jae Rae",
                null)));
        when(campaignContentService.getEmailListName(123L)).thenReturn("JAE_RAE_TEST_MERGE");

        emailCampaignUploaderAdapter.sendMessageToPlayers(new CampaignDeliverMessage(123L, ChannelType.EMAIL));
        Thread.sleep(100000L);
        verify(campaignDeliverMessageQueuePublishingService).send(any(EmailCampaignDeliverMessage.class));
        fail();
    }

    @Test
    @Ignore
    public void getStatusShould() {
        System.out.println(client.getUploadStatus(27021L));
        fail();
    }

    @Test
    @Ignore
    public void createAListAndSegmentCloneAmessageCreateACampaignAndPostACampaignShouldAllWork() throws InterruptedException {
        configuration.addProperty("emailvision.campaign.postcampaign", Boolean.TRUE);
        List<EmailTarget> emailTargets = newArrayList(
                new EmailTarget("hchahine@yazino.com", "Big Boss Man", null),
                new EmailTarget("dseeto+openEmail@yazino.com", "dseeto", null),
                new EmailTarget("acrankshaw@yazino.com", "emailVision automated test", null),
                new EmailTarget("mtan@yazino.com", "Mel do you get this", null),
                new EmailTarget("aelahmar@yazino.com", "ali do you get this", null));


        Long campaignRunId = 151085L;
        Long uploadId = client.addEmailAddresses(emailTargets, campaignRunId, "test why no mel");


        while (client.getUploadStatus(uploadId) != EmailVisionUploadStatus.DONE) {
            System.out.println("sleepy");
            Thread.sleep(10000);
        }

        String templateId = "56799";
        assertTrue(client.deliverCampaign(campaignRunId, templateId, "ON"));
    }

    @Test
    @Ignore
    public void sendEmailWithContent() throws InterruptedException {
        configuration.addProperty("emailvision.campaign.postcampaign", Boolean.TRUE);

        final Map<String, Object> content = newHashMap();
        content.put("PROVIDER_NAME", "FACEBOOK");
        content.put("BALANCE", "666666");
        content.put("DISPLAY_NAME", "BOBBY FB");

        final Map<String, Object> nonfbcontent = newHashMap();
        nonfbcontent.put("PROVIDER_NAME", "YAZINO");
        nonfbcontent.put("BALANCE", "t333");
        nonfbcontent.put("DISPLAY_NAME", "JIMMY NON");

        List<EmailTarget> emailTargets = newArrayList(
                new EmailTarget("aelahmar+nonfb@yazino.com", "non facebook", nonfbcontent),
                new EmailTarget("aelahmar+fb@yazino.com", "facebook", content)
        );

        Long campaignRunId = 151090L;


        Long uploadId = client.addEmailAddresses(emailTargets, campaignRunId, "test facebook url switching");


        EmailVisionUploadStatus uploadStatus;
        while ((uploadStatus = client.getUploadStatus(uploadId)) != EmailVisionUploadStatus.DONE) {
            if (uploadStatus == EmailVisionUploadStatus.FAILURE || uploadStatus == EmailVisionUploadStatus.ERROR) {
                fail();
                break;
            }
            System.out.println("sleepy");
            Thread.sleep(10000);
        }

        Integer[] emailTemplates = new Integer[]{
                LAPSED_4D,
                LAPSED_7D,
//                LAPSED_14D,
//                LAPSED_28D,
                BUYER_4D,
                BUYER_7D
//                BUYER_14D,
//                BUYER_28D
        };

        for (Integer emailTemplate : emailTemplates) {

            assertTrue(client.deliverCampaign(campaignRunId, emailTemplate.toString(), "OFF"));
        }
    }

    @Test
    @Ignore
    public void Should(){
        configuration.addProperty("emailvision.campaign.postcampaign", Boolean.TRUE);
        Integer[] emailTemplates = new Integer[]{
//                LAPSED_4D,
//                LAPSED_7D,
//                LAPSED_14D,
//                LAPSED_28D,
//                BUYER_4D,
//                BUYER_7D
//                BUYER_14D,
//                BUYER_28D
                82356,
                82339,
                82295
                };

        for (Integer emailTemplate : emailTemplates) {

            assertTrue(client.deliverCampaign(151090l, emailTemplate.toString(), "OFF"));
        }

    }

    @Test
    @Ignore
    public void createAListAndSegmentCloneAmessageCreateACampaignAndPostACampaignShouldWorkWithNoFilter() throws InterruptedException {
        configuration.addProperty("emailvision.campaign.postcampaign", Boolean.TRUE);
        List<EmailTarget> emailTargets = newArrayList(
                new EmailTarget("dseeto+openEmail@yazino.com", "dseeto", null),
                new EmailTarget("acrankshaw@yazino.com", "emailVision automated test", null),
                new EmailTarget("mtan@yazino.com", "Mel do you get this", null),
                new EmailTarget("aelahmar@yazino.com", "ali do you get this", null));


        Long campaignRunId = 151085L;
        Long uploadId = client.addEmailAddresses(emailTargets, campaignRunId, "test why no mel");


        while (client.getUploadStatus(uploadId) != EmailVisionUploadStatus.DONE) {
            System.out.println("sleepy");
            Thread.sleep(10000);
        }

        String templateId = "56799";
        assertTrue(client.deliverCampaign(campaignRunId, templateId, null));
    }

    @Test
    @Ignore
    public void testDoneWithErrors() throws InterruptedException {
        configuration.addProperty("emailvision.campaign.postcampaign", Boolean.TRUE);
        List<EmailTarget> emailTargets = newArrayList(
                new EmailTarget("15039331768", "Francisco Javier Canche Kantun", null),
                new EmailTarget("447852688081", "Samantha Forrest", null));


        Long campaignRunId = 15108599L;
        Long uploadId = client.addEmailAddresses(emailTargets, campaignRunId, "test done with errors failure");


        while (!client.getUploadStatus(uploadId).isSuccess()) {
            System.out.println("sleepy");
            Thread.sleep(10000);
        }

        String templateId = "56799";
        assertTrue(client.deliverCampaign(campaignRunId, templateId, null));
    }
}
