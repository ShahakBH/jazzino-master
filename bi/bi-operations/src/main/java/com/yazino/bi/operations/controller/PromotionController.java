package com.yazino.bi.operations.controller;

import com.yazino.bi.operations.model.PlayerReaderResult;
import com.yazino.bi.operations.model.PromotionPlayerUpload;
import com.yazino.bi.operations.persistence.JdbcBackOfficePromotionDao;
import com.yazino.bi.operations.service.FtpOutboundFileSender;
import com.yazino.bi.operations.util.DateTimeEditor;
import com.yazino.bi.operations.util.PlayerIdReader;
import com.yazino.bi.operations.view.*;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import strata.server.lobby.api.promotion.*;
import strata.server.operations.promotion.model.ChipPackage;
import strata.server.operations.promotion.service.PaymentOptionsToChipPackageTransformer;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SessionAttributes("searchForm")
@Controller
public class PromotionController {

    private static final Logger LOG = LoggerFactory.getLogger(PromotionController.class);

    static final String LIST_VIEW = "promotion/list";
    static final String ADD_PLAYERS_VIEW = "promotion/addPlayers";
    public static final String EDIT_VIEW = "promotion/edit";
    public static final String SHOW_PLAYERS_VIEW = "promotion/showPlayers";
    public static final Integer DEFAULT_PLAYER_PAGE_SIZE = 500;
    static final String LIST_REDIRECT = "redirect:list";
    public static final int HOURS_IN_DAY = 24;
    public static final int MINUTES_IN_HOUR = 60;
    public static final String COPIED_PROMOTION_NEW_NAME_PREFIX = "**** Copy of ";
    public static final int COPIED_PROMOTION_NEW_NAME_MAX_LENGTH = 255;

    private PromotionMaintenanceService promotionMaintenanceService;
    private PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer;
    private JdbcBackOfficePromotionDao jdbcBackOfficePromotionDao;
    private PlayerIdReader playerIdReader;
    private final PromotionFormValidator dailyAwardPromotionFormValidator;
    private final PromotionFormValidator buyChipsPromotionFormValidator;
    private FtpOutboundFileSender ftpOutboundFileSender;
    private DailyAwardConfig defaultDailyAwardConfiguration;
    private String marketingPath;
    private String marketingWeb;
    private String assetUrl;
    private String marketingCdn;
    private Map<Platform, List<ChipPackage>> defaultPackages;

    @Autowired(required = true)
    public PromotionController(
            @Qualifier("promotionMaintenanceService") final PromotionMaintenanceService promotionMaintenanceService,
            final JdbcBackOfficePromotionDao jdbcBackOfficePromotionDao,
            @Qualifier("playerIdReader") final PlayerIdReader playerIdReader,
            final PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer,
            @Qualifier("dailyAwardPromotionFormValidator") final PromotionFormValidator dailyAwardPromotionFormValidator,
            @Qualifier("buyChipsPromotionFormValidator") final PromotionFormValidator buyChipsPromotionFormValidator,
            @Qualifier("ftpOutboundFileSender") final FtpOutboundFileSender ftpOutboundFileSender) {
        this.promotionMaintenanceService = promotionMaintenanceService;
        this.jdbcBackOfficePromotionDao = jdbcBackOfficePromotionDao;
        this.playerIdReader = playerIdReader;
        this.paymentOptionsToChipPackageTransformer = paymentOptionsToChipPackageTransformer;
        this.dailyAwardPromotionFormValidator = dailyAwardPromotionFormValidator;
        this.buyChipsPromotionFormValidator = buyChipsPromotionFormValidator;
        this.ftpOutboundFileSender = ftpOutboundFileSender;
    }

    @Value("${senet.path.marketing}")
    public void setMarketingPath(final String marketingPath) {
        this.marketingPath = marketingPath;
    }

    @Value("${senet.web.marketing}")
    public void setMarketingWeb(final String marketingWeb) {
        this.marketingWeb = marketingWeb;
    }

    @Value("${strata.server.lobby.ssl.content}")
    public void setAssetUrl(final String assetUrl) {
        this.assetUrl = assetUrl;
    }

    @Value("${senet.cdn.marketing}")
    public void setMarketingCdn(final String marketingCdn) {
        this.marketingCdn = marketingCdn;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor("yyyy-MM-dd", true));
    }

    @ModelAttribute("platforms")
    public Map<Platform, String> populatePlatforms() {
        final Map<Platform, String> platforms = new LinkedHashMap<Platform, String>();
        platforms.put(Platform.WEB, "Web");
        platforms.put(Platform.IOS, "iOS");
        platforms.put(Platform.FACEBOOK_CANVAS, "Facebook");
        platforms.put(Platform.ANDROID, "Android");
        return platforms;
    }

    @ModelAttribute("getDefaultPackages")
    public Map<Platform, List<ChipPackage>> populateDefaultPackages() {
        return getDefaultPackages();
    }

    private Map<Platform, List<ChipPackage>> getDefaultPackages() {
        return paymentOptionsToChipPackageTransformer.getDefaultPackages();
    }

    @ModelAttribute("playerOptions")
    public Map<String, String> populatePlayerOptions() {
        final Map<String, String> searchOptions = new LinkedHashMap<String, String>();
        searchOptions.put("ALL", "all");
        searchOptions.put("SELECTED", "selected");
        return searchOptions;
    }

    @ModelAttribute("searchForm")
    public SearchForm createDefaultSearchForm() {
        final SearchForm searchForm = new SearchForm();
        searchForm.setSearchType(PromotionSearchType.LIVE);
        searchForm.setPromotionType(PromotionType.DAILY_AWARD);
        final Map<PromotionSearchType, String> searchTypes = new LinkedHashMap<PromotionSearchType, String>();
        searchTypes.put(PromotionSearchType.LIVE, "Live");
        searchTypes.put(PromotionSearchType.ARCHIVED, "Archived");
        searchForm.setSearchTypes(searchTypes);
        final Map<PromotionType, String> promotionTypes = new LinkedHashMap<PromotionType, String>();
        for (PromotionType promotionType : PromotionType.values()) {
            promotionTypes.put(promotionType, promotionType.getDisplayName());
        }
        searchForm.setPromotionTypes(promotionTypes);
        return searchForm;
    }

    @ModelAttribute("hours")
    public Map<String, String> addHoursToModel() {
        final Map<String, String> hours = new LinkedHashMap<String, String>();
        for (int i = 0; i < HOURS_IN_DAY; i++) {
            hours.put("" + i, String.format("%02d", i));
        }
        return hours;
    }

    @ModelAttribute("minutes")
    public Map<String, String> addMinutesToModel() {
        final Map<String, String> minutes = new LinkedHashMap<String, String>();
        for (int i = 0; i < MINUTES_IN_HOUR; i++) {
            minutes.put("" + i, String.format("%02d", i));
        }
        return minutes;
    }

    @RequestMapping(value = {"promotion/list"}, method = RequestMethod.GET)
    public String listPromotions(final ModelMap model) {
        final SearchForm searchForm = (SearchForm) model.get("searchForm");
        findPromotionsAndAddToModel(model, searchForm);
        return LIST_VIEW;
    }

    @RequestMapping(value = "promotion/list", params = "search", method = RequestMethod.POST)
    public String listPromotions(@ModelAttribute("searchForm") final SearchForm searchForm, final ModelMap model) {
        findPromotionsAndAddToModel(model, searchForm);
        return LIST_VIEW;
    }

    @RequestMapping(value = "promotion/list", params = "create", method = RequestMethod.POST)
    public String createPromotion(@ModelAttribute("searchForm") final SearchForm searchForm, final Model model) {
        final PromotionForm promotionForm = PromotionFormFactory.createDefaultForm(searchForm.getPromotionType(), getDefaultDailyAward());
        if (searchForm.getPromotionType() == PromotionType.BUY_CHIPS) {
            PromotionFormFactory.applyDefaultsPackages(getDefaultPackages(), (BuyChipsPromotionForm) promotionForm);
        }
        model.addAttribute("promotionForm", promotionForm);
        addTypeSpecificModelAttrs(model, searchForm.getPromotionType());
        return EDIT_VIEW;
    }

    private void findPromotionsAndAddToModel(final ModelMap model, final SearchForm searchForm) {
        final PromotionType promotionType = searchForm.getPromotionType();
        final List<Promotion> promotions = jdbcBackOfficePromotionDao.find(searchForm.getSearchType(), promotionType);
        model.addAttribute("promotions", promotions);
    }

    @RequestMapping(value = {"promotion/edit"}, method = RequestMethod.GET)
    public String editPromotion(final Model model, @RequestParam final Long promotionId) {
        final Promotion promotion = jdbcBackOfficePromotionDao.findById(PromotionSearchType.LIVE, promotionId);
        final PromotionForm promotionForm = PromotionFormFactory.createPromotionForm(promotion);
        if (promotion.getPromotionType() == PromotionType.BUY_CHIPS) {
            PromotionFormFactory.applyDefaultsPackages(getDefaultPackages(), (BuyChipsPromotionForm) promotionForm);
            PromotionFormFactory.applyOverridesFromPromotionToForm((BuyChipsPromotion) promotion, (BuyChipsPromotionForm) promotionForm, getDefaultPackages());
        }
        model.addAttribute("promotionForm", promotionForm);
        addTypeSpecificModelAttrs(model, promotion.getPromotionType());
        return EDIT_VIEW;
    }

    @RequestMapping(value = {"promotion/copy"}, method = RequestMethod.GET)
    public String copyPromotion(@RequestParam final Long promotionId) throws IOException {
        final Promotion promotion = jdbcBackOfficePromotionDao.findById(PromotionSearchType.LIVE, promotionId);
        setCopiedPromotionAttributes(promotion);
        final PromotionForm promotionForm = PromotionFormFactory.createPromotionForm(promotion);
        if (promotion.getPromotionType() == PromotionType.BUY_CHIPS) {
            PromotionFormFactory.applyDefaultsPackages(getDefaultPackages(), (BuyChipsPromotionForm) promotionForm);
            PromotionFormFactory.applyOverridesFromPromotionToForm((BuyChipsPromotion) promotion, (BuyChipsPromotionForm) promotionForm, getDefaultPackages());
        }
        persistPromotion(promotionForm);
        return LIST_REDIRECT;
    }

    protected void setCopiedPromotionAttributes(final Promotion promotion) {
        promotion.setId(null);
        promotion.setName(getCopiedPromotionNewName(promotion.getName()));
        promotion.setControlGroupFunction(ControlGroupFunctionType.EXTERNAL_ID);
        if (promotion.isActive()) {
            movePromotionIntoFuture(promotion);
        }
    }

    private void movePromotionIntoFuture(final Promotion promotion) {
        final Days lengthOfPromotionInDays = Days.daysBetween(promotion.getStartDate(), promotion.getEndDate());
        promotion.setStartDate(promotion.getStartDate().plusDays(lengthOfPromotionInDays.getDays() + 1));
        promotion.setEndDate(promotion.getEndDate().plusDays(lengthOfPromotionInDays.getDays() + 1));
    }

    protected String getCopiedPromotionNewName(final String copiedPromotionName) {
        final String promotionNewName = (COPIED_PROMOTION_NEW_NAME_PREFIX + copiedPromotionName);
        if (promotionNewName.length() > COPIED_PROMOTION_NEW_NAME_MAX_LENGTH) {
            return promotionNewName.substring(0, COPIED_PROMOTION_NEW_NAME_MAX_LENGTH);
        }
        return promotionNewName;
    }

    @RequestMapping(value = {"promotion/saveDAILY_AWARD", "promotion/savePROGRESSIVE_DAY_*"}, method = RequestMethod.POST)
    public String savePromotion(final Model model,
                                @ModelAttribute("promotionForm") final DailyAwardPromotionForm promotionForm,
                                final BindingResult binder) throws IOException {
        return processForm(model, promotionForm, binder, dailyAwardPromotionFormValidator);
    }

    @RequestMapping(value = {"promotion/saveBUY_CHIPS"}, method = RequestMethod.POST)
    public String savePromotion(final Model model,
                                @ModelAttribute("promotionForm") final BuyChipsPromotionForm promotionForm,
                                final BindingResult binder) throws IOException {
        return processForm(model, promotionForm, binder, buyChipsPromotionFormValidator);
    }

    private String processForm(final Model model,
                               final PromotionForm promotionForm,
                               final BindingResult binder,
                               final PromotionFormValidator validator) throws IOException {

        validator.validate(promotionForm, binder);
        if (binder.hasErrors()) {
            model.addAttribute("promotionForm", promotionForm);
            addTypeSpecificModelAttrs(model, promotionForm.getPromotionType());
            return EDIT_VIEW;
        } else {
            persistPromotion(promotionForm);
            return LIST_REDIRECT;
        }
    }

    private void persistPromotion(final PromotionForm form) throws IOException {
        Promotion promotion = null;
        try {
            promotion = uploadImagesAndBuildPromotion(form);
            if (PromotionType.BUY_CHIPS == form.getPromotionType()) {
                ((BuyChipsPromotionForm) form).updateConfigurationWithOverriddenChipAmounts((BuyChipsPromotion) promotion, getDefaultPackages());
            }
            boolean isNewPromotion = promotion.getId() == null;
            if (isNewPromotion) {
                promotionMaintenanceService.create(promotion);
            } else {
                promotionMaintenanceService.update(promotion);
            }
        } catch (final RuntimeException e) {
            LOG.error(String.format("Failed to processForm promotion[%s]", promotion), e);
            throw e;
        }
    }

    private Promotion uploadImagesAndBuildPromotion(PromotionForm form) throws IOException {
        uploadImagesAndUpdateUrlsInForm(form); // NOTE: this method modifies the form data and so it must be called before form.buildPromotion
        return form.buildPromotion();
    }

    private void uploadImagesAndUpdateUrlsInForm(PromotionForm promotionForm) throws IOException {
        if (PromotionType.DAILY_AWARD == promotionForm.getPromotionType()
                || promotionForm.getPromotionType().isProgressive()) {
            saveImage(promotionForm.getMainImage(), getDefaultDailyAward().getMainImage());
            saveImage(promotionForm.getSecondaryImage(), getDefaultDailyAward().getSecondaryImage());
            saveImage(promotionForm.getIosImage(), getDefaultDailyAward().getIosImage());
            saveImage(promotionForm.getAndroidImage(), getDefaultDailyAward().getAndroidImage());
        } else if (PromotionType.BUY_CHIPS == promotionForm.getPromotionType()) {
            saveImage(promotionForm.getMainImage(), "");
            saveImage(promotionForm.getSecondaryImage(), "");
            saveImage(promotionForm.getIosImage(), "");
            saveImage(promotionForm.getAndroidImage(), "");
        }
    }

    private void saveImage(final ImageForm imageForm, final String defaultImageUrl) throws IOException {
        if ("upload".equals(imageForm.getImageType())) {
            final MultipartFile imageFile = imageForm.getImageFile();
            if (imageFile != null && StringUtils.isNotBlank(imageFile.getOriginalFilename())) {
                if (anImageServedFromCDN()) {
                    transferFileToCDN(imageFile);
                    imageForm.setImageUrl(marketingCdn + "/" + imageFile.getOriginalFilename());
                } else {
                    final File upLoadFile = new File(marketingPath + "/" + imageFile.getOriginalFilename());
                    imageFile.transferTo(upLoadFile);
                    imageForm.setImageUrl(marketingWeb + "/" + imageFile.getOriginalFilename());
                }
            }
        } else if ("default".equals(imageForm.getImageType()) && StringUtils.isNotBlank(imageForm.getImageUrl())) {
            imageForm.setImageUrl(defaultImageUrl);
        }
    }

    private boolean anImageServedFromCDN() {
        return (!marketingCdn.isEmpty());
    }

    private void transferFileToCDN(final MultipartFile imageFile) {
        OutputStream outputStream = null;
        File upLoadFile = null;
        try {
            upLoadFile = new File(imageFile.getOriginalFilename());
            outputStream = new FileOutputStream(upLoadFile);
            outputStream.write(imageFile.getBytes());
            ftpOutboundFileSender.transfer(upLoadFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                LOG.warn("An exception occurred while transferring file to CDN", e);
            }
            try {
                if (upLoadFile != null) {
                    if (!upLoadFile.delete()) {
                        LOG.warn("Unable to delete upload file: " + upLoadFile.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                LOG.warn("An exception occurred while deleting upload file", e);
            }
        }

    }

    private void addTypeSpecificModelAttrs(final Model model, final PromotionType type) {
        if (PromotionType.DAILY_AWARD == type || type.isProgressive()) {
            addImageUrlsToModel(model);
            model.addAttribute("assetUrl", assetUrl);
        } else if (PromotionType.BUY_CHIPS == type) {
            model.addAttribute("assetUrl", assetUrl);
            addPaymentMethods(model);
            model.addAttribute("defaultWebBuyChipPackages", getDefaultPackages().get(Platform.WEB));
            model.addAttribute("defaultIOSBuyChipPackages", getDefaultPackages().get(Platform.IOS));
            model.addAttribute("defaultFacebookBuyChipPackages", getDefaultPackages().get(Platform.FACEBOOK_CANVAS));
            model.addAttribute("defaultAndroidBuyChipPackages", getDefaultPackages().get(Platform.ANDROID));
        } else {
            LOG.error(String.format("Unknown Promotion Type:%s", type.toString()));
        }
    }

    private void addImageUrlsToModel(final Model model) {
        model.addAttribute("defaultMainImageUrl", getDefaultDailyAward().getMainImage());
        model.addAttribute("defaultSecondaryImageUrl", getDefaultDailyAward().getSecondaryImage());
        model.addAttribute("defaultIosImageUrl", getDefaultDailyAward().getIosImage());
        model.addAttribute("defaultAndroidImageUrl", getDefaultDailyAward().getAndroidImage());

    }

    @RequestMapping(value = {"promotion/showPlayers"}, method = RequestMethod.GET)
    public String showPlayers(final HttpSession session,
                              @RequestParam final PromotionSearchType searchType,
                              @RequestParam final Long promotionId,
                              @RequestParam(required = false) final Integer pageSize) {
        final Promotion promotion = jdbcBackOfficePromotionDao.findById(searchType, promotionId);
        final Integer playerCount = jdbcBackOfficePromotionDao.countPlayersInPromotion(searchType, promotionId);
        final PlayerPage playerPage = new PlayerPage();
        playerPage.setPromotion(promotion);
        if (pageSize == null) {
            playerPage.setPageSize(DEFAULT_PLAYER_PAGE_SIZE);
        } else {
            playerPage.setPageSize(pageSize);
        }
        playerPage.setPageNumber(1);
        final int offsetPages;
        if (playerCount % playerPage.getPageSize() > 0) {
            offsetPages = 1;
        } else {
            offsetPages = 0;
        }
        playerPage.setPagesAvailable(playerCount / playerPage.getPageSize() + offsetPages);
        playerPage.setPromotionPlayers(jdbcBackOfficePromotionDao.findPlayers(searchType, promotionId, 0, playerPage.getPageSize()));
        playerPage.setSearchType(searchType);
        session.setAttribute("page", playerPage);
        session.setAttribute("totalPlayers", playerCount);
        return SHOW_PLAYERS_VIEW;
    }

    @RequestMapping(value = {"promotion/nextPlayers"}, method = RequestMethod.GET)
    public String nextPlayers(final HttpSession session, @RequestParam final Integer pageNumber) {
        final PlayerPage page = (PlayerPage) session.getAttribute("page");
        page.setPromotionPlayers(jdbcBackOfficePromotionDao.findPlayers(page.getSearchType(), page.getPromotion().getId(),
                (pageNumber - 1) * page.getPageSize(), page.getPageSize()));
        page.setPageNumber(pageNumber);
        return SHOW_PLAYERS_VIEW;
    }

    @RequestMapping(value = {"promotion/addPlayers"}, method = RequestMethod.GET)
    public String addPlayers(@RequestParam final PromotionSearchType searchType, @RequestParam final Long promotionId,
                             final Model model, final HttpSession session) {
        final Promotion promotion = jdbcBackOfficePromotionDao.findById(searchType, promotionId);
        session.setAttribute("promotionAddPlayers", promotion);
        final PromotionPlayerUpload promotionPlayerUpload = new PromotionPlayerUpload();
        promotionPlayerUpload.setPromotionId(promotionId);
        model.addAttribute(promotionPlayerUpload);
        return ADD_PLAYERS_VIEW;
    }

    @RequestMapping(value = {"promotion/addPlayers"}, method = RequestMethod.POST)
    public String addPlayers(final HttpSession session,
                             final Model model,
                             final PromotionPlayerUpload promotionPlayerUpload,
                             final BindingResult binder) throws IOException {
        final Long promotionId = promotionPlayerUpload.getPromotionId();
        final String filename = promotionPlayerUpload.getFile().getOriginalFilename();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Uploading players from file[%s] to promotion[%s]", filename, promotionId));
        }
        if (filename.isEmpty()) {
            binder.rejectValue("file", "nofile", "Please choose player file to upload");
            return ADD_PLAYERS_VIEW;
        }
        if (promotionPlayerUpload.getFile().isEmpty()) {
            binder.rejectValue("file", "empty", new Object[]{filename}, "File, {0}, contained no player ids");
            return ADD_PLAYERS_VIEW;
        }


        try {
            PlayerReaderResult playerReaderResult = playerIdReader.readPlayerIds(promotionPlayerUpload.getFile().getInputStream());
            if (playerReaderResult.hasRpxErrors() || playerReaderResult.hasInputLineErrors()) {
                model.addAttribute("inputLineErrors", playerReaderResult.getInvalidInputLines());
                model.addAttribute("rpxIdsNotMatched", playerReaderResult.getNotMatched());
                model.addAttribute("rpxIdsMatchedToManyPlayers", playerReaderResult.getMultipleMatches());
            }
            promotionMaintenanceService.addPlayersTo(promotionId, playerReaderResult.getPlayerIds());
        } catch (final IOException e) {
            LOG.error(String.format("Failed to upload players to promotion[%s].", promotionId), e);
            binder.rejectValue("file", "playerUpload.error", new Object[]{filename},
                    "Failed to upload players from file {0} to promotion: " + e.getMessage());
            return ADD_PLAYERS_VIEW;
        } finally {
            promotionPlayerUpload.getFile().getInputStream().close();
        }

        session.removeAttribute("promotionAddPlayers");
        return showPlayers(session, PromotionSearchType.LIVE, promotionId, null);
    }

    private void addPaymentMethods(final Model model) {
        final Map<PaymentPreferences.PaymentMethod, String> paymentMethods =
                new LinkedHashMap<PaymentPreferences.PaymentMethod, String>();
        paymentMethods.put(PaymentPreferences.PaymentMethod.CREDITCARD, "Credit Card");
        paymentMethods.put(PaymentPreferences.PaymentMethod.PAYPAL, "PayPal");
        paymentMethods.put(PaymentPreferences.PaymentMethod.ITUNES, "iTunes");
        paymentMethods.put(PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT, "Google Checkout");
        model.addAttribute("allPaymentMethods", paymentMethods);
    }

    private DailyAwardConfig getDefaultDailyAward() {
        if (defaultDailyAwardConfiguration == null) {
            defaultDailyAwardConfiguration = promotionMaintenanceService.getDefaultDailyAwardConfiguration();
        }
        return defaultDailyAwardConfiguration;
    }


}
