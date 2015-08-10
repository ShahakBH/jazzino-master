package com.yazino.bi.operations.campaigns.controller;

import com.yazino.bi.operations.campaigns.*;
import com.yazino.bi.operations.campaigns.model.CampaignPlayerUpload;
import com.yazino.engagement.ChannelType;
import com.yazino.promotions.BuyChipsForm;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import strata.server.operations.promotion.service.PaymentOptionsToChipPackageTransformer;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.bi.operations.campaigns.controller.CampaignController.*;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CampaignControllerTest {
    private static final long CAMPAIGN_ID = 1l;
    private static final String CAMPAIGN_NAME = "campaign one";
    private static final DateTime CURRENT_DATE_TIME = new DateTime();

    @Mock
    private CampaignScheduleWithNameDao campaignScheduleWithNameDao;
    @Mock
    private OperationsCampaignService operationsCampaignService;
    @Mock
    private CampaignFormValidator campaignFormValidator;
    @Mock
    private CampaignPlayerUploadValidator campaignPlayerUploadValidator;
    @Mock
    private BindingResult bindingResult;
    @Mock
    private PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer;

    private Map<String, String> supportedGameTypes = newHashMap();

    private CampaignController underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(CURRENT_DATE_TIME.getMillis());
        underTest = new CampaignController(campaignScheduleWithNameDao,
                operationsCampaignService,
                campaignFormValidator,
                campaignPlayerUploadValidator,
                paymentOptionsToChipPackageTransformer);
        underTest.setSupportedGameTypes(supportedGameTypes);//yes. I want to be sick a bit... spring...
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void listRecentEventsShouldAddCampaignToModel() {
        final List<CampaignScheduleWithName> campaignList = asList(
                new CampaignScheduleWithName(CAMPAIGN_ID, "campaign one", new DateTime(), new DateTime(), null, null));
        when(campaignScheduleWithNameDao.getCampaignList(anyInt(), anyInt(), anyBoolean())).thenReturn(campaignList);

        final ModelAndView actual = underTest.listCampaigns(0);
        final ModelAndView expected = new ModelAndView("campaigns/display");
        expected.addObject("campaigns", campaignList);

        assertThat(actual.getViewName(), equalTo(expected.getViewName()));
        Assert.assertThat(actual.getModelMap().get("campaigns"), equalTo(expected.getModelMap().get("campaigns")));
    }

    @Test
    public void addCampaignsShouldCallServiceToAddCampaignIfThereAreNoValidationErrors() {
        final Campaign campaignToBeSaved = CampaignHelper.getDefaultCampaignForView(supportedGameTypes);
        final CampaignForm campaignForm = new CampaignForm(campaignToBeSaved, new BuyChipsForm());

        ModelAndView modelAndView = underTest.addCampaign(campaignForm, bindingResult);
        verify(operationsCampaignService).save(campaignForm);
        assertThat(modelAndView.getViewName(), is(LIST_CAMPAIGNS_VIEW));
    }

    @Test
    public void addCampaignsShouldReturnToCreateViewThereAreValidationErrors() {
        final Campaign campaignToBeSaved = CampaignHelper.getDefaultCampaignForView(supportedGameTypes);
        final CampaignForm campaignForm = new CampaignForm(campaignToBeSaved, new BuyChipsForm());

        when(bindingResult.hasErrors()).thenReturn(true);

        ModelAndView modelAndView = underTest.addCampaign(campaignForm, bindingResult);
        assertThat(modelAndView.getViewName(), is(CampaignController.CREATE_CAMPAIGN_VIEW));
        verifyZeroInteractions(operationsCampaignService);
    }

    @Test
    public void channelMapShouldHaveAllChannels() {
        Map<String, String> channelMap = underTest.createChannelMap();
        Collection<String> channels = channelMap.values();
        assertThat(channels, hasItems(ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION.getDescription(),
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST.getDescription(), ChannelType.IOS.getDescription(),
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID.getDescription()));
    }

    @Test
    public void disableShouldCallDisableOnService() {
        underTest.disableCampaign(123L, 10);
        verify(operationsCampaignService).disable(123L);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void disableShouldCallRedisplayCampaignsKeepingPage() {
        final List<CampaignScheduleWithName> campaignList = newArrayList();
        when(campaignScheduleWithNameDao.getCampaignList(10, PAGE_SIZE, false)).thenReturn(campaignList);
        final ModelAndView modelAndView = underTest.disableCampaign(123L, 10);
        assertThat(((List<CampaignScheduleWithName>) modelAndView.getModelMap().get(CAMPAIGN_DATA_MODEL)), equalTo(campaignList));
        assertThat(((Integer) modelAndView.getModel().get("startPosition")), equalTo(10));

    }

    @Test
    public void createCampaignViewShouldReturnCreateViewWithDefaultModel() {
        ModelMap model = new ModelMap();
        String campaignView = underTest.createCampaignView(model);

        assertThat(campaignView, is(CREATE_CAMPAIGN_VIEW));

        BuyChipsForm buyChipsForm = new BuyChipsForm();
        Map<Integer, BigDecimal> chipsPackagePercentages = new LinkedHashMap<>();
        for (int packageIndex = 1; packageIndex <= 6; packageIndex++) {
            chipsPackagePercentages.put(packageIndex, BigDecimal.ZERO);
        }
        buyChipsForm.setChipsPackagePercentages(chipsPackagePercentages);


        assertThat(model.containsValue(new CampaignForm(CampaignHelper.getDefaultCampaignForView(supportedGameTypes), buyChipsForm)), is(true));
    }

    @Test
    public void editCampaignViewShouldReturnEditViewWithSelectedCampaignForm() {
        Campaign campaignForView = CampaignHelper.getDefaultCampaignForView(supportedGameTypes);
        final CampaignForm campaignForm = new CampaignForm(campaignForView, getDefaultBuyChipsForm());
        when(operationsCampaignService.getCampaignForm(CAMPAIGN_ID)).thenReturn(campaignForm);
        ModelMap model = new ModelMap();
        String campaignView = underTest.editCampaignView(model, CAMPAIGN_ID);

        assertThat(campaignView, is(EDIT_CAMPAIGN_VIEW));
        assertThat((CampaignForm) model.get("campaignForm"), CoreMatchers.equalTo(campaignForm));
    }

    @Test
    public void editCampaignShouldCallDbToUpdateCampaignAndForwardToListCampaignView() {
        Campaign campaign = CampaignHelper.getDefaultCampaignForView(supportedGameTypes);

        final CampaignForm campaignForm = new CampaignForm(campaign, getDefaultBuyChipsForm());
        ModelAndView modelAndView = underTest.editCampaign(campaignForm, null);

        verify(operationsCampaignService).update(campaignForm);
        assertThat(modelAndView.getViewName(), is(LIST_CAMPAIGNS_VIEW));
    }

    @Test
    public void editCampaignShouldSaveCampaignFormWithResetId() {

        Campaign campaign = CampaignHelper.getDefaultCampaignForView(supportedGameTypes);
        campaign.getCampaignScheduleWithName().setCampaignId(-1L);

        final CampaignForm campaignForm = new CampaignForm(campaign, getDefaultBuyChipsForm());
        ModelAndView modelAndView = underTest.editCampaign(campaignForm, null);

        verify(operationsCampaignService).save(campaignForm);
        assertThat(modelAndView.getViewName(), is(LIST_CAMPAIGNS_VIEW));

    }

    @Test
    public void duplicateShouldLoadUpSelectedCampaignAndResetId() {
        Campaign campaign = CampaignHelper.getDefaultCampaignForView(supportedGameTypes);
        final CampaignForm campaignForm = new CampaignForm(campaign, getDefaultBuyChipsForm());
        final ModelMap model = new ModelMap();
        when(operationsCampaignService.getCampaignForm(CAMPAIGN_ID)).thenReturn(campaignForm);
        underTest.duplicateCampaign(model, CAMPAIGN_ID);
        verify(operationsCampaignService).getCampaignForm(CAMPAIGN_ID);
        final CampaignForm campaignFormFromModel = (CampaignForm) model.get("campaignForm");
        assertThat(campaignFormFromModel.getCampaign().getCampaignId(), is(-1L));
        assertThat(campaignFormFromModel.getCampaign().getName(), startsWith("COPY OF "));
    }

    @Test
    public void editCampaignShouldForwardToEditCampaignViewWithMessageInCaseOfError() {
        Campaign campaign = CampaignHelper.getDefaultCampaignForView(supportedGameTypes);

        doThrow(new RuntimeException("Duplicate key")).when(operationsCampaignService).update(Matchers.any(CampaignForm.class));

        final CampaignForm campaignForm = new CampaignForm(campaign, getDefaultBuyChipsForm());
        ModelAndView modelAndView = underTest.editCampaign(campaignForm, null);

        assertThat(modelAndView.getViewName(), is(EDIT_CAMPAIGN_VIEW));
        assertThat((CampaignForm) modelAndView.getModel().get("campaignForm"), CoreMatchers.equalTo(campaignForm));
    }

    @Test
    public void editCampaignShouldReturnEditViewInCaseOfValidationErrors() {
        Campaign campaign = CampaignHelper.getDefaultCampaignForView(supportedGameTypes);

        when(bindingResult.hasErrors()).thenReturn(true);

        ModelAndView modelAndView = underTest.editCampaign(new CampaignForm(campaign, getDefaultBuyChipsForm()), bindingResult);

        assertThat(modelAndView.getViewName(), is(EDIT_CAMPAIGN_VIEW));
        verifyZeroInteractions(operationsCampaignService);
    }

    @Test
    public void listCampaignsShouldSetPaginationFields() {
        final int numberOfCampaigns = 100;
        final int firstRecord = 10;
        final List<CampaignScheduleWithName> campaignList = asList(
                new CampaignScheduleWithName(CAMPAIGN_ID, CAMPAIGN_NAME, new DateTime(), new DateTime(), null, null));

        when(campaignScheduleWithNameDao.getCampaignRecordCount()).thenReturn(numberOfCampaigns);
        when(campaignScheduleWithNameDao.getCampaignList(10, CampaignController.PAGE_SIZE, false)).thenReturn(campaignList);

        final ModelAndView actual = underTest.listCampaigns(firstRecord);
        final ModelAndView expected = new ModelAndView("campaigns/display");
        expected.addObject("campaigns", campaignList);
        expected.addObject("pageSize", CampaignController.PAGE_SIZE);
        expected.addObject("startPosition", firstRecord);
        expected.addObject("totalSize", numberOfCampaigns);

        Assert.assertThat(actual.getViewName(), equalTo(expected.getViewName()));
        Assert.assertThat(actual.getModelMap(), equalTo(expected.getModelMap()));
    }

    @Test
    public void addPlayersShouldAddCampaignFormToModel() {
        final CampaignScheduleWithName expectedCampaignSchedule = new CampaignScheduleWithName(CAMPAIGN_ID,
                CAMPAIGN_NAME,
                new DateTime(),
                new DateTime(),
                2l, 0l);
        when(campaignScheduleWithNameDao.getCampaignScheduleWithName(CAMPAIGN_ID))
                .thenReturn(expectedCampaignSchedule);

        final ModelAndView modelAndView = underTest.addPlayers(CAMPAIGN_ID);
        Assert.assertThat(modelAndView.getViewName(), equalTo("campaigns/addPlayersToCampaign"));
        Assert.assertThat((CampaignScheduleWithName) modelAndView.getModel().get("campaignSchedule"), equalTo(expectedCampaignSchedule));
    }

    @Test
    public void addPlayerShouldAddPayersFromTheCsvFile() throws IOException {
        final CampaignPlayerUpload campaignPlayerUpload = new CampaignPlayerUpload();

        underTest.addPlayers(campaignPlayerUpload, bindingResult);
        verify(operationsCampaignService).addPlayersToCampaign(campaignPlayerUpload);
    }

    private BuyChipsForm getDefaultBuyChipsForm() {
        BuyChipsForm buyChipsForm = new BuyChipsForm();
        buyChipsForm.setInGameNotificationHeader("in game blah");
        buyChipsForm.setMaxRewards(1);
        buyChipsForm.setInGameNotificationMsg("in game message");
        buyChipsForm.setValidForHours(24);
        return buyChipsForm;
    }

}
