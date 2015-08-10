package com.yazino.platform.service.community;

import com.yazino.configuration.YazinoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static com.yazino.platform.gifting.CollectChoice.GAMBLE;
import static com.yazino.platform.gifting.CollectChoice.TAKE_MONEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GiftGamblingServiceTest {
    private static final String STRATA_GIFTING_PROBABILITY_TAKE_MONEY = "strata.gifting.probability.TAKE_MONEY";
    private static final String STRATA_GIFTING_PROBABILITY_GAMBLE = "strata.gifting.probability.GAMBLE";
    private static final String VALID_PROBABILITY_STRING = "35:20 30:40 13:50 8:100 3:150 5:200 3:400 2:1000 1:2000";
    @Mock
    private YazinoConfiguration configuration;
    private GiftGamblingService underTest;
    private int reallyNotThatRandom = 10;

    @Before
    public void setUp() throws Exception {
        when(configuration.getString(anyString())).thenReturn("100:100");
        underTest = new GiftGamblingService(configuration) {
            @Override
            protected int getRandomPercent() {
                return reallyNotThatRandom;
            }
        };
    }

    @Test
    public void giftGamblingServiceShouldLoadProbabilitiesForGamble() {
        underTest.collectGift(GAMBLE);
        verify(configuration).getString(STRATA_GIFTING_PROBABILITY_GAMBLE);
    }

    @Test
    public void giftGamblingServiceShouldLoadProbabilitiesForTakeMoney() {

        underTest.collectGift(TAKE_MONEY);
        verify(configuration).getString(STRATA_GIFTING_PROBABILITY_TAKE_MONEY);
    }

    @Test
    public void takeMoneyShouldReturnValueGivenForTakeMoneyInConfig() {
        when(configuration.getString(STRATA_GIFTING_PROBABILITY_TAKE_MONEY)).thenReturn("100:666");
        final BigDecimal takeMoneyAmount = underTest.collectGift(TAKE_MONEY);
        assertThat(takeMoneyAmount, comparesEqualTo(BigDecimal.valueOf(666)));
    }

    @Test
    public void invalidProportionShouldThrowException() {
        when(configuration.getString(STRATA_GIFTING_PROBABILITY_GAMBLE)).thenReturn("50:666 49:666");
        try {
            underTest.collectGift(GAMBLE);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(equalTo("Probabilities did not add up to 100:99")));
        }

        when(configuration.getString(STRATA_GIFTING_PROBABILITY_GAMBLE)).thenReturn("50:666 51:666");
        try {
            underTest.collectGift(GAMBLE);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(equalTo("Probabilities did not add up to 100:101")));
        }

    }

    @Test
    public void probabilityStringShould() {
        when(configuration.getString(STRATA_GIFTING_PROBABILITY_GAMBLE)).thenReturn(VALID_PROBABILITY_STRING);
        assertThatRandomResultGives(0, 20L);
        assertThatRandomResultGives(1, 20L);
        assertThatRandomResultGives(34, 20L);
        assertThatRandomResultGives(35, 40L);
        assertThatRandomResultGives(64, 40L);
        assertThatRandomResultGives(65, 50L);
        assertThatRandomResultGives(77, 50L);
        assertThatRandomResultGives(78, 100L);
        assertThatRandomResultGives(85, 100L);
        assertThatRandomResultGives(86, 150L);
        assertThatRandomResultGives(88, 150L);
        assertThatRandomResultGives(89, 200L);
        assertThatRandomResultGives(93, 200L);
        assertThatRandomResultGives(94, 400L);
        assertThatRandomResultGives(96, 400L);
        assertThatRandomResultGives(98, 1000L);
        assertThatRandomResultGives(99, 2000L);
    }

    @Test
    public void randomReturnShouldMatchExpectation() {
        Long total = 0L;
        for (int i = 0; i < 100; i++) {
            reallyNotThatRandom = 0;
            total += underTest.collectGift(GAMBLE).longValue();
        }
        assertThat(total / 100, is(100L));
    }

    private void assertThatRandomResultGives(int random, long result) {
        when(configuration.getString(STRATA_GIFTING_PROBABILITY_GAMBLE)).thenReturn(VALID_PROBABILITY_STRING);
        reallyNotThatRandom = random;
        assertThat(underTest.collectGift(GAMBLE).longValue(), is(result));
    }

    @Test
    public void gambleShouldNotCacheDifferentProbabilities() {
        when(configuration.getString(STRATA_GIFTING_PROBABILITY_GAMBLE)).thenReturn(VALID_PROBABILITY_STRING);
        assertThat(underTest.collectGift(GAMBLE).longValue(), is(20L));

        when(configuration.getString(STRATA_GIFTING_PROBABILITY_GAMBLE)).thenReturn("100:100");
        assertThat(underTest.collectGift(GAMBLE).longValue(), is(100L));

        assertThat(underTest.getProbabilitiesCache().get("100:100")[99], is(100L));
        assertThat(underTest.getProbabilitiesCache().get(VALID_PROBABILITY_STRING)[99], is(2000L));
    }
}
