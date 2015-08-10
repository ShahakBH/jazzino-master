package com.yazino.platform;

import org.junit.Test;

import static com.yazino.platform.Partner.TANGO;
import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.Partner.parse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;


public class PartnerTest {
    @Test
    public void parseNullValueShouldReturnYazino() {
        assertThat(parse(null), is(equalTo(YAZINO)));
    }

    @Test
    public void parseYazinoShouldReturnYazino() {
        assertThat(parse("YAZINO"), is(equalTo(YAZINO)));
    }

    @Test
    public void parseBlahShouldReturnYazino() {
        assertThat(parse("BLAH"), is(equalTo(YAZINO)));
    }

    @Test
    public void parseTangoShouldReturnTango() {
        assertThat(parse("TANGO"), is(equalTo(TANGO)));
    }

}
