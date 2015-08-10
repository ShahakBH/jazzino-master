package com.yazino.web.controller.social;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class ResponseHelperTest extends AbstractSocialFlowTest {

    @Test
    public void responseBuilderOutputsDefaults() {
        ModelAndView mv = new ResponseHelper.ResponseBuilder().toModelAndView();
        assertEquals("partials/social-flow/layout", mv.getViewName());
        assertClassesMatch(mv, "start");
        assertFalse(mv.getModelMap().containsKey("pageHeader"));
        assertFalse(mv.getModelMap().containsKey("showPersonSelector"));
    }

    @Test
    public void responseBuilderCanAddMultipleClasses() {
        final ResponseHelper.ResponseBuilder rb = new ResponseHelper.ResponseBuilder();
        rb.withPageClass("abc").withPageClass("def");
        ModelAndView mv = rb.toModelAndView();
        assertClassesMatch(mv, "abc", "def", "start");
    }

    @Test
    public void responseBuilderDefaultsToPreSent() {
        ModelAndView mv = new ResponseHelper.ResponseBuilder().toModelAndView();
        assertEquals("preSend", mv.getModelMap().get("pageHeaderType"));
        assertEquals("start", mv.getModelMap().get("pageClasses"));
    }

    @Test
    public void responseBuilderCanBeSetToToPostSent() {
        ModelAndView mv = new ResponseHelper.ResponseBuilder()
                .withSentVariation()
                .toModelAndView();
        assertEquals("postSend", mv.getModelMap().get("pageHeaderType"));
        assertEquals("sent", mv.getModelMap().get("pageClasses"));
    }

    @Test
    public void responseBuilderCanAddSinglePageType() {
        final ResponseHelper.ResponseBuilder rb = new ResponseHelper.ResponseBuilder();
        rb.withPageType("invitation").withPageType("challenge");
        ModelAndView mv = rb.toModelAndView();
        assertEquals("challenge", mv.getModelMap().get("pageType"));
        assertClassesMatch(mv, "challenge", "start");

    }

    @Test
    public void responseBuilderCanTurnPersonSelectorOn() {
        final ResponseHelper.ResponseBuilder rb = new ResponseHelper.ResponseBuilder();
        ResponseHelper.ResponseBuilder rb2 = rb.withPersonSelector();
        ModelAndView mv = rb.toModelAndView();
        assertEquals(true, mv.getModelMap().get("showPersonSelector"));
        assertEquals(rb, rb2);
    }

    @Test
    public void responseBuilderCanIncludeProviders() {
        final ResponseHelper.ResponseBuilder rb = new ResponseHelper.ResponseBuilder();
        ArrayList<ResponseHelper.ProviderVO> myProviders = new ArrayList<ResponseHelper.ProviderVO>();
        ResponseHelper.ResponseBuilder rb2 = rb.withProviders(myProviders);
        ModelAndView mv = rb.toModelAndView();
        assertEquals(myProviders, mv.getModelMap().get("providers"));
        assertEquals(rb, rb2);
    }

    @Test
    public void responseBuilderCanIncludeCustomModelAttributes() {
        final ResponseHelper.ResponseBuilder rb = new ResponseHelper.ResponseBuilder();
        rb.withModelAttribute("abc", "def").withModelAttribute("ghi", "jkl");
        ModelAndView mv = rb.toModelAndView();
        assertEquals("def", mv.getModelMap().get("abc"));
        assertEquals("jkl", mv.getModelMap().get("ghi"));
    }

    @Test
    public void classCompairerWorks() {
        assertTrue(TestHelper.classesMatch("abc def", "abc", "def"));
        assertFalse(TestHelper.classesMatch("abcdef", "abc", "def"));
        assertFalse(TestHelper.classesMatch("abc def ghi", "abc", "def"));
    }

}
