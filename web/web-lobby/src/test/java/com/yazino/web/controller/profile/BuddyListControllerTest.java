package com.yazino.web.controller.profile;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class BuddyListControllerTest {


    private BuddyListController underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new BuddyListController(null);

    }


    @Test
    public void shouldReturnModelAndView() {
        final ModelAndView buddyList = underTest.getBuddyList(false);
        assertThat(buddyList.getViewName(), is(equalTo("playerProfilePage")));
        assertTrue(buddyList.getModel().containsKey("allTabs"));
        assertTrue(buddyList.getModel().containsKey("currentTab"));
        assertThat(((String) buddyList.getModel().get("currentTab")), Matchers.is(equalTo("buddyList")));
    }
}
