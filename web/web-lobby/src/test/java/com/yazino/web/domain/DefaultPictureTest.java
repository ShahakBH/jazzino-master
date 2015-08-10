package com.yazino.web.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultPictureTest {

    @Test
    public void handlesNullPath() {
        DefaultPicture picture = new DefaultPicture("content", null);
        assertEquals("content", picture.getUrl());
    }

    @Test(expected = NullPointerException.class)
    public void requiresContentUrl() {
        new DefaultPicture(null, "somePath");
    }

    @Test
    public void generatesUrlByConcatenatingContentUrlAndAvatarPath() {
        assertEquals("ab", new DefaultPicture("a", "b").getUrl());
    }

}
