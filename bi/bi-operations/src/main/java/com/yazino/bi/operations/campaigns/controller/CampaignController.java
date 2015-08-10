package com.yazino.bi.operations.campaigns.controller;

import com.yazino.bi.operations.campaigns.*;
import com.yazino.bi.operations.campaigns.model.CampaignPlayerUpload;
import com.yazino.engagement.ChannelType;
import com.yazino.platform.Platform;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import strata.server.operations.promotion.model.ChipPackage;
import strata.server.operations.promotion.service.PaymentOptionsToChipPackageTransformer;
import com.yazino.bi.operations.util.DateTimeEditor;
import com.yazino.bi.operations.controller.ControllerWithDateBinderAndFormatterAndReportFormat;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Controller
public class CampaignController extends ControllerWithDateBinderAndFormatterAndReportFormat {

    public static final String CAMPAIGN_DATA_MODEL = "campaigns";
    private static final Logger LOG = LoggerFactory.getLogger(CampaignController.class);
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm";
    public static final String CREATE_CAMPAIGN_VIEW = "campaigns/create";
    public static final String EDIT_CAMPAIGN_VIEW = "campaigns/edit";
    public static final String LIST_CAMPAIGNS_VIEW = "campaigns/display";

    private static final String ERROR_MESSAGES_KEY = "errorMessages";
    private static final String SUCCESS_MESSAGES_KEY = "successMessages";
    public static final String INFO_MAP_KEY = "infoMap";
    public static final List<Platform> SUPPORTED_PLATFORMS = asList(Platform.WEB, Platform.IOS, Platform.FACEBOOK_CANVAS, Platform.ANDROID, Platform.AMAZON);
    public static final int PAGE_SIZE = 35;
    public static final String ADD_PLAYERS_TO_CAMPAIGN = "campaigns/addPlayersToCampaign";

    private final CampaignScheduleWithNameDao campaignScheduleWithNameDao;
    private final OperationsCampaignService operationsCampaignService;
    private final CampaignFormValidator campaignFormValidator;
    private final PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer;
    private final CampaignPlayerUploadValidator campaignPlayerUploadValidator;


    public void setSupportedGameTypes(final Map<String, String> supportedGameTypes) {
        this.supportedGameTypes = supportedGameTypes;
    }

    @Resource
    private Map<String, String> supportedGameTypes = newHashMap();

    @Autowired
    public CampaignController(final CampaignScheduleWithNameDao campaignScheduleWithNameDao,
                              final OperationsCampaignService operationsCampaignService,
                              final CampaignFormValidator campaignFormValidator,
                              final CampaignPlayerUploadValidator campaignPlayerUploadValidator,
                              @Qualifier("defaultChipPackageService") final PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer) {
        this.campaignPlayerUploadValidator = campaignPlayerUploadValidator;
        Validate.notNull(campaignScheduleWithNameDao);
        Validate.notNull(operationsCampaignService);
        Validate.notNull(campaignFormValidator);
        Validate.notNull(operationsCampaignService);
        Validate.notNull(paymentOptionsToChipPackageTransformer);

        this.campaignFormValidator = campaignFormValidator;
        this.campaignScheduleWithNameDao = campaignScheduleWithNameDao;
        this.operationsCampaignService = operationsCampaignService;
        this.paymentOptionsToChipPackageTransformer = paymentOptionsToChipPackageTransformer;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor(DATE_TIME_FORMAT, true));
        binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat(DATE_TIME_FORMAT), true));
    }

    @RequestMapping("/campaigns")
    public ModelAndView listCampaigns(@RequestParam(defaultValue = "0", value = "firstRecord") final Integer firstRecord) {
        ModelAndView modelAndView = new ModelAndView(LIST_CAMPAIGNS_VIEW);
        final List<CampaignScheduleWithName> campaignList = campaignScheduleWithNameDao.getCampaignList(firstRecord, PAGE_SIZE, false);
        modelAndView.addObject(CAMPAIGN_DATA_MODEL, campaignList);
        setPaginationFields(firstRecord, modelAndView);

        return modelAndView;
    }

    @RequestMapping("/campaigns/{id}/disable/{page}")
    public ModelAndView disableCampaign(@PathVariable("id") long campaignId,
                                        @PathVariable("page") final Integer firstRecord) {
        operationsCampaignService.disable(campaignId);
        return listCampaigns(firstRecord);
    }

    private void setPaginationFields(final Integer firstRecord, final ModelAndView modelAndView) {
        modelAndView.addObject("pageSize", PAGE_SIZE);
        modelAndView.addObject("startPosition", firstRecord);
        modelAndView.addObject("totalSize", campaignScheduleWithNameDao.getCampaignRecordCount());
    }

    @RequestMapping("/campaigns/create")
    public String createCampaignView(final ModelMap model) {
        model.addAttribute(getDefaultValuesForForm());
        addDefaultBuyChipsPackage(model);
        return CREATE_CAMPAIGN_VIEW;
    }


    @RequestMapping(value = {"/campaigns/add"}, method = RequestMethod.POST)
    public ModelAndView addCampaign(@ModelAttribute("campaignForm") final CampaignForm campaignForm,
                                    BindingResult bindingResult) {
        LOG.debug("Received campaignForm {} to saveCampaign", campaignForm);

        ModelAndView modelAndView;
        campaignFormValidator.validate(campaignForm, bindingResult);
        if (bindingResult != null && bindingResult.hasErrors()) {
            LOG.debug("Validation failed with error {}", bindingResult.getAllErrors());
            modelAndView = createModelAndView(campaignForm, CREATE_CAMPAIGN_VIEW);
        } else {
            try {
                if (!isBlank(campaignForm.getPromotionType())) {
                    campaignForm.getCampaign().setPromo(Boolean.TRUE);
                }
                Long campaignId = operationsCampaignService.save(campaignForm);
                modelAndView = listCampaigns(0);
                modelAndView.addObject(SUCCESS_MESSAGES_KEY, "Successfully added campaign " + campaignId);
                modelAndView.addObject(INFO_MAP_KEY, campaignForm.toStringMap());
            } catch (Exception e) {
                LOG.error("Failed to saveCampaign campaign {}", campaignForm, e);
                modelAndView = createModelAndView(campaignForm, CREATE_CAMPAIGN_VIEW);
                modelAndView.addObject(ERROR_MESSAGES_KEY, "Couldn't add campaign " + campaignForm + ", error was " + e.getMessage());
            }
        }
        return modelAndView;
    }

    private ModelAndView createModelAndView(CampaignForm campaignForm, String viewName) {
        ModelAndView modelAndView = new ModelAndView(viewName);
        modelAndView.addObject(campaignForm);
        return modelAndView;
    }

    @RequestMapping("/campaigns/{id}/edit")
    public String editCampaignView(final ModelMap model, @PathVariable("id") long campaignId) {

        CampaignForm campaignForm = operationsCampaignService.getCampaignForm(campaignId);

        model.addAttribute(campaignForm);
        addDefaultBuyChipsPackage(model);

        return EDIT_CAMPAIGN_VIEW;
    }

    @RequestMapping("/campaigns/{id}/duplicate")
    public String duplicateCampaign(final ModelMap model, @PathVariable("id") long campaignId) {

        CampaignForm campaignForm = operationsCampaignService.getCampaignForm(campaignId);
        final Campaign campaign = campaignForm.getCampaign();
        campaign.getCampaignScheduleWithName().setCampaignId(-1L);
        campaign.getCampaignScheduleWithName().setName("COPY OF " + campaign.getName());
        model.addAttribute(campaignForm);
        addDefaultBuyChipsPackage(model);


        return EDIT_CAMPAIGN_VIEW;
    }


    @RequestMapping(value = {"/campaigns/edit"}, method = RequestMethod.POST)
    public ModelAndView editCampaign(final CampaignForm campaignForm, BindingResult bindingResult) {
        final Campaign campaign = campaignForm.getCampaign();
        LOG.debug("Received Campaign {}  to edit", campaign);
        campaignFormValidator.validate(campaignForm, bindingResult);
        ModelAndView modelAndView;

        if (bindingResult != null && bindingResult.hasErrors()) {
            LOG.debug("Validation failed with error {}", bindingResult.getAllErrors());
            modelAndView = createModelAndView(campaignForm, EDIT_CAMPAIGN_VIEW);
        } else {
            try {
                if (campaignForm.getCampaign().getCampaignId() == -1L) {
                    operationsCampaignService.save(campaignForm);
                } else {
                    operationsCampaignService.update(campaignForm);
                }
                modelAndView = listCampaigns(0);
                modelAndView.addObject(SUCCESS_MESSAGES_KEY, "Successfully updated campaign " + campaign.getCampaignId());
                modelAndView.addObject(INFO_MAP_KEY, campaignForm.toStringMap());
            } catch (Exception e) {
                LOG.error("Failed to update campaign ", e);
                modelAndView = createModelAndView(campaignForm, EDIT_CAMPAIGN_VIEW);
                modelAndView.addObject(ERROR_MESSAGES_KEY, "Couldn't update campaign " + campaignForm + ", error was " + e.getMessage());
            }
        }
        return modelAndView;
    }

    @RequestMapping(value = {"/campaigns/{id}/addPlayers"}, method = RequestMethod.GET)
    public ModelAndView addPlayers(@PathVariable("id") long campaignId) {
        final CampaignScheduleWithName campaignScheduleWithName = campaignScheduleWithNameDao.getCampaignScheduleWithName(campaignId);

        final ModelAndView modelAndView = createModelAndView(getDefaultValuesForForm(), ADD_PLAYERS_TO_CAMPAIGN);
        modelAndView.addObject("campaignSchedule", campaignScheduleWithName);
        modelAndView.addObject("campaignPlayerUpload", new CampaignPlayerUpload());
        return modelAndView;
    }

    @RequestMapping(value = {"/campaigns/addPlayers"}, method = RequestMethod.POST)
    public ModelAndView addPlayers(final CampaignPlayerUpload campaignPlayerUpload,
                                   BindingResult bindingResult) {
        ModelAndView modelAndView;

        campaignPlayerUploadValidator.validate(campaignPlayerUpload, bindingResult);
        if (bindingResult != null && bindingResult.hasErrors()) {
            LOG.debug("Validation failed with error {}", bindingResult.getAllErrors());
            modelAndView = addPlayers(campaignPlayerUpload.getCampaignId());
            modelAndView.addObject(campaignPlayerUpload);
        } else {
            try {
                Integer numberOfTargets = operationsCampaignService.addPlayersToCampaign(campaignPlayerUpload);
                modelAndView = listCampaigns(0);
                if (numberOfTargets == 0) {
                    bindingResult.reject("file.invalidTargets", "problem with format of csv file, since 0 targets were imported");
                    modelAndView.addObject(ERROR_MESSAGES_KEY, format("It seems that no players were added to the campaign check your csv format"));
                } else {
                    modelAndView.addObject(SUCCESS_MESSAGES_KEY, format("Successfully added players %s to campaign %s", numberOfTargets, campaignPlayerUpload.getCampaignId()));
                }
            } catch (IOException e) {
                LOG.error("problem processing CSV file  ", e);
                bindingResult.reject("file.invalid", "problem processing CSV file");
                modelAndView = addPlayers(campaignPlayerUpload.getCampaignId());
            }
        }
        return modelAndView;
    }

    @ModelAttribute("channelMap")
    public Map<String, String> createChannelMap() {
        final Map<String, String> platformMap = new LinkedHashMap<>();
        List<ChannelType> supportedChannels = CampaignHelper.getSupportedChannelList();

        for (ChannelType channel : supportedChannels) {
            platformMap.put(channel.toString(), channel.getDescription());
        }
        return platformMap;
    }

    @ModelAttribute("gameTypeMap")
    public Map<String, String> gameTypeMap() {
//        Map <String,String> supportedChannels = CampaignHelper.getSupportedGameList();
        return supportedGameTypes;
    }

    @ModelAttribute("platformMap")
    private Map<String, String> getPlatforms() {
        final Map<String, String> platformMap = new LinkedHashMap<>();

        for (Platform platform : SUPPORTED_PLATFORMS) {
            platformMap.put(platform.toString(), platform.toString());
        }
        return platformMap;
    }

    @ModelAttribute("promoTypes")
    public Map<String, String> getValidPromoTypes() {
        final Map<String, String> promoTypeMap = new LinkedHashMap<>();

        promoTypeMap.put("", "None");
        promoTypeMap.put("BUY_CHIPS", "Buy Chips");
        promoTypeMap.put("DAILY_AWARD", "Daily Award");
        promoTypeMap.put("GIFTING", "App to User Gifting");

        return promoTypeMap;
    }

    @ModelAttribute("gameSenders")
    public Map<String, String> getGameSenders() {
        final Map<String, String> promoTypeMap = new LinkedHashMap<>();

        promoTypeMap.put("", "Yazino");
        promoTypeMap.put("SLOTS", "Wheeldeal");
        promoTypeMap.put("BLACKJACK", "Blackjack");
        promoTypeMap.put("HIGH_STAKES", "High Stakes");
        promoTypeMap.put("TEXAS_HOLDEM", "Texas Hold'em");
        promoTypeMap.put("ROULETTE", "Roulette");

        return promoTypeMap;
    }

    @ModelAttribute("allPlayers")
    public Map<String, String> getValidAllPlayerTypes() {
        final Map<String, String> promoTypeMap = new LinkedHashMap<>();

        promoTypeMap.put("false", "Selective");
        promoTypeMap.put("true", "All Players");

        return promoTypeMap;
    }

    @ModelAttribute("pageSize")
    public Integer pageSizePrefill() {
        return PAGE_SIZE;
    }

    private CampaignForm getDefaultValuesForForm() {
        // todo add Dailyaward defaults
        return new CampaignForm(CampaignHelper.getDefaultCampaignForView(supportedGameTypes), CampaignHelper.getDefaultBuyChipsForm());
    }

    private void addDefaultBuyChipsPackage(final ModelMap model) {
        for (Platform platform : SUPPORTED_PLATFORMS) {
            model.addAttribute("defaultBuyChipPackages" + platform, getDefaultPackages().get(platform));
        }
    }

    @ModelAttribute("getDefaultPackages")
    public Map<Platform, List<ChipPackage>> populateDefaultPackages() {
        return getDefaultPackages();
    }

    private Map<Platform, List<ChipPackage>> getDefaultPackages() {
        return paymentOptionsToChipPackageTransformer.getDefaultPackages();
    }

    @ModelAttribute("allAvailablePercentages")
    public Map<BigDecimal, BigDecimal> getAllAvailablePercentages() {
        return CampaignHelper.getAllAvailableChipPercentages();
    }
}
