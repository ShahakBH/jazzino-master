package com.yazino.test.logback;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

public class ListAppenderTest {
    private static final Logger LOG = LoggerFactory.getLogger(ListAppenderTest.class);

    @Test
    public void shouldAddLogMessageToListAppender() {
        ListAppender listAppender = ListAppender.addAppenderTo(ListAppenderTest.class);

        LOG.error("sample error");

        assertThat(listAppender.getMessages(), hasItem(containsString("sample error")));
    }
}

