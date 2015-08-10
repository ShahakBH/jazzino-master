package com.yazino.web.domain;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class GameTypeMapperTest {

    private GameTypeMapper underTest = new GameTypeMapper();

    @Test
    public void aNullGameTypeIsReturnedAsNullView() {
        assertThat(underTest.getViewName(null), is(nullValue()));
    }

    @Test
    public void aNullViewIsReturnedAsNullGameType() {
        assertThat(underTest.fromViewName(null), is(nullValue()));
    }

    @Test
    public void slotsGameTypeMapsToWheelDealView() {
        assertThat(underTest.getViewName("SLOTS"), is(equalTo("wheelDeal")));
    }

    @Test
    public void wheelDealViewMapsToWheelSlotsGameType() {
        assertThat(underTest.fromViewName("wheelDeal"), is(equalTo("SLOTS")));
    }

    @Test
    public void aSingleWordGameTypeIsConvertedToALowerCaseView() {
        assertThat(underTest.getViewName("BLACKJACK"), is(equalTo("blackjack")));
    }

    @Test
    public void aSingleWordViewIsConvertedToAnUpperCaseGameType() {
        assertThat(underTest.fromViewName("blackjack"), is(equalTo("BLACKJACK")));
    }

    @Test
    public void underscoreSeparatedGameTypesAreConvertedToCamelCaseViews() {
        assertThat(underTest.getViewName("TEXAS_HOLDEM"), is(equalTo("texasHoldem")));
    }

    @Test
    public void camelCaseViewsAreConvertedToUnderscoreSeparatedGameTypes() {
        assertThat(underTest.fromViewName("texasHoldem"), is(equalTo("TEXAS_HOLDEM")));
    }
    
}
