package com.yazino.bi.operations.controller;

import com.yazino.bi.operations.view.*;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.platform.Platform;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import strata.server.lobby.api.promotion.*;
import strata.server.lobby.api.promotion.helper.BuyChipsPromotionBuilder;
import strata.server.lobby.api.promotion.helper.DailyAwardPromotionBuilder;
import com.yazino.bi.operations.persistence.JdbcBackOfficePromotionDao;
import strata.server.operations.promotion.model.ChipPackage;
import com.yazino.bi.operations.model.PlayerReaderResult;
import com.yazino.bi.operations.model.PromotionPlayer;
import com.yazino.bi.operations.model.PromotionPlayerUpload;
import com.yazino.bi.operations.service.FtpOutboundFileSender;
import strata.server.operations.promotion.service.PaymentOptionsToChipPackageTransformer;
import com.yazino.bi.operations.util.PlayerIdReader;
import com.yazino.bi.operations.util.SpringModelAttributeTestHelper;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static com.yazino.bi.operations.controller.PromotionController.*;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"NullableProblems"})
public class PromotionControllerTest {

    @Mock
    private PromotionMaintenanceService promotionMaintenanceService;

    @Mock
    private JdbcBackOfficePromotionDao promotionDao;

    @Mock
    private PlayerIdReader playerIdReader;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private FtpOutboundFileSender ftpOutboundFileSender;

    private ModelMap modelMap;
    private Model model;

    private PromotionController underTest;

    private DailyAwardConfig defaultCfg;

    private Map<Platform, List<ChipPackage>> defaultChipPackages;

    private List<BigDecimal> defaultIOSChipAmounts = new ArrayList<>();

    @Mock
    private PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer;

    @Mock
    private PromotionFormValidator dailyAwardPromotionFormValidator;

    @Mock
    private PromotionFormValidator buyChipsPromotionFormValidator;

    @Before
    public void init() {
        defaultCfg = new DailyAwardConfig();
        defaultCfg.setMainImage("mainImage");
        defaultCfg.setSecondaryImage("secImage");
        defaultCfg.setMainImageLink("mainLink");
        defaultCfg.setSecondaryImageLink("secImageLinnk");
        defaultCfg.setPromotionId(12l);
        defaultCfg.setMaxRewards(2);
        defaultCfg.setRewardChips(3000);
        given(promotionMaintenanceService.getDefaultDailyAwardConfiguration()).willReturn(defaultCfg);

        PaymentOption paymentOption = new PaymentOption();
        paymentOption.setCurrencyLabel("$");
        paymentOption.setAmountRealMoneyPerPurchase(BigDecimal.ONE);
        paymentOption.setId("1");
        paymentOption.setNumChipsPerPurchase(new BigDecimal("10000"));
        paymentOption.setRealMoneyCurrency("USD");

        List<ChipPackage> chipPackages = new ArrayList<>();
        ChipPackage chipPackage = new ChipPackage();
        chipPackage.setDefaultChips(new BigDecimal("10000"));
        chipPackages.add(chipPackage);

        defaultChipPackages = new HashMap<>();
        defaultChipPackages.put(Platform.WEB, chipPackages);
        defaultChipPackages.put(Platform.IOS, chipPackages);
        defaultChipPackages.put(Platform.ANDROID, chipPackages);
        defaultChipPackages.put(Platform.FACEBOOK_CANVAS, chipPackages);

        for (ChipPackage current : defaultChipPackages.get(Platform.IOS)) {
            defaultIOSChipAmounts.add(current.getDefaultChips());
        }


        when(paymentOptionsToChipPackageTransformer.getDefaultPackages()).thenReturn(defaultChipPackages);

        underTest = new PromotionController(promotionMaintenanceService, promotionDao, playerIdReader, paymentOptionsToChipPackageTransformer,
                dailyAwardPromotionFormValidator, buyChipsPromotionFormValidator, ftpOutboundFileSender);
        underTest.setAssetUrl("http://assetulr/jkjkj");
        modelMap = new ExtendedModelMap();
        model = new ExtendedModelMap();
    }

    @Test
    public void list_shouldReturnListView() {
        // when handling a list request
        SearchForm searchForm = new SearchForm();
        modelMap.addAttribute("searchForm", searchForm);
        final String view = underTest.listPromotions(modelMap);

        // THEN list view is returned
        assertEquals(PromotionController.LIST_VIEW, view);
    }

    @Test
    public void listWithSearchFormShouldReturnListView() {
        // when handling a list request
        SearchForm searchForm = new SearchForm();
        final String view = underTest.listPromotions(searchForm, modelMap);

        // THEN list view is returned
        assertEquals(PromotionController.LIST_VIEW, view);
    }

    @Test
    public void listWithSearchOption_shouldFindPromotionsForGivenSearchParameters() {
        // when handling a list request with search form
        SearchForm searchForm = new SearchForm();
        searchForm.setPromotionType(PromotionType.BUY_CHIPS);
        searchForm.setSearchType(PromotionSearchType.LIVE);
        underTest.listPromotions(searchForm, modelMap);

        // THEN a search for ALL promotions should be executed
        verify(promotionDao).find(eq(PromotionSearchType.LIVE), eq(PromotionType.BUY_CHIPS));
    }

    @Test
    public void edit_shouldFindPromotion() {
        // GIVEN the promotion
        final Promotion promotion = new DailyAwardPromotionBuilder().getPromotion();
        given(promotionDao.findById(PromotionSearchType.LIVE, promotion.getId())).willReturn(promotion);

        // WHEN handling an edit request this promotion
        underTest.editPromotion(model, promotion.getId());

        // THEN the promo should be loaded
        verify(promotionDao).findById(PromotionSearchType.LIVE, promotion.getId());
    }

    @Test
    public void edit_shouldAddDailyAwardPromotionFormToModel() {
        // GIVEN the promotion
        final DailyAwardPromotion promotion = new DailyAwardPromotionBuilder().getPromotion();
        given(promotionDao.findById(PromotionSearchType.LIVE, promotion.getId())).willReturn(promotion);

        // WHEN editing
        underTest.editPromotion(model, promotion.getId());

        // THEN the promotionForm is added to the model
        assertEquals(new DailyAwardPromotionForm(promotion), model.asMap().get("promotionForm"));
    }

    @Test
    public void shouldAddHoursAndMinutesToModel() {
        // GIVEN the promotion
        final DailyAwardPromotion promotion = new DailyAwardPromotionBuilder().getPromotion();
        given(promotionDao.findById(PromotionSearchType.LIVE, promotion.getId())).willReturn(promotion);

        Model model = SpringModelAttributeTestHelper.collectModelAttributes(underTest);

        // THEN the hours and minutes are added to the model
        assertNotNull(model.asMap().get("hours"));
        assertNotNull(model.asMap().get("minutes"));
    }

    @Test
    public void edit_shouldAddCorrectFormFieldsForDailyAwardPromotion() {
        final Promotion dailyAwardPromotion = new DailyAwardPromotionBuilder().getPromotion();

        given(promotionDao.findById(PromotionSearchType.LIVE, dailyAwardPromotion.getId())).willReturn(dailyAwardPromotion);

        underTest.editPromotion(model, dailyAwardPromotion.getId());

        assertEquals("http://assetulr/jkjkj", model.asMap().get("assetUrl"));
        assertEquals("mainImage", model.asMap().get("defaultMainImageUrl"));
        assertEquals("secImage", model.asMap().get("defaultSecondaryImageUrl"));
    }

    @Test
    public void edit_shouldAddCorrectFormFieldsForBuyChipPromotion() {
        final BuyChipsPromotion buyChipPromotion = new BuyChipsPromotionBuilder().getPromotion();

        given(promotionDao.findById(PromotionSearchType.LIVE, buyChipPromotion.getId())).willReturn(buyChipPromotion);

        underTest.editPromotion(model, buyChipPromotion.getId());

        assertEquals("http://assetulr/jkjkj", model.asMap().get("assetUrl"));
        assertNotNull(model.asMap().get("defaultWebBuyChipPackages"));
        assertNotNull(model.asMap().get("defaultIOSBuyChipPackages"));
        assertNotNull(model.asMap().get("defaultFacebookBuyChipPackages"));
        assertNotNull(model.asMap().get("defaultAndroidBuyChipPackages"));
    }

    @Test
    public void edit_shouldAddCorrectFormFieldsForProgressiveChipPromotion() {
        final Promotion progressivePromotion = new DailyAwardPromotionBuilder()
                .withPromotionType(PromotionType.PROGRESSIVE_DAY_1)
                .getPromotion();

        given(promotionDao.findById(PromotionSearchType.LIVE, progressivePromotion.getId())).willReturn(progressivePromotion);

        underTest.editPromotion(model, progressivePromotion.getId());
        assertEquals("http://assetulr/jkjkj", model.asMap().get("assetUrl"));
        assertEquals("mainImage", model.asMap().get("defaultMainImageUrl"));
        assertEquals("secImage", model.asMap().get("defaultSecondaryImageUrl"));
    }

    @Test
    public void copy_shouldFindCurrentPromotionAndPersistCopy() throws IOException {
        // GIVEN the promotion
        final DailyAwardPromotion promotion = new DailyAwardPromotionBuilder().getPromotion();
        given(promotionDao.findById(PromotionSearchType.LIVE, promotion.getId())).willReturn(promotion);

        // WHEN copying
        String redirect = underTest.copyPromotion(promotion.getId());

        // THEN promo should be persisted
        verify(promotionMaintenanceService).create(promotion);

        // AND redirect to list
        assertEquals(LIST_REDIRECT, redirect);
    }

    @Test
    public void copy_shouldTruncateLongPromotionName() {
        String LONG_PROMOTION_NAME = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        String newName = underTest.getCopiedPromotionNewName(LONG_PROMOTION_NAME);
        assertTrue(newName.length() <= PromotionController.COPIED_PROMOTION_NEW_NAME_MAX_LENGTH);
    }

    @Test
    public void copy_shouldModifyPromotionName() {
        String PROMOTION_NAME = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String newName = underTest.getCopiedPromotionNewName(PROMOTION_NAME);
        assertTrue(newName.startsWith(PromotionController.COPIED_PROMOTION_NEW_NAME_PREFIX));
    }

    @Test
    public void copy_shouldSetPromotionAttibutes() {
        final DateTime TEN_MINUTES_AGO = (new DateTime()).minusMinutes(10);
        final DateTime TOMORROW = (new DateTime()).plusDays(1);

        final Promotion promotion = new DailyAwardPromotionBuilder().getPromotion();
        promotion.setId(1L);
        promotion.setStartDate(TEN_MINUTES_AGO);
        promotion.setEndDate(TOMORROW);

        underTest.setCopiedPromotionAttributes(promotion);
        assertNull(promotion.getId());
        assertTrue(promotion.getControlGroupFunction().equals(ControlGroupFunctionType.EXTERNAL_ID));
        assertFalse(promotion.isActive());
    }

    @Test
    public void create_shouldAddDailyAwardPromotionFormToModel() {
        // WHEN creating
        SearchForm searchForm = new SearchForm();
        searchForm.setPromotionType(PromotionType.DAILY_AWARD);
        underTest.createPromotion(searchForm, model);

        // THEN the promotionForm is added to the model
        final DailyAwardPromotionForm actual = (DailyAwardPromotionForm) model.asMap().get("promotionForm");
        assertNotNull(actual);

        // AND defaults have been set
        assertEquals(0, actual.getStartHour().intValue());
        assertEquals(0, actual.getStartMinute().intValue());
        assertEquals(23, actual.getEndHour().intValue());
        assertEquals(59, actual.getEndMinute().intValue());
        assertEquals("ALL", actual.getAllPlayers());
    }

    @Test
    public void create_shouldAddBuyChipsPromotionFormToModel() {
        // WHEN creating
        SearchForm searchForm = new SearchForm();
        searchForm.setPromotionType(PromotionType.BUY_CHIPS);
        underTest.createPromotion(searchForm, model);

        // THEN the promotionForm is added to the model
        final BuyChipsPromotionForm actual = (BuyChipsPromotionForm) model.asMap().get("promotionForm");
        assertNotNull(actual);

        // AND defaults have been set
        assertEquals(0, actual.getStartHour().intValue());
        assertEquals(0, actual.getStartMinute().intValue());
        assertEquals(23, actual.getEndHour().intValue());
        assertEquals(59, actual.getEndMinute().intValue());
        assertEquals("ALL", actual.getAllPlayers());
    }

    @Test
    public void create_shouldAddAssetUrlToModel() {
        // WHEN creating
        SearchForm searchForm = new SearchForm();
        searchForm.setPromotionType(PromotionType.DAILY_AWARD);
        underTest.createPromotion(searchForm, model);

        // THEN the hours and minutes are added to the model
        assertNotNull(model.asMap().get("assetUrl"));
    }

    @Test
    public void save_shouldValidatePromotion_dailyAward() throws IOException {

        // GIVEN promotion form
        final DailyAwardPromotionForm promotionForm = new DailyAwardPromotionForm(new DailyAwardPromotionBuilder().getPromotion());

        // WHEN saving
        underTest.savePromotion(model, promotionForm, bindingResult);

        // THEN validator should be invoked
        verify(dailyAwardPromotionFormValidator).validate(promotionForm, bindingResult);
    }

    @Test
    public void save_shouldStopProcessingIfValidatorRaisesErrors_dailyAward() throws IOException {

        when(bindingResult.hasErrors()).thenReturn(true);

        // GIVEN promotion form
        final DailyAwardPromotionForm promotionForm = new DailyAwardPromotionForm(new DailyAwardPromotionBuilder().getPromotion());

        // WHEN saving
        String view = underTest.savePromotion(model, promotionForm, bindingResult);

        assertEquals(PromotionController.EDIT_VIEW, view);
        verify(promotionMaintenanceService, never()).create(any(Promotion.class));
        verify(promotionMaintenanceService, never()).update(any(Promotion.class));
    }

    @Test
    public void save_shouldValidatePromotion_buyChips() throws IOException {

        // GIVEN promotion form
        final BuyChipsPromotionForm promotionForm = new BuyChipsPromotionForm(new BuyChipsPromotionBuilder().getPromotion());

        // WHEN saving
        underTest.savePromotion(model, promotionForm, bindingResult);

        // THEN validator should be invoked
        verify(buyChipsPromotionFormValidator).validate(promotionForm, bindingResult);
    }

    @Test
    public void save_shouldStopProcessingIfValidatorRaisesErrors_buyChips() throws IOException {

        when(bindingResult.hasErrors()).thenReturn(true);

        // GIVEN promotion form
        final BuyChipsPromotionForm promotionForm = new BuyChipsPromotionForm(new BuyChipsPromotionBuilder().getPromotion());

        // WHEN saving
        String view = underTest.savePromotion(model, promotionForm, bindingResult);

        assertEquals(PromotionController.EDIT_VIEW, view);
        verify(promotionMaintenanceService, never()).create(any(Promotion.class));
        verify(promotionMaintenanceService, never()).update(any(Promotion.class));
    }


    @Test
    public void save_shouldAddAssetUrlToModelAndReturnEditViewIfValidationFails() throws IOException {
        // GIVEN promotion form
        final DailyAwardPromotionForm promotionForm = new DailyAwardPromotionForm(new DailyAwardPromotionBuilder().getPromotion());
        // AND failing validation
        given(bindingResult.hasErrors()).willReturn(Boolean.TRUE);
        given(promotionMaintenanceService.getDefaultDailyAwardConfiguration()).willReturn(defaultCfg);

        // WHEN saving
        final String viewName = underTest.savePromotion(model, promotionForm, bindingResult);

        // THEN the hours and minutes are added to the model
        assertNotNull(model.asMap().get("assetUrl"));

        // AND edit view is returned
        assertEquals(PromotionController.EDIT_VIEW, viewName);
    }

    @Test
    public void save_shouldCreatePromotionAndRedirectToList() throws IOException {
        // GIVEN new promotion form
        final DailyAwardPromotion promotion = new DailyAwardPromotionBuilder().getPromotion();

        promotion.setId(null);
        final DailyAwardPromotionForm promotionForm = new DailyAwardPromotionForm(promotion);

        // WHEN saving
        final String redirect = underTest.savePromotion(model, promotionForm, bindingResult);

        // THEN promo should be persisted
        verify(promotionMaintenanceService).create(promotion);

        // AND redirect to list
        assertEquals(LIST_REDIRECT, redirect);
    }

    @Test
    public void save_shouldCreateBuyChipsPromotionAndRedirectToList() throws IOException {
        // GIVEN new promotion form
        final BuyChipsPromotion promotion = new BuyChipsPromotionBuilder()
                .withDefaultChipsForPlatformAndPackage(new BigDecimal(BuyChipsPromotionBuilder.CHIP_DEFAULT_PACKAGE_VALUE), Platform.WEB)
                .getPromotion();
        promotion.setId(null);
        final BuyChipsPromotionForm promotionForm = new BuyChipsPromotionForm(promotion);
        PromotionFormFactory.applyOverridesFromPromotionToForm(promotion, promotionForm, defaultChipPackages);

        // WHEN saving
        final String redirect = underTest.savePromotion(model, promotionForm, bindingResult);

        // THEN promo should be persisted
        verify(promotionMaintenanceService).create(eq(promotion));

        // AND redirect to list
        assertEquals(LIST_REDIRECT, redirect);
    }

    @Test
    public void save_shouldUpdatePromotionAddRedirectToList() throws IOException {
        // GIVEN new promotion form
        final BuyChipsPromotion promotion = new BuyChipsPromotionBuilder().withId(12l)
                .withDefaultChipsForPlatformAndPackage(new BigDecimal(BuyChipsPromotionBuilder.CHIP_DEFAULT_PACKAGE_VALUE), Platform.WEB)
                .getPromotion();
        final BuyChipsPromotionForm promotionForm = new BuyChipsPromotionForm(promotion);
        PromotionFormFactory.applyOverridesFromPromotionToForm(promotion, promotionForm, defaultChipPackages);

        // WHEN saving
        final String redirect = underTest.savePromotion(model, promotionForm, bindingResult);

        // THEN promo should be updated
        verify(promotionMaintenanceService).update(promotion);

        // AND redirect to list
        assertEquals(LIST_REDIRECT, redirect);
    }

    @Test
    public void show_shouldQueryFirstPageOfPlayersAndAddPlayerPageToSession() {
        // given session
        final MockHttpSession session = new MockHttpSession();
        // and promo id
        final Long promoId = 1l;
        // and page size of 50
        final Integer pageSize = 1;
        // and promotions has these players
        final PromotionPlayer p1 = new PromotionPlayer();
        p1.setPromotionId(promoId);
        p1.setPlayerId(BigInteger.valueOf(1));
        final PromotionPlayer p2 = new PromotionPlayer();
        p1.setPromotionId(promoId);
        p1.setPlayerId(BigInteger.valueOf(2));
        final List<PromotionPlayer> players = Arrays.asList(p1, p2);
        given(promotionDao.findPlayers(PromotionSearchType.LIVE, promoId, 0, pageSize)).willReturn(players);
        final Promotion promo = new BuyChipsPromotion();
        promo.setId(promoId);
        given(promotionDao.findById(PromotionSearchType.LIVE, promoId)).willReturn(promo);
        given(promotionDao.countPlayersInPromotion(PromotionSearchType.LIVE, promoId)).willReturn(2);

        // when requesting first page of players
        underTest.showPlayers(session, PromotionSearchType.LIVE, promoId, pageSize);

        // then page object should be added to session
        final PlayerPage page = (PlayerPage) session.getAttribute("page");
        assertNotNull(page);
        assertEquals(1, page.getPageNumber());
        assertEquals(2, page.getPagesAvailable());
        assertEquals(1, page.getPageSize());
    }

    @Test
    public void shouldRefuseSubmitsWithNoFileOnCreate() throws IOException {
        // GIVEN the file information of the promotion upload returns an empty name
        final PromotionPlayerUpload upload = new PromotionPlayerUpload();
        final MultipartFile file = mock(MultipartFile.class);
        upload.setFile(file);
        given(file.getOriginalFilename()).willReturn("");

        // AND there is a binder object to pass to the controller
        final BindingResult binder = mock(BindingResult.class);

        // WHEN trying to add players for a promotion
        final String view = underTest.addPlayers(mock(HttpSession.class), model, upload, binder);

        // THEN the submit is refused
        assertThat(view, is(ADD_PLAYERS_VIEW));

        // AND the binder gets the error information
        verify(binder).rejectValue(eq("file"), eq("nofile"), anyString());
    }

    @Test
    public void shouldRefuseSubmitsWithEmptyFileOnCreate() throws IOException {
        // GIVEN the file information of the promotion upload returns an empty file
        final PromotionPlayerUpload upload = new PromotionPlayerUpload();
        final MultipartFile file = mock(MultipartFile.class);
        upload.setFile(file);
        given(file.getOriginalFilename()).willReturn("blabla");
        given(file.isEmpty()).willReturn(true);

        // AND there is a binder object to pass to the controller
        final BindingResult binder = mock(BindingResult.class);

        // WHEN trying to add players for a promotion
        final String view = underTest.addPlayers(mock(HttpSession.class), model, upload, binder);

        // THEN the submit is refused
        assertThat(view, is(ADD_PLAYERS_VIEW));

        // AND the binder gets the error information
        verify(binder).rejectValue(eq("file"), eq("empty"), any(Object[].class), anyString());
    }

    @Test
    // TODO add tests for mnew model attributes
    public void whenUploadingPlayersErrorsShouldBeAddedToModel() throws IOException {
        // GIVEN the file information of the promotion upload is not accepted by the CSV reader
        final PromotionPlayerUpload upload = new PromotionPlayerUpload();
        final MultipartFile file = mock(MultipartFile.class);
        upload.setFile(file);
        given(file.getOriginalFilename()).willReturn("blabla");
        given(file.isEmpty()).willReturn(false);
        final InputStream isMock = mock(InputStream.class);
        given(file.getInputStream()).willReturn(isMock);
        PlayerReaderResult playerReaderResult = new PlayerReaderResult();
        playerReaderResult.addMatched(BigDecimal.TEN);
        playerReaderResult.addNotMatched("facebook", "343453453");
        playerReaderResult.addInvalidInputLine(Arrays.asList("part1", "part2", "part3"));
        playerReaderResult.addInvalidInputLine(Arrays.asList("part1", "part2", "part3", "part4"));
        playerReaderResult.addMultipleMatch("yazino", "675", Arrays.asList(BigDecimal.valueOf(2), BigDecimal.valueOf(5)));
        given(playerIdReader.readPlayerIds(any(InputStream.class))).willReturn(playerReaderResult);

        // AND there is a binder object to pass to the controller
        final BindingResult binder = mock(BindingResult.class);

        // WHEN trying to add players to a promotion
        underTest.addPlayers(mock(HttpSession.class), model, upload, binder);

        // THEN the submit is ok and show players view is returned
//        assertThat(view, is(ADD_PLAYERS_VIEW));

        // AND input line errors are added to model
        assertThat((List<List<String>>) model.asMap().get("inputLineErrors"), hasItems(Arrays.asList("part1", "part2", "part3"),
                Arrays.asList("part1", "part2", "part3", "part4")));
        // AND rpx external ids not matched are added to model
        assertThat((List<PlayerReaderResult.RpxCredential>) model.asMap().get("rpxIdsNotMatched"),
                hasItem(new PlayerReaderResult.RpxCredential("facebook", "343453453")));
        // AND rpx external ids matched to multiple players are added to model
        assertThat((List<PlayerReaderResult.RpxCredential>) model.asMap().get("rpxIdsMatchedToManyPlayers"),
                hasItem(new PlayerReaderResult.RpxCredential("yazino", "675", Arrays.asList(BigDecimal.valueOf(2), BigDecimal.valueOf(5)))));
    }

    @Test
    public void shouldRefuseSubmitsWithIoError() throws IOException {
        // GIVEN the file information of the promotion upload is not accepted by the CSV reader
        final PromotionPlayerUpload upload = new PromotionPlayerUpload();
        final MultipartFile file = mock(MultipartFile.class);
        upload.setFile(file);
        given(file.getOriginalFilename()).willReturn("blabla");
        given(file.isEmpty()).willReturn(false);
        final InputStream isMock = mock(InputStream.class);
        given(file.getInputStream()).willReturn(isMock);

        given(playerIdReader.readPlayerIds(any(InputStream.class))).willThrow(new IOException());

        // AND there is a binder object to pass to the controller
        final BindingResult binder = mock(BindingResult.class);

        // WHEN trying to add players for a promotion
        final String view = underTest.addPlayers(mock(HttpSession.class), model, upload, binder);

        // THEN the submit is refused
        assertThat(view, is(ADD_PLAYERS_VIEW));

        // AND the binder gets the error information
        verify(binder).rejectValue(eq("file"), eq("playerUpload.error"), any(Object[].class), anyString());
    }

    @Test
    public void shouldAcceptValidFileUpload() throws IOException {
        // GIVEN the file information of the promotion upload
        final PromotionPlayerUpload upload = new PromotionPlayerUpload();
        final MultipartFile file = mock(MultipartFile.class);
        upload.setFile(file);
        given(file.getOriginalFilename()).willReturn("blabla");
        given(file.isEmpty()).willReturn(false);
        final InputStream isMock = mock(InputStream.class);
        given(file.getInputStream()).willReturn(isMock);

        // AND the Set of player IDs is read
        PlayerReaderResult playerReaderResult = new PlayerReaderResult();
        playerReaderResult.addMatched(BigDecimal.valueOf(17L));
        given(playerIdReader.readPlayerIds(any(InputStream.class))).willReturn(playerReaderResult);

        // AND the promotion ID is received from the client
        final Long promoId = 12L;
        upload.setPromotionId(promoId);

        // AND there is a binder object to pass to the controller
        final BindingResult binder = mock(BindingResult.class);

        // WHEN trying to add players for a promotion
        final String view = underTest.addPlayers(mock(HttpSession.class), model, upload, binder);

        // THEN the submit is refused
        assertThat(view, is(SHOW_PLAYERS_VIEW));

        // AND the promotion service gets the uploaded information
        verify(promotionMaintenanceService).addPlayersTo(eq(promoId), eq(playerReaderResult.getPlayerIds()));
    }

}
