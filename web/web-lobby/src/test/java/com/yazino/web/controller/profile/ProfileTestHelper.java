package com.yazino.web.controller.profile;

import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProfileTestHelper {

    public static void assertSelectTabIs(ModelAndView modelAndView, String tabName) {
        assertEquals(tabName, modelAndView.getModel().get("currentTab"));
    }

    public static void assertTemplateUsed(ModelAndView modelAndView, String expectedViewName) {
        assertEquals(expectedViewName, modelAndView.getViewName());
    }

    public static void assertNonPartialLayout(ModelAndView modelAndView) {
        assertTemplateUsed(modelAndView, "playerProfilePage");
        assertPartialFlagSetCorrectly(modelAndView, false);
    }

    public static void assertPartialLayout(ModelAndView modelAndView) {
        assertTemplateUsed(modelAndView, "partials/playerProfileMain");
        assertPartialFlagSetCorrectly(modelAndView, true);
    }

    public static void assertPartialFlagSetCorrectly(ModelAndView modelAndView, boolean expectedValue) {
        Object partialFlag = modelAndView.getModel().get("partial");
        boolean isTruthy = partialFlag != null && ((Boolean) partialFlag).equals(Boolean.TRUE);
        assertTrue(isTruthy == expectedValue);
    }

    public static void assertFullListOfTabsInModel(ModelAndView modelAndView) {
        ArrayList tabList = new ArrayList<ProfileTab>();
        tabList.add(new ProfileTab("profile", "Profile", "/player/profile"));
        tabList.add(new ProfileTab("invitations", "Invitation Statement", "/player/invitations"));
        tabList.add(new ProfileTab("buddyList", "Buddy List", "/player/buddyList"));
        assertEquals(tabList, modelAndView.getModel().get("allTabs"));
    }

}
