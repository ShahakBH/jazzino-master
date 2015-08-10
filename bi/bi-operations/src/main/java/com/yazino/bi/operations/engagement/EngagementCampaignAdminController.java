package com.yazino.bi.operations.engagement;

import com.yazino.engagement.ChannelType;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import com.yazino.bi.operations.util.TargetCsvReader;
import com.yazino.bi.operations.util.DateTimeEditor;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.lang.Validate.notNull;

/**
 * Controller for the facebook App request admin application
 * management of facebook apprequests.
 */

@Controller
public class EngagementCampaignAdminController {
    private static final Logger LOG = LoggerFactory.getLogger(EngagementCampaignAdminController.class);
    static final int TARGET_PAGE_SIZE = 5000;

    private final EngagementCampaignValidator validator;

    private final EngagementCampaignDao dao;
    private final TargetCsvReader reader;

    public static final String LIST_VIEW = "/engagementCampaign/list";
    public static final String LIST_REDIRECT_VIEW = "redirect:/engagementCampaign";

    @Resource
    private Map<ChannelType, GameTypeValidator> gameTypeValidatorMap;

    @Autowired(required = true)
    public EngagementCampaignAdminController(final EngagementCampaignDao engagementCampaignDao,
                                             final EngagementCampaignValidator engagementCampaignValidator,
                                             final TargetCsvReader reader) {
        notNull(engagementCampaignDao);
        notNull(engagementCampaignValidator);
        notNull(reader);

        this.dao = engagementCampaignDao;
        this.validator = engagementCampaignValidator;
        this.reader = reader;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor("yyyy/MM/dd HH:mm", true));
    }

    @RequestMapping(value = {"/engagementCampaign"}, method = RequestMethod.GET)
    public String list(final ModelMap model) {
        model.addAttribute(new EngagementCampaign());
        addEngagementCampaignsToModel(model);
        return LIST_VIEW;
    }

    @RequestMapping(value = {"/engagementCampaign"}, method = RequestMethod.POST)
    public String create(final ModelMap model,
                         final EngagementCampaign engagementCampaign,
                         final BindingResult binder) {
        validator.validate(engagementCampaign, binder);
        if (binder.hasErrors()) {
            addEngagementCampaignsToModel(model);
            return LIST_VIEW;
        }
        dao.create(engagementCampaign);
        return LIST_REDIRECT_VIEW;
    }

    @RequestMapping(value = {"engagementCampaign/delete/{id}"}, method = {RequestMethod.DELETE, RequestMethod.GET})
    public String delete(final ModelMap model, @PathVariable("id") final Integer id) {
        final EngagementCampaign toBeDeleted = dao.findById(id);
        model.addAttribute("msg", "'" + toBeDeleted.getDescription() + "'" + " has been deleted.");
        dao.delete(id);
        return LIST_REDIRECT_VIEW;
    }

    @RequestMapping(value = {"engagementCampaign/addTargets"}, method = {RequestMethod.POST})
    public String addTargets(final ModelMap model,
                             final AppRequestTargetUpload upload,
                             final BindingResult binder) throws IOException {
        final Integer engagementCampaignId = upload.getId();
        final String filename = upload.getFile().getOriginalFilename();


        if (filename.isEmpty()) {
            binder.rejectValue("file", "nofile", "Please choose file to upload");
            return LIST_REDIRECT_VIEW;
        }

        if (upload.getFile().isEmpty()) {
            binder.rejectValue("file", "empty", new Object[]{filename}, "File, {0}, contained no targets");
            return LIST_REDIRECT_VIEW;
        }

        try {
            final Set<AppRequestTarget> targets = reader.readTargets(upload.getFile().getInputStream());
            final GameTypeValidator gameTypeValidator = gameTypeValidatorMap.get(upload.getChannelType());

            if (gameTypeValidator != null) {
                gameTypeValidator.validate(targets, binder);
            } else {
                LOG.error("No validator for :" + upload.getChannelType().name());
            }

            if (binder.hasErrors()) {
                LOG.error("invalid GameType:" + targets);
            }

            dao.addAppRequestTargets(engagementCampaignId, new ArrayList<AppRequestTarget>(targets));
        } catch (IOException e) {
            LOG.error(String.format("Failed to upload targets to notification id [%s].", engagementCampaignId), e);
            binder.rejectValue("file", "upload.error", new Object[]{filename},
                    "Failed to upload targets from file {0} to notification id: " + e.getMessage());
        } finally {
            upload.getFile().getInputStream().close();
        }


        addEngagementCampaignsToModel(model);
        return LIST_REDIRECT_VIEW;
    }

    @RequestMapping(value = {"engagementCampaign/showTargets/{engagementCampaignId}"}, method = RequestMethod.GET)
    public String showTargets(final ModelMap model,
                              @PathVariable("engagementCampaignId") final Integer engagementCampaignId,
                              @RequestParam final Integer pageNumber) {
        model.addAttribute(new EngagementCampaign());

        model.addAttribute("pageNumber", pageNumber);
        model.addAttribute("pagesAvailable", (dao.getTargetCountById(engagementCampaignId) / TARGET_PAGE_SIZE) + 1);

        final List<AppRequestTarget> targets = dao.findAppRequestTargetsById(
                engagementCampaignId,
                (pageNumber - 1) * TARGET_PAGE_SIZE,
                TARGET_PAGE_SIZE);
        model.addAttribute("targets", targets);
        model.addAttribute("engagementCampaignId", engagementCampaignId);
        model.addAttribute(new EngagementCampaign());
        addEngagementCampaignsToModel(model);

        return LIST_VIEW;
    }

    private void addEngagementCampaignsToModel(final ModelMap model) {
        final List<EngagementCampaign> engagementCampaigns = dao.findAll();
        model.addAttribute("engagementCampaigns", engagementCampaigns);
    }

    @ModelAttribute("platformMap")
    public Map<String, String> createPlatformMap() {
        final Map<String, String> platformMap = new LinkedHashMap<String, String>();
        platformMap.put(ChannelType.FACEBOOK_APP_TO_USER_REQUEST.toString(), ChannelType.FACEBOOK_APP_TO_USER_REQUEST.getDescription());
        platformMap.put(ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION.toString(), ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION.getDescription());
        return platformMap;
    }

    public void setGameTypeValidatorMap(final Map<ChannelType, GameTypeValidator> gameTypeValidatorMap) {
        this.gameTypeValidatorMap = gameTypeValidatorMap;
    }
}
