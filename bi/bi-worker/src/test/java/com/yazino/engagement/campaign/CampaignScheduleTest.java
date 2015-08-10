package com.yazino.engagement.campaign;

import com.yazino.bi.campaign.domain.CampaignSchedule;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class CampaignScheduleTest {
    public static final DateTime NEXT_RUN_TS = new DateTime(100l);
    public static final Long CAMPAIGN_ID = 1l;
    public static final Long RUN_HOURS = 168l;
    public static final Long RUN_MINUTES = 23l;
    public static final DateTime END_TIME = new DateTime(500l);
    public static final DateTime CURRENT_TIME = new DateTime(300l);
    private CampaignSchedule underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(CURRENT_TIME.getMillis());
        underTest = new CampaignSchedule(CAMPAIGN_ID, NEXT_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME);
    }

    @Test
    public void testCalculateNextRunTs() throws Exception {
        assertThat(underTest.calculateNextRunTs(),
                equalTo((new DateTime(100l).plusHours(RUN_HOURS.intValue()).plusMinutes(RUN_MINUTES.intValue()))));
    }

    @Test
    public void isExpiredShouldReturnFalseIfPromotionIsValid()   {
        assertThat(underTest.isExpired(), is(Boolean.FALSE));
    }

    @Test
    public void isExpiredShouldReturnTrueIfPromotionIsExpired()   {
        underTest.setEndTime(new DateTime(299l));
        assertThat(underTest.isExpired(), is(Boolean.TRUE));
    }

    @Test
    public void isActiveShouldReturnTrueIfPromotionIsCurrentlyActiveAndTimeisBeforeNextRunTs(){
        underTest.setNextRunTs(new DateTime(301l));
        assertThat(underTest.isActive(), is(Boolean.TRUE));
    }

    @Test
    public void isActiveShouldReturnFalseIfPromoIsNotActive()   {
        underTest.setEndTime(CURRENT_TIME.minusSeconds(1));
        assertThat(underTest.isActive(), is(Boolean.FALSE));
    }
}
