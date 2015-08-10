package com.yazino.web.controller.social;

import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.Assert.assertTrue;

public abstract class AbstractSocialFlowTest {
    void assertClassesMatch(final ModelAndView modelAndView, String... expectedClasses) {
        assertClassesMatch(modelAndView.getModelMap(), expectedClasses);
    }

    void assertClassesMatch(final ModelMap model, String... expectedClasses) {
        assertTrue(TestHelper.classesMatch(model.get("pageClasses").toString(), expectedClasses));
    }
}
