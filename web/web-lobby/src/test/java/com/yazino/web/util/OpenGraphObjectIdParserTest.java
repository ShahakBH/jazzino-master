package com.yazino.web.util;

import org.junit.Test;

import static com.yazino.web.util.OpenGraphObjectIdParser.parse;
import static com.yazino.web.util.OpenGraphObjectIdParser.parseGameType;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class OpenGraphObjectIdParserTest {
    @Test
    public void shouldParseShortCodes() {
        assertThat(parse("hs_level_10").getPrefix(), is(equalTo("hs")));
        assertThat(parse("hs_level_10").getMiddle(), is(equalTo("level")));
        assertThat(parse("hs_level_10").getSuffix(), is(equalTo(10)));
        assertThat(parse("hs_credits").getMiddle(), is(equalTo("credits")));
    }

    @Test
    public void shouldParseLongCodes() {
        assertThat(parseGameType("HIGH_STAKES_credits").getPrefix(), is(equalTo("HIGH_STAKES")));
        assertThat(parseGameType("HIGH_STAKES_credits").getMiddle(), is(equalTo("credits")));

        assertThat(parseGameType("BLACKJACK_credits").getPrefix(), is(equalTo("BLACKJACK")));
        assertThat(parseGameType("BLACKJACK_credits").getMiddle(), is(equalTo("credits")));

        assertThat(parseGameType("HIGH_STAKES_level_10").getPrefix(), is(equalTo("HIGH_STAKES")));
        assertThat(parseGameType("BLACKJACK_level_10").getPrefix(), is(equalTo("BLACKJACK")));

        assertThat(parseGameType("HIGH_STAKES_level_10").getMiddle(), is(equalTo("level")));
        assertThat(parseGameType("HIGH_STAKES_level_10").getSuffix(), is(equalTo(10)));
    }
}
