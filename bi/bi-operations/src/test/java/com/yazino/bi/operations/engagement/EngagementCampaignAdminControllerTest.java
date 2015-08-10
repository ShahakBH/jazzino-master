package com.yazino.bi.operations.engagement;

import com.yazino.engagement.ChannelType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import com.yazino.bi.operations.util.TargetCsvReader;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.*;

public class EngagementCampaignAdminControllerTest {

    public static final int APPREQUEST_TARGET_ID = 101;
    public static final BigDecimal TARGET_ID1 = new BigDecimal(102);
    public static final BigDecimal TARGET_ID2 = new BigDecimal(103);
    public static final String UNKNOWN_IOS_GAME_TYPE = "UNKNOWN_IOS_GAME_TYPE";
    public static final BigDecimal TARGET_ID3 = BigDecimal.valueOf(104);

    @Mock
    private EngagementCampaignDao campaignDao;
    @Mock
    private EngagementCampaignValidator engagementCampaignValidator;
    @Mock
    private GameTypeValidator facebookGameTypeValidator;
    @Mock
    private GameTypeValidator iosGameTypeValidator;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private TargetCsvReader reader;

    @Mock
    private MultipartFile file;
    EngagementCampaignAdminController underTest;
    private ModelMap model;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Map<ChannelType, GameTypeValidator> gameTypeValidatorMap = new HashMap<>();
        model = new ModelMap();
        gameTypeValidatorMap.put(ChannelType.IOS, iosGameTypeValidator);
        gameTypeValidatorMap.put(ChannelType.FACEBOOK_APP_TO_USER_REQUEST, facebookGameTypeValidator);
        gameTypeValidatorMap.put(ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION, facebookGameTypeValidator);
        underTest = new EngagementCampaignAdminController(campaignDao, engagementCampaignValidator, reader);
        underTest.setGameTypeValidatorMap(gameTypeValidatorMap);
    }

    @Test
    public void listShouldReturnModelAndView() {
        assertEquals("/engagementCampaign/list", underTest.list(new ModelMap()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void listShouldAddAllAppRequestToModelMap() {
        // given a list of app requests
        final List<EngagementCampaign> expectedEngagementCampaigns =
                asList(new EngagementCampaignBuilder().withId(new Integer("123")).build(),
                        new EngagementCampaignBuilder().withId(new Integer("321")).build());
        Mockito.when(campaignDao.findAll()).thenReturn(expectedEngagementCampaigns);

        // when request list view
        ModelMap map = new ModelMap();
        underTest.list(map);

        // then app requests should be added to the model map
        List<EngagementCampaign> engagementCampaigns = (List<EngagementCampaign>) map.get("engagementCampaigns");
        assertEquals(2, engagementCampaigns.size());
        assertTrue(engagementCampaigns.containsAll(expectedEngagementCampaigns));
    }

    @Test
    @SuppressWarnings("NullableProblems")
    public void createShouldCallDaoCreateWithCorrectParamsAndNullId() {
        EngagementCampaign enagagementCampaign = new EngagementCampaignTestBuilder().build();
        enagagementCampaign.setId(null);
        ModelMap map = new ModelMap();


        underTest.create(map, enagagementCampaign, bindingResult);
        Mockito.verify(campaignDao).create(enagagementCampaign);
    }

    @Test
    public void deleteShouldCallDaoDeleteMethod() {
        Integer requestId = new Integer("2213");
        ModelMap map = new ModelMap();

        Mockito.when(campaignDao.findById(requestId)).thenReturn(new EngagementCampaignTestBuilder().build());
        underTest.delete(map, requestId);
        Mockito.verify(campaignDao).delete(requestId);
    }

    @Test
    public void deleteShouldAddMessageToModel() {
        Integer faceBookRequestId = new Integer("2213");
        ModelMap map = new ModelMap();

        Mockito.when(campaignDao.findById(faceBookRequestId)).thenReturn(new EngagementCampaignTestBuilder().build());
        underTest.delete(map, faceBookRequestId);
        assertEquals("'descriptions are over rated' has been deleted.", map.get("msg"));
    }


    @Test
    public void addTargetShouldReturnListRedirectViewAndErrorMessageOnBlankFileName() throws IOException {
        given(file.getOriginalFilename()).willReturn("");
        AppRequestTargetUpload appRequestTargetUpload = new AppRequestTargetUpload();
        appRequestTargetUpload.setFile(file);


        final String actualView = underTest.addTargets(new ModelMap(), appRequestTargetUpload, bindingResult);
        final String expectedView = EngagementCampaignAdminController.LIST_REDIRECT_VIEW;

        assertEquals(expectedView, actualView);
        verify(bindingResult).rejectValue("file", "nofile", "Please choose file to upload");
    }


    @Test
    public void addTargetShouldReturnListRedirectViewAndErrorMessageForEmptyFile() throws IOException {

        // GIVEN the file information of the upload returns an empty name
        AppRequestTargetUpload upload = new AppRequestTargetUpload();

        upload.setFile(file);
        given(file.getOriginalFilename()).willReturn("File Names are Overrated");
        given(file.isEmpty()).willReturn(true);

        // WHEN trying to add players
        underTest.addTargets(new ModelMap(), upload, bindingResult);

        // AND the binder gets the error information
        verify(bindingResult).rejectValue(eq("file"), eq("empty"), Matchers.<Object[]>anyObject(), anyString());
    }

    @Test
    public void testAddPlayers_HappyPath() throws IOException {
        // GIVEN the file information of the promotion upload is accepted by the CSV reader
        final AppRequestTargetUpload upload = new AppRequestTargetUpload();
        upload.setFile(file);
        upload.setId(APPREQUEST_TARGET_ID);
        upload.setChannelType(ChannelType.FACEBOOK_APP_TO_USER_REQUEST);
        given(file.getOriginalFilename()).willReturn("a lovely file name");
        given(file.isEmpty()).willReturn(false);
        final InputStream isMock = mock(InputStream.class);
        given(file.getInputStream()).willReturn(isMock);

        Set<AppRequestTarget> readerResult = new HashSet<>();

        AppRequestTarget target1 = new AppRequestTargetBuilder().withGameType("SLOTS").withPlayerId(TARGET_ID1).build();
        AppRequestTarget target2 = new AppRequestTargetBuilder().withGameType("BLACKJACK").withPlayerId(TARGET_ID2).build();
        readerResult.add(target1);
        readerResult.add(target2);
        given(reader.readTargets((any(InputStream.class)))).willReturn(readerResult);

        // WHEN trying to add players
        final String view = underTest.addTargets(model, upload, bindingResult);

        verify(campaignDao).addAppRequestTargets(101, new ArrayList<>(readerResult));
        Assert.assertEquals(EngagementCampaignAdminController.LIST_REDIRECT_VIEW, view);
    }

    @Test
    public void addingTargetsToIosAppRequestShouldIgnoreInvalidTargets() throws IOException {
        // GIVEN the file information of the promotion upload is accepted by the CSV reader
        final AppRequestTargetUpload upload = new AppRequestTargetUpload();
        upload.setFile(file);
        upload.setId(APPREQUEST_TARGET_ID);
        upload.setChannelType(ChannelType.IOS);
        given(file.getOriginalFilename()).willReturn("a lovely file name");
        given(file.isEmpty()).willReturn(false);
        final InputStream isMock = mock(InputStream.class);
        given(file.getInputStream()).willReturn(isMock);

        Set<AppRequestTarget> readerResult = new HashSet<>();

        AppRequestTarget target1 = new AppRequestTargetBuilder().withGameType("SLOTS").withPlayerId(TARGET_ID1).build();
        AppRequestTarget target2 = new AppRequestTargetBuilder().withGameType("BLACKJACK").withPlayerId(TARGET_ID2).build();
        AppRequestTarget target3 = new AppRequestTargetBuilder().withGameType(UNKNOWN_IOS_GAME_TYPE).withPlayerId(TARGET_ID3).build();
        readerResult.add(target1);
        readerResult.add(target2);
        readerResult.add(target3);
        given(reader.readTargets((any(InputStream.class)))).willReturn(readerResult);

        // WHEN adding targets
        final String view = underTest.addTargets(model, upload, bindingResult);

        // validator should be checked if IOS
        verify(iosGameTypeValidator).validate(readerResult, bindingResult);
        Assert.assertEquals(EngagementCampaignAdminController.LIST_REDIRECT_VIEW, view);
    }

    @Test
    public void addingTargetsToFacebookAppRequestShouldIgnoreInvalidTargets() throws IOException {
        // GIVEN the file information of the promotion upload is accepted by the CSV reader
        final AppRequestTargetUpload upload = new AppRequestTargetUpload();
        upload.setFile(file);
        upload.setId(APPREQUEST_TARGET_ID);
        upload.setChannelType(ChannelType.FACEBOOK_APP_TO_USER_REQUEST);
        given(file.getOriginalFilename()).willReturn("a lovely file name");
        given(file.isEmpty()).willReturn(false);
        final InputStream isMock = mock(InputStream.class);
        given(file.getInputStream()).willReturn(isMock);

        Set<AppRequestTarget> readerResult = new HashSet<>();

        AppRequestTarget target1 = new AppRequestTargetBuilder().withGameType("SLOTS").withPlayerId(TARGET_ID1).build();
        AppRequestTarget target2 = new AppRequestTargetBuilder().withGameType("BLACKJACK").withPlayerId(TARGET_ID2).build();
        AppRequestTarget target3 = new AppRequestTargetBuilder().withGameType("UNKNOWN_FACEBOOK_GAME_TYPE").withPlayerId(TARGET_ID3).build();
        readerResult.add(target1);
        readerResult.add(target2);
        readerResult.add(target3);
        given(reader.readTargets((any(InputStream.class)))).willReturn(readerResult);

        // WHEN adding targets
        final String view = underTest.addTargets(model, upload, bindingResult);

        // validator should be checked if IOS
        verify(facebookGameTypeValidator).validate(readerResult, bindingResult);
        Assert.assertEquals(EngagementCampaignAdminController.LIST_REDIRECT_VIEW, view);
    }

    @Test
    public void addingTargetsToFacebookAppToUserNotificationRequestShouldIgnoreInvalidTargets() throws IOException {
        // GIVEN the file information of the promotion upload is accepted by the CSV reader
        final AppRequestTargetUpload upload = new AppRequestTargetUpload();
        upload.setFile(file);
        upload.setId(APPREQUEST_TARGET_ID);
        upload.setChannelType(ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION);
        given(file.getOriginalFilename()).willReturn("a lovely file name");
        given(file.isEmpty()).willReturn(false);
        final InputStream isMock = mock(InputStream.class);
        given(file.getInputStream()).willReturn(isMock);

        Set<AppRequestTarget> readerResult = new HashSet<>();

        AppRequestTarget target1 = new AppRequestTargetBuilder().withGameType("SLOTS").withPlayerId(TARGET_ID1).build();
        AppRequestTarget target2 = new AppRequestTargetBuilder().withGameType("BLACKJACK").withPlayerId(TARGET_ID2).build();
        AppRequestTarget target3 = new AppRequestTargetBuilder().withGameType("UNKNOWN_FACEBOOK_GAME_TYPE").withPlayerId(TARGET_ID3).build();
        readerResult.add(target1);
        readerResult.add(target2);
        readerResult.add(target3);
        given(reader.readTargets((any(InputStream.class)))).willReturn(readerResult);

        // WHEN adding targets
        final String view = underTest.addTargets(model, upload, bindingResult);

        // validator should be checked if IOS
        verify(facebookGameTypeValidator).validate(readerResult, bindingResult);
        Assert.assertEquals(EngagementCampaignAdminController.LIST_REDIRECT_VIEW, view);
    }

    @Test
    public void showTargetsShouldAddTargetsToModel() {
        final int appRequestId1 = 123;
        final int appRequestId2 = 124;
        Integer pageNumber = 1;
        List<AppRequestTarget> targetList = new ArrayList<>();
        targetList.add(new AppRequestTarget(1, appRequestId1, TARGET_ID1, "545", "SLOTS"));
        targetList.add(new AppRequestTarget(2, appRequestId2, TARGET_ID1, "545", "BLACKJACK"));

        when(campaignDao.findAppRequestTargetsById(eq(appRequestId1), anyInt(), anyInt())).thenReturn(targetList);
        underTest.showTargets(model, appRequestId1, pageNumber);
        assertEquals(targetList, model.get("targets"));
    }

    @Test
    public void showTargetsShouldAddEngagementCampaignIdToModel() {
        final int appRequestId = 123;
        Integer pageNumber = 1;
        List<AppRequestTarget> targetList = new ArrayList<>();
        targetList.add(new AppRequestTarget(1, appRequestId, TARGET_ID1, "545", "SLOTS"));
        targetList.add(new AppRequestTarget(2, appRequestId, TARGET_ID2, "545", "BLACKJACK"));

        when(campaignDao.findAppRequestTargetsById(eq(appRequestId), anyInt(), anyInt())).thenReturn(targetList);
        underTest.showTargets(model, appRequestId, pageNumber);
        assertEquals(appRequestId, model.get("engagementCampaignId"));
    }

}
