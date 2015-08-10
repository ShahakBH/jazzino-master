package com.yazino.engagement.campaign.application;

import com.yazino.bi.campaign.dao.CampaignDefinitionDao;
import com.yazino.bi.campaign.domain.CampaignDefinition;
import com.yazino.engagement.campaign.dao.CampaignContentDao;
import com.yazino.engagement.campaign.dao.CampaignRunDao;
import com.yazino.engagement.campaign.domain.CampaignRun;
import com.yazino.game.api.GameType;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.operations.repository.GameTypeRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.campaign.domain.MessageContentType.MESSAGE;
import static com.yazino.engagement.campaign.domain.MessageContentType.TITLE;
import static com.yazino.engagement.campaign.domain.NotificationCustomField.DISPLAY_NAME;
import static com.yazino.engagement.campaign.domain.NotificationCustomField.PROGRESSIVE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CampaignContentServiceImplTest {
    private static final long CAMPAIGN_ID = 1l;
    private static final long CAMPAIGN_RUN_ID = 2l;
    @Mock
    private CampaignContentDao campaignContentDao;
    @Mock
    private CampaignRunDao campaignRunDao;
    @Mock
    private CampaignDefinitionDao campaignDefinitionDao;
    @Mock
    private GameTypeRepository gameTypeRepository;

    private CampaignContentService underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new CampaignContentServiceImpl(campaignContentDao, campaignRunDao, campaignDefinitionDao, gameTypeRepository);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2013, 10, 15, 9, 0).getMillis());
    }

    @Test
    public void updateCustomDataFieldsShouldDoNothingIfCampaignContentIsEmpty() {
        underTest.updateCustomDataFields(CAMPAIGN_RUN_ID, new HashMap<String, String>());
        Mockito.verifyZeroInteractions(campaignContentDao);
    }

    @Test
    public void updateCustomDataFieldsShouldNotFillProgressiveBonusAmountIfProgressivePlaceHolderDoesNotExist() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(MESSAGE.getKey(), "Nothing to replace here, move on");

        underTest.updateCustomDataFields(CAMPAIGN_RUN_ID, campaignContent);
        Mockito.verifyZeroInteractions(campaignContentDao);
    }

    @Test
    public void updateCustomDataFieldsShouldPopulateMessage() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(MESSAGE.getKey(), "if message has {SENDER_NAME} placeholder then the name should be filled");


        final Map<String, String> customData = newHashMap();
        customData.put("SENDER_NAME", "your mum");

        String expected = "if message has your mum placeholder then the name should be filled";
        final String actual = underTest.personaliseContentData(campaignContent, customData, null).get(MESSAGE.getKey());
        Assert.assertThat(actual, is(equalTo(expected)));

    }

    @Test
    public void updateCustomDataFieldsShouldCallPersistProgressiveBonusToSegmentSelectionIfProgressivePlaceHolderInMessage() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(MESSAGE.getKey(), "if message has {PROGRESSIVE} placeholder then amount should be filled");

        underTest.updateCustomDataFields(CAMPAIGN_RUN_ID, campaignContent);
        Mockito.verify(campaignContentDao).fillProgressiveBonusAmount(CAMPAIGN_RUN_ID);
    }

    @Test
    public void mapCustomDataShouldMapProgressivePlaceHolderToValueInTitle() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(TITLE.getKey(), "{NAME} sent you a gift!");

        Map<String, String> customData = new HashMap<>();
        customData.put("NAME", "Bob");

         String expected = "Bob sent you a gift!";
        final String actual = underTest.personaliseContentData(campaignContent, customData, null).get(TITLE.getKey());
        Assert.assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void updateCustomDataFieldsShouldCallPersistDisplayNameToSegmentSelectionIfDisplayNamePlaceholderInMessage() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(MESSAGE.getKey(), "if message has {DISPLAY_NAME} placeholder then their display name should show");

        underTest.updateCustomDataFields(CAMPAIGN_RUN_ID, campaignContent);
        Mockito.verify(campaignContentDao).fillDisplayName(CAMPAIGN_RUN_ID);
    }

    @Test
    public void mapCustomDataShouldMapProgressivePlaceHolderToValue() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(MESSAGE.getKey(), "if message has {PROGRESSIVE} placeholder then amount should be filled");

        Map<String, String> customData = new HashMap<>();
        customData.put(PROGRESSIVE.name(), "1234");

        String expected = "if message has 1,234 placeholder then amount should be filled";
        final String actual = underTest.personaliseContentData(campaignContent, customData, null).get(MESSAGE.getKey());
        Assert.assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void mapCustomDataShouldMapProgressivePlaceHolderToValueLarge() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(MESSAGE.getKey(), "if message has {PROGRESSIVE} placeholder then amount should be filled");

        Map<String, String> customData = new HashMap<>();
        customData.put(PROGRESSIVE.name(), "2100234");

        String expected = "if message has 2,100,234 placeholder then amount should be filled";
        final String actual = underTest.personaliseContentData(campaignContent, customData, null).get(MESSAGE.getKey());

        Assert.assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void mapCustomDataShouldMapDisplayNameToPlaceholder() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(MESSAGE.getKey(), "if message has {DISPLAY_NAME} placeholder then display name should show");

        Map<String, String> customData = new HashMap<>();
        customData.put(DISPLAY_NAME.name(), "Go Go Gadget");

        final String expected = "if message has Go Go Gadget placeholder then display name should show";
        final String actual = underTest.personaliseContentData(campaignContent, customData, null).get(MESSAGE.getKey());
        Assert.assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void mapCustomDataShouldDoNothingIfThereAreNoCustomFields() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(MESSAGE.getKey(), "if message has does not have placeholder then do nothing");

        Map<String, String> customData = new HashMap<>();

        final String expected = "if message has does not have placeholder then do nothing";
        final String actual = underTest.personaliseContentData(campaignContent, customData, null).get(MESSAGE.getKey());
        Assert.assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void mapCustomDataShouldMapDataIfAvailable() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(MESSAGE.getKey(), "if message {DISPLAY_NAME} has does not have placeholder then do nothing{PROGRESSIVE}");

        Map<String, String> customData = new HashMap<>();
        customData.put(DISPLAY_NAME.name(), "Go Go Gadget");

        final String expected = "if message Go Go Gadget has does not have placeholder then do nothing";
        final String actual = underTest.personaliseContentData(campaignContent, customData, null).get(MESSAGE.getKey());
        Assert.assertThat(actual, is(equalTo(expected)));
    }

    public void mapCustomDataShouldReturnMapWitOriginalDataIfNoCustomDataFoundForField() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(MESSAGE.getKey(), "if message has {PROGRESSIVE} placeholder then amount should be filled");

        Map<String, String> customData = new HashMap<>();

        final Map<String, String> modifiedCampaignContent = underTest.personaliseContentData(campaignContent, customData, null);

        assertThat(modifiedCampaignContent.get(MESSAGE.getKey()),
                is("if message has placeholder then amount should be filled"));
    }

    @Test
    public void mapCustomDataShouldMapMultiplePlaceholders() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(MESSAGE.getKey(), "if message has {PROGRESSIVE} placeholder and {DISPLAY_NAME} then both should be filled");

        Map<String, String> customData = new HashMap<>();
        customData.put(DISPLAY_NAME.name(), "Go Go Gadget");
        customData.put(PROGRESSIVE.name(), "2100234");

        final String expected = "if message has 2,100,234 placeholder and Go Go Gadget then both should be filled";
        final String actual = underTest.personaliseContentData(campaignContent, customData, null).get(MESSAGE.getKey());

        Assert.assertThat(actual, is(equalTo(expected)));
    }


    @Test
    public void substituteTitleForStringIfTitleIsBlankShouldChangeTheTitleIfItIsEmpty() {
        Map<String, String> campaignContent = new HashMap<>();
        campaignContent.put(TITLE.getKey(), "");


        Map<String, String> customData = new HashMap<>();

        final String expected = "Game Name";
        final String actual = underTest.personaliseContentData(campaignContent,
                customData,
                new GameType("12", "Game Name", new HashSet<String>())).get(TITLE.getKey());
        Assert.assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void testGetContentReturnsCorrectContent() throws Exception {
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        underTest.getContent(CAMPAIGN_RUN_ID);
        verify(campaignDefinitionDao).getContent(CAMPAIGN_ID);
    }

    @Test
    public void getEmailListNameShouldReturnNameForEmailVisionUpload() {

        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(new CampaignDefinition(CAMPAIGN_ID,
                "This is the title",
                "",
                new HashMap<String, String>(),
                null,
                null,
                null,
                true, false));

        Assert.assertThat(underTest.getEmailListName(CAMPAIGN_RUN_ID),
                CoreMatchers.is(IsEqual.equalTo("151013_0900-This_is_the_title-" + CAMPAIGN_RUN_ID + ".csv")));
    }
}
