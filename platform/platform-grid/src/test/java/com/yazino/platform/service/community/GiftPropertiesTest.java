package com.yazino.platform.service.community;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GiftPropertiesTest {
    private static final DateTimeZone NEW_YORK = DateTimeZone.forID("America/New_York");
    private static final DateTimeZone LONDON = DateTimeZone.forID("Europe/London");

    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private GiftProperties underTest;

    @Before
    public void setUp() {
        underTest = new GiftProperties(yazinoConfiguration);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void serviceCannotBeCreatedWithANullConfiguration() {
        new GiftProperties(null);
    }

    @Test
    public void regiftCooldownUsesTheRegiftProperty() {
        when(yazinoConfiguration.getInt("strata.gifting.regiftcooldownperiod", 86400)).thenReturn(123);

        final int cooldownPeriod = underTest.regiftCooldownPeriodInSeconds();

        assertThat(cooldownPeriod, is(equalTo(123)));
    }

    @Test
    public void expiryTimeUsesTheExpiryTimeProperty() {
        when(yazinoConfiguration.getInt("strata.gifting.expiryOfGiftInHours", 96)).thenReturn(678);

        final int expiryTime = underTest.expiryTimeInHours();

        assertThat(expiryTime, is(equalTo(678)));
    }


    @Test
    public void giftRetentionUsesTheRetentionProperty() {
        when(yazinoConfiguration.getInt("gifting.retention-hours", 168)).thenReturn(234);

        final int retentionHours = underTest.retentionInHours();

        assertThat(retentionHours, is(equalTo(234)));
    }

    @Test
    public void maxGiftsUsesTheMaxGiftsProperty() {
        when(yazinoConfiguration.getInt("strata.gifting.maxGiftsPerDay", 25)).thenReturn(999);

        final int maxGifts = underTest.maxGiftCollectionsPerDay();

        assertThat(maxGifts, is(equalTo(999)));
    }

    @Test
    public void giftsRemainingUsesTheMaxGiftsProperty() {
        when(yazinoConfiguration.getInt("strata.gifting.maxGiftsPerDay", 25)).thenReturn(3);

        final int remainingGifts = underTest.remainingGiftCollections(2);

        assertThat(remainingGifts, is(equalTo(1)));
    }

    @Test
    public void giftsRemainingReturnsZeroWhenCollectedIsMoreThanAvailable() {
        when(yazinoConfiguration.getInt("strata.gifting.maxGiftsPerDay", 25)).thenReturn(3);

        final int remainingGifts = underTest.remainingGiftCollections(5);

        assertThat(remainingGifts, is(equalTo(0)));
    }

    @Test
    public void getEndOfGiftPeriodShouldReturnCorrectEndOfGiftPeriodIfPlayerisBefore5Am() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2012, 10, 15, 1, 0).getMillis());
        when(yazinoConfiguration.getString("strata.gifting.timeZone", "America/New_York")).thenReturn("America/New_York");

        final DateTime actual = underTest.endOfGiftPeriod();

        assertThat(actual.withZone(LONDON), is(equalTo(new DateTime(2012, 10, 15, 0, 0, NEW_YORK).toDateTime(LONDON))));
    }

    @Test
    public void getEndOfGiftPeriodShouldReturnCorrectEndOfGiftPeriodIfPlayerPlayedAfter5AM() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2012, 10, 15, 14, 0).getMillis());
        when(yazinoConfiguration.getString("strata.gifting.timeZone", "America/New_York")).thenReturn("America/New_York");

        final DateTime actual = underTest.endOfGiftPeriod();

        assertThat(actual.withZone(LONDON), is(equalTo(new DateTime(2012, 10, 16, 0, 0, NEW_YORK).toDateTime(LONDON))));
    }

    @Test
    public void getStartOfGiftPeriodShouldReturnCorrectStartOfGiftPeriodIfPlayerIsBefore5Am() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2012, 10, 15, 1, 0).getMillis());
        when(yazinoConfiguration.getString("strata.gifting.timeZone", "America/New_York")).thenReturn("America/New_York");

        final DateTime actual = underTest.startOfGiftPeriod();

        assertThat(actual.withZone(LONDON), is(equalTo(new DateTime(2012, 10, 14, 0, 0, NEW_YORK).toDateTime(LONDON))));
    }

    @Test
    public void getStartOfGiftPeriodShouldReturnCorrectStartOfGiftPeriodIfPlayerIsAfter5am() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2012, 10, 15, 15, 0).getMillis());
        when(yazinoConfiguration.getString("strata.gifting.timeZone", "America/New_York")).thenReturn("America/New_York");

        final DateTime actual = underTest.startOfGiftPeriod();

        assertThat(actual.withZone(LONDON), is(equalTo(new DateTime(2012, 10, 15, 0, 0, NEW_YORK).toDateTime(LONDON))));
    }

}
