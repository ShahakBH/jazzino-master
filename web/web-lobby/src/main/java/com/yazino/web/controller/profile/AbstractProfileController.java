package com.yazino.web.controller.profile;

import com.yazino.configuration.YazinoConfiguration;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;

public abstract class AbstractProfileController {

    public static final String PARTIAL_URI_FLAG = "partial";
    public static final String PARTIAL_URI_FLAG_DEFAULT = "false";
    public static final boolean PARTIAL_URI_FLAG_IS_REQUIRED = false;

    private static final String STATEMENT_ENABLED_CONFIG_KEY = "strata.web.statement-enabled";

    private final YazinoConfiguration yazinoConfiguration;

    protected AbstractProfileController(final YazinoConfiguration yazinoConfiguration) {
        this.yazinoConfiguration = yazinoConfiguration;
    }

    public static class ModelKey {
        public static final String CURRENT_TAB = "currentTab";
    }

    private String getViewName(final boolean isPartial) {
        if (isPartial) {
            return "partials/playerProfileMain";
        }
        return "playerProfilePage";
    }

    protected void setupDefaultModel(final ModelMap modelMap) {
        final ArrayList<ProfileTab> allTabs = new ArrayList<ProfileTab>();
        allTabs.add(new ProfileTab(PlayerProfileController.TAB_CODE_NAME, "Profile", "/player/profile"));
        allTabs.add(new ProfileTab(InvitationStatementController.TAB_CODE_NAME, "Invitation Statement", "/player/invitations"));
        allTabs.add(new ProfileTab(BuddyListController.TAB_CODE_NAME, "Buddy List", "/player/buddyList"));

        modelMap.addAttribute("allTabs", allTabs);
        modelMap.addAttribute(ModelKey.CURRENT_TAB, getCurrentTab());
    }

    private void setPartialFlag(final ModelMap modelMap, final boolean isPartial) {
        if (isPartial) {
            modelMap.addAttribute("partial", true);
        }
    }

    protected ModelAndView getInitialisedModelAndView(final boolean isPartial) {
        final ModelAndView modelAndView = new ModelAndView(getViewName(isPartial));
        setPartialFlag(modelAndView.getModelMap(), isPartial);
        return modelAndView;
    }

    abstract String getCurrentTab();

}
