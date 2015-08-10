package com.yazino.web.controller;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.ModelMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

public class MarketingControllerTest {
    private MarketingController underTest;
    private ModelMap modelMap;

    @Before
    public void setUp() {
        modelMap = new ModelMap();
        underTest = new MarketingController();
    }

    @Test
    public void jobsPageReturnsTheJobsView() {
        assertThat(underTest.jobs(), is(equalTo("jobs")));
    }

    @Test
    public void processAboutUsSetsPageNameToAboutUs() {
        final String viewName = underTest.processAboutUs(modelMap);
        final String expected = "about-us";
        assertEquals(expected, modelMap.get(MarketingController.PAGE_NAME));
        assertEquals("aboutUs", viewName);
    }

    @Test
    public void processContactUsSetsPageNameToContactUs() {
        final String viewName = underTest.processContactUs(modelMap);
        final String expected = "contact-us";
        assertEquals("contactUs", viewName);
        assertEquals(expected, modelMap.get(MarketingController.PAGE_NAME));
    }

    @Test
    public void managementPageHusseinSetsPersonToHusseinAndReturnsManagementView() {
        final String viewName = underTest.managementPageHussein(modelMap);
        final String expectedView = "management";
        final String expectedPerson = "hussein";
        assertEquals(expectedView, viewName);
        assertEquals(expectedPerson, modelMap.get(MarketingController.WHO));
    }

    @Test
    public void managementPageMelissaSetsPersonToMelissaAndReturnsManagementView() {
        final String viewName = underTest.managementPageMelissa(modelMap);
        final String expectedView = "management";
        final String expectedPerson = "melissa";
        assertEquals(expectedView, viewName);
        assertEquals(expectedPerson, modelMap.get(MarketingController.WHO));
    }

    @Test
    public void managementPageAlyssaSetsPersonToAlyssaAndReturnsManagementView() {
        final String viewName = underTest.managementPageAlyssa(modelMap);
        final String expectedView = "management";
        final String expectedPerson = "alyssa";
        assertEquals(expectedView, viewName);
        assertEquals(expectedPerson, modelMap.get(MarketingController.WHO));
    }

    @Test
    public void managementPageGuillaumeSetsPersonToGuillaumeAndReturnsManagementView() {
        final String viewName = underTest.managementPageGuillaume(modelMap);
        final String expectedView = "management";
        final String expectedPerson = "guillaume";
        assertEquals(expectedView, viewName);
        assertEquals(expectedPerson, modelMap.get(MarketingController.WHO));
    }

    @Test
    public void managementPageStephanSetsPersonToStephanAndReturnsManagementView() {
        final String viewName = underTest.managementPageStephan(modelMap);
        final String expectedView = "management";
        final String expectedPerson = "stephan";
        assertEquals(expectedView, viewName);
        assertEquals(expectedPerson, modelMap.get(MarketingController.WHO));
    }

    @Test
    public void managementPageJonSetsPersonToJohnAndReturnsManagementView() {
        final String viewName = underTest.managementPageJon(modelMap);
        final String expectedView = "management";
        final String expectedPerson = "john";
        assertEquals(expectedView, viewName);
        assertEquals(expectedPerson, modelMap.get(MarketingController.WHO));
    }

}
