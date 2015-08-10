package com.yazino.bi.operations.engagement;

import com.yazino.android.GoogleCloudMessagingConstants;
import com.yazino.engagement.ChannelType;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;

import static org.junit.Assert.assertTrue;

public class EngagementCampaignValidatorTest {

    private EngagementCampaignValidator underTest = new EngagementCampaignValidator();
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String MESSAGE = "message";
    private static final DateTime SCHEDULED = new DateTime("2012-12-30T14:59:00Z");
    private static final DateTime EXPIRES = new DateTime("2012-12-31T14:59:00Z");

    @Test
    public void shouldRejectCampaignThatExpiresWhenScheduled() {
        Errors errors = new MapBindingResult(new HashMap(), "campaign");
        EngagementCampaign campaign = validEngagementCampaign(ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID);
        campaign.setExpires(campaign.getScheduled());
        underTest.validate(campaign, errors);
        assertTrue(errors.hasFieldErrors("expires") && errors.getFieldError("expires").getDefaultMessage().startsWith("Expiry date must be after"));
    }

    @Test
    public void shouldRejectCampaignThatExpiresBeforeScheduled() {
        Errors errors = new MapBindingResult(new HashMap(), "campaign");
        EngagementCampaign campaign = validEngagementCampaign(ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID);
        campaign.setExpires(campaign.getScheduled().minusSeconds(1));
        underTest.validate(campaign, errors);
        assertTrue(errors.hasFieldErrors("expires") && errors.getFieldError("expires").getDefaultMessage().startsWith("Expiry date must be after"));
    }

    @Test
    public void shouldRejectAndroidCampaignThatExpiresMoreThan4WeeksAfterScheduled() {
        Errors errors = new MapBindingResult(new HashMap(), "campaign");
        EngagementCampaign campaign = validEngagementCampaign(ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID);
        campaign.setExpires(campaign.getScheduled().plusSeconds(GoogleCloudMessagingConstants.MAX_SECONDS_TO_LIVE + 1));
        underTest.validate(campaign, errors);
        assertTrue(errors.hasFieldErrors("expires") && errors.getFieldError("expires").getDefaultMessage().startsWith("Expiry date must be no more"));
    }

    private EngagementCampaign validEngagementCampaign(ChannelType channelType) {
        EngagementCampaign campaign = new EngagementCampaign();
        campaign.setChannelType(channelType);
        campaign.setTitle(TITLE);
        campaign.setDescription(DESCRIPTION);
        campaign.setMessage(MESSAGE);
        campaign.setScheduled(SCHEDULED);
        campaign.setExpires(EXPIRES);
        return campaign;
    }


}
