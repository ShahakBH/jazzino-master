package com.yazino.bi.operations.campaigns;

import com.yazino.bi.operations.campaigns.controller.Campaign;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.domain.MessageContentType;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.promotions.BuyChipsForm;
import com.yazino.promotions.DailyAwardForm;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class CampaignFormValidatorTest {

    public static final String CAMPAIGN_FORM_BINDING = "campaignForm";
    private DateTime DATE_TIME = new DateTime(2013, 6, 28, 11, 22);

    private CampaignFormValidator underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new CampaignFormValidator();
    }

    @Test
    public void campaignValidatorShouldAddEntryToBindingResultIfRunHoursIsLessThanZero() {
        Campaign campaign = createCampaign();
        campaign.getCampaignScheduleWithName().setRunHours(-1L);
        CampaignForm campaignForm = new CampaignForm(campaign, null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertThat(bindingResult.getFieldError("campaign.campaignScheduleWithName.runHours").getDefaultMessage(),
                is("Run Hours cannot be less than zero"));

    }

    @Test
    public void campaignValidatorShouldRejectNullCampaign() {
        CampaignForm campaignForm = new CampaignForm(null, new BuyChipsForm());
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertThat(bindingResult.getFieldError("campaign").getDefaultMessage(), is("Campaign is Empty"));
    }

    @Test
    public void campaignValidatorShouldRejectIfChannelEmailAndNullTemplate() {
        Campaign campaign = createCampaign();
        campaign.setChannels(newArrayList(ChannelType.EMAIL));
        campaign.getCampaignScheduleWithName().setRunHours(-1L);
        CampaignForm campaignForm = new CampaignForm(campaign, null);


        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertThat(bindingResult.getFieldError("campaign.channelConfig[TEMPLATE]").getDefaultMessage(), is("Email Template can not be Empty"));
    }

    @Test
    public void campaignValidatorShouldRejectNullCampaignValues() {
        CampaignForm campaignForm = new CampaignForm(new Campaign(), null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertThat(bindingResult.getFieldError("campaign.campaignScheduleWithName.campaignId").getDefaultMessage(),
                is("CampaignId is Empty"));
        assertThat(bindingResult.getFieldError("campaign.campaignScheduleWithName.nextRunTsAsDate").getDefaultMessage(),
                is("Next Run Time is Empty"));
        assertThat(
                bindingResult.getFieldError("campaign.campaignScheduleWithName.runHours").getDefaultMessage(),
                is("Run Hours is Empty"));
        assertThat(bindingResult.getFieldError("campaign.campaignScheduleWithName.endTimeAsDate").getDefaultMessage(),
                is("End Time is Empty"));
        assertThat(bindingResult.getFieldError("campaign.campaignScheduleWithName.name").getDefaultMessage(), is("Name is Empty"));
        assertThat(bindingResult.getFieldError("campaign.sqlQuery").getDefaultMessage(), is("Sql Query is Empty"));
        assertThat(bindingResult.getFieldError("campaign.content").getDefaultMessage(), is("Content is Empty"));
        assertThat(bindingResult.getFieldError("campaign.channels").getDefaultMessage(), is("Tick at least one notification Channel"));
    }

    @Test
    public void campaignValidatorShouldRejectInvalidSqlCampaignValues() {
        CampaignForm campaignForm = new CampaignForm(new Campaign(), null);
        campaignForm.getCampaign().setSqlQuery("invalid sql");
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertThat(bindingResult.getFieldError("campaign.sqlQuery").getDefaultMessage(),
                is("select must start with select and not have naughty sql in it"));
    }

    private Campaign createCampaign() {
        Map<String, String> contentMap = newLinkedHashMap();
        contentMap.put(MessageContentType.DESCRIPTION.getKey(), "Yazino description");
        contentMap.put(MessageContentType.MESSAGE.getKey(), "Hooray !! Yazino Message");
        contentMap.put(MessageContentType.TRACKING.getKey(), "TRACKED_DATA");

        List<ChannelType> channels = asList(ChannelType.values());

        final HashMap<NotificationChannelConfigType, String> channelConfig = new HashMap<NotificationChannelConfigType, String>();
        channelConfig.put(NotificationChannelConfigType.GAME_TYPE_FILTER, "TEXAS_HOLDEM");
        return new Campaign(100L,
                "Own Campaign",
                DATE_TIME,
                DATE_TIME.plusDays(1),
                2L,
                0l, "select One as 1",
                contentMap,
                channels,
                Boolean.FALSE,
                channelConfig,
                false);
    }
    @Test
    public void campValidatorShouldDisallowShortRerunDurationDelayedNotificationCamps(){
        final Campaign campaign = createCampaign();
        campaign.getCampaignScheduleWithName().setRunHours(23L);
        campaign.setDelayNotifications(true);
        CampaignForm campaignForm = new CampaignForm(campaign, null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);
        assertThat(bindingResult.getFieldError("campaign.campaignScheduleWithName.runHours").getDefaultMessage(),
                   is("cannot have delayed notification campaign which runs <24 hours"));


    }
    @Test
    public void campaignValidatorShouldNotCheckForBuyChipsOnlyIfPromoNotEnabledInCampaign() {
        CampaignForm campaignForm = new CampaignForm(new Campaign(), null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertNull(bindingResult.getFieldError("buyChipsForm"));
    }

    @Test
    public void campaignValidatorShouldRejectNullBuyChipsFormValues() {
        BuyChipsForm buyChipsForm = new BuyChipsForm();
        buyChipsForm.setValidForHours(null);
        buyChipsForm.setMaxRewards(null);
        buyChipsForm.setPlatforms(null);
        buyChipsForm.setPriority(null);
        buyChipsForm.setPaymentMethods(null);
        buyChipsForm.setChipsPackagePercentages(null);
        Campaign campaign = new Campaign();
        campaign.setPromo(true);
        CampaignForm campaignForm = new CampaignForm(campaign, buyChipsForm);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);

        underTest.validate(campaignForm, bindingResult);

        assertThat(bindingResult.getFieldError("buyChipsForm.priority").getDefaultMessage(), is("Priority is Empty"));
        assertThat(bindingResult.getFieldError("buyChipsForm.validForHours").getDefaultMessage(), is("Valid For field is Empty"));
        assertThat(bindingResult.getFieldError("buyChipsForm.platforms").getDefaultMessage(), is("Select at least one platform"));
        assertThat(bindingResult.getFieldError("buyChipsForm.maxRewards").getDefaultMessage(), is("Up to field is Empty"));
        assertThat(bindingResult.getFieldError("buyChipsForm.inGameNotificationHeader").getDefaultMessage(),
                is("On site Message Header is Empty"));
        assertThat(bindingResult.getFieldError("buyChipsForm.inGameNotificationMsg").getDefaultMessage(),
                is("Notification Message is Empty"));
        assertThat(bindingResult.getFieldError("buyChipsForm.rolloverHeader").getDefaultMessage(), is("Buy Chips Header is Empty"));
        assertThat(bindingResult.getFieldError("buyChipsForm.rolloverText").getDefaultMessage(), is("Buy Chips Message is Empty"));
        assertThat(bindingResult.getFieldError("buyChipsForm.chipsPackagePercentages").getDefaultMessage(),
                is("Chips Package Percentages is empty. Something has gone horribly wrong"));
    }

    @Test
    public void campaignValidatorShouldRejectNullDailyAwardFormValues() {
        DailyAwardForm dailyAwardForm = new DailyAwardForm();
        dailyAwardForm.setTopUpAmount(null);
        dailyAwardForm.setPriority(null);
        dailyAwardForm.setValidForHours(null);
        dailyAwardForm.setPlatforms(null);
        dailyAwardForm.setMaxRewards(null);
        Campaign campaign = new Campaign();
        campaign.setPromo(true);
        CampaignForm campaignForm = new CampaignForm(campaign, dailyAwardForm);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);

        underTest.validate(campaignForm, bindingResult);

        assertThat(bindingResult.getFieldError("dailyAwardForm.topUpAmount").getDefaultMessage(), is("topUpAmount is Empty"));
        assertThat(bindingResult.getFieldError("dailyAwardForm.priority").getDefaultMessage(), is("Priority is Empty"));
        assertThat(bindingResult.getFieldError("dailyAwardForm.validForHours").getDefaultMessage(), is("Valid For field is Empty"));
        assertThat(bindingResult.getFieldError("dailyAwardForm.platforms").getDefaultMessage(), is("Select at least one platform"));
        assertThat(bindingResult.getFieldError("dailyAwardForm.maxRewards").getDefaultMessage(), is("Up to field is Empty"));

    }

    @Test
    public void validateShouldAddEntryToBindingResultIfValidHoursIsZero() {
        BuyChipsForm buyChipsForm = new BuyChipsForm();
        buyChipsForm.setValidForHours(0);
        Campaign campaign = new Campaign();
        campaign.setPromo(true);
        CampaignForm campaignForm = new CampaignForm(campaign, buyChipsForm);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);
        assertThat(bindingResult.getFieldError("buyChipsForm.validForHours").getDefaultMessage(),
                is("Valid For field should be greater than zero"));

    }

    @Test
    public void validatorShouldIgnoreChannelListIfItIsAPromo() {
        Campaign campaign = new Campaign();
        campaign.setPromo(true);
        CampaignForm campaignForm = new CampaignForm(campaign, null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertNull(bindingResult.getFieldError("campaign.channels"));

    }

    @Test
    public void validatorShouldWarnIfNoGameTypesSpecified() {
        Campaign campaign = new Campaign();
        final Map<NotificationChannelConfigType, String> channelConfig=newHashMap();
        campaign.setChannelConfig(channelConfig);

        CampaignForm campaignForm = new CampaignForm(campaign, null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertThat(bindingResult.getFieldError("campaign.gameTypes").getDefaultMessage(),is(equalTo("Tick at least one game type")));
    }

    @Test
    public void validatorShouldNotWarnIfGameTypeIsSpecified() {
        Campaign campaign = new Campaign();
        final Map<NotificationChannelConfigType, String> channelConfig=newHashMap();
        channelConfig.put(NotificationChannelConfigType.GAME_TYPE_FILTER, "GAME");
        campaign.setChannelConfig(channelConfig);

        CampaignForm campaignForm = new CampaignForm(campaign, null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertNull(bindingResult.getFieldError("campaign.gameTypes"));
    }

    @Test
    public void validationChecksThatCustomContentTagsThatAppearInContentDontAppearInSql() {
        final Campaign campaign = new Campaign();
        Map<String, String> contentMap = newLinkedHashMap();

        contentMap.put(MessageContentType.MESSAGE.getKey(), "sql should contain {THIS} maybe {THAT}");
        contentMap.put(MessageContentType.DESCRIPTION.getKey(), "sql should contain {ANOTHER}");
        campaign.setContent(contentMap);

        campaign.setPromo(false);
        final CampaignForm campaignForm = new CampaignForm(campaign, null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertThat(bindingResult.getFieldError("campaign.content[description]").getDefaultMessage(),
                is("SQL does not contain column matching content tags:ANOTHER"));
        assertThat(bindingResult.getFieldError("campaign.content[message]").getDefaultMessage(),
                is("SQL does not contain column matching content tags:THIS, THAT"));
    }

    @Test
    public void validationChecksThatProgressiveTagsThatAppearInContentDontHaveToAppearInSql() {
        final Campaign campaign = new Campaign();
        Map<String, String> contentMap = newLinkedHashMap();

        contentMap.put(MessageContentType.MESSAGE.getKey(), "sql should contain {PROGRESSIVE} maybe ");

        campaign.setContent(contentMap);

        campaign.setPromo(false);
        final CampaignForm campaignForm = new CampaignForm(campaign, null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertThat(bindingResult.getFieldError("campaign.content[message]"),
                is(nullValue()));
    }

    @Test
    public void validationShouldMatchLowerCaseSql(){
        final Campaign campaign = new Campaign();
        Map<String, String> contentMap = newLinkedHashMap();

        contentMap.put(MessageContentType.MESSAGE.getKey(), "sql should contain {THIS} ");
        contentMap.put(MessageContentType.DESCRIPTION.getKey(), "sql should contain {THAT}");
        campaign.setContent(contentMap);

        campaign.setSqlQuery("select this, THAT from whatever");

        campaign.setPromo(false);
        final CampaignForm campaignForm = new CampaignForm(campaign, null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertNull(bindingResult.getFieldError("campaign.content[description]"));
        assertNull(bindingResult.getFieldError("campaign.content[message]"));

    }
    @Test
    public void contentTagsShouldBeUpperCase(){
        final Campaign campaign = new Campaign();
        Map<String, String> contentMap = newLinkedHashMap();

        contentMap.put(MessageContentType.MESSAGE.getKey(), "sql should contain {this} maybe {THAT}");
        campaign.setContent(contentMap);

        campaign.setPromo(false);
        final CampaignForm campaignForm = new CampaignForm(campaign, null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertThat(bindingResult.getFieldError("campaign.content[message]").getDefaultMessage(),
                is("content tags must be upper case:this"));
    }

    @Test
    public void validationChecksThatCustomContentTagsThatAppearInContentShouldAppearInSql() {
        final Campaign campaign = new Campaign();
        Map<String, String> contentMap = newLinkedHashMap();

        campaign.setSqlQuery("select ANOTHER from stuff");
        contentMap.put(MessageContentType.DESCRIPTION.getKey(), "sql should contain {ANOTHER}");
        campaign.setContent(contentMap);

        campaign.setPromo(false);
        final CampaignForm campaignForm = new CampaignForm(campaign, null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertNull(bindingResult.getFieldError("campaign.content[description]"));
    }

    @Test
    public void validatorShouldCheckChannelListIfItIsNotAPromo() {
        Campaign campaign = new Campaign();
        CampaignForm campaignForm = new CampaignForm(campaign, null);
        BindingResult bindingResult = new BeanPropertyBindingResult(campaignForm, CAMPAIGN_FORM_BINDING);
        underTest.validate(campaignForm, bindingResult);

        assertThat(bindingResult.getFieldError("campaign.channels").getDefaultMessage(), is("Tick at least one notification Channel"));

    }

    @Test
    public void naughtySqlValidatorShouldSteriliseSql() {
        assertFalse(underTest.containsNaughtySql("select 1 as player_id"));
        assertFalse(underTest.containsNaughtySql("select 1"));
        assertFalse(underTest.containsNaughtySql(" select 1"));
        assertFalse(underTest.containsNaughtySql("select yo momma may be bad sql but that's not our problem. we're not a sql parser"));

        assertTrue(underTest.containsNaughtySql(" select1"));
        assertTrue(underTest.containsNaughtySql("anything without a select at the start; "));
        assertTrue(underTest.containsNaughtySql("select anything with a delete "));
        assertTrue(underTest.containsNaughtySql("select anything with a update "));
        assertTrue(underTest.containsNaughtySql("select anything with a ; "));
        assertTrue(underTest.containsNaughtySql("select anything with a drop "));
        assertTrue(underTest.containsNaughtySql("select anything with a truncate "));
        assertTrue(underTest.containsNaughtySql("select anything with a alter "));
        assertTrue(underTest.containsNaughtySql("select anything with a exit "));
        assertTrue(underTest.containsNaughtySql("select anything with a replace "));

    }
}
