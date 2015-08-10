package com.yazino.web.domain.facebook;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FacebookLocaleParserTest {

    private FacebookLocaleParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new FacebookLocaleParser();
    }

    @Test
    public void shouldNotRecogniseCountryFromNullString() {
        assertEquals("US", parser.parseCountry(null));
    }

    @Test
    public void shouldNotRecogniseCountryFromEmptyString() {
        assertEquals("US", parser.parseCountry(""));
    }

    @Test
    public void shouldRecogniseCountryFromLanguageOnly() {
        assertEquals("US", parser.parseCountry("pt"));
    }

    @Test
    public void shouldNotRecogniseCountryWithInvalidIso() {
        assertEquals("US", parser.parseCountry("en_XX"));
    }

    @Test
    public void shouldRecogniseCountryFromLocale() {
        assertEquals("BR", parser.parseCountry("pt_BR"));
    }

    @Test
    public void shouldRecogniseCountryFromLocaleWithVariant() {
        assertEquals("TH", parser.parseCountry("th_TH_TR"));
    }
}
