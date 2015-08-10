package com.yazino.bi.aggregator;

import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import utils.PlayerBuilder;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static utils.PlayerBuilder.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
public class AdNetBandsAggregatorIntegrationTest {
    private static final String YESTERDAY = "yesterday";
    private static final String DAY_BEFORE = "day before";
    private static final String GT_YEAR = ">1 year";
    private static final String A_BIT_AGO = "a bit ago";
    private static final String A_WEEK = "a week";
    private static final String LT_MONTH = "<1 month";
    private static final String A_YEAR = "a year";
    private static final String A_MONTH = "a month";
    private static final String ZERO_DAYS = "0 days";
    private static final String ONE_DAY = "1 day";
    private static final String ONE_YEAR = "1+ y";
    private static final String FIRST_WEEK = "1st wk";
    private static final String TWO_TO_TWELVE_MONTHS = "2-12 m";
    @Autowired
    AdNetBandsAggregator underTest;

    @Autowired
    AdNetworkAggregator adNetworkAggregator;

    @Autowired
    DmrAggregator dmrAggregator;

    @Autowired
    private NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;


    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
        // TODO: Mr Rae shall be resolving this failing for 1/3-30/3/14
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2014, 2, 28, 0, 0).getMillis());
        PlayerBuilder.initialise(externalDwNamedJdbcTemplate);
        final HashMap<String, Object> params = newHashMap();
        externalDwNamedJdbcTemplate.update("delete from adnet_registration_bands", params);
        externalDwNamedJdbcTemplate.update("delete from adnet_registrations", params);
        externalDwNamedJdbcTemplate.update("delete from lobby_user", params);
        externalDwNamedJdbcTemplate.update("delete from aggregator_last_update", params);
        params.put("last_run_ts", toTS(lastYear.minusDays(5)));
        params.put("today", toTS(today));
        externalDwNamedJdbcTemplate.update(
                "insert into aggregator_last_update (id,last_run_ts) values('player_activity_daily', :today)",
                params);
        externalDwNamedJdbcTemplate.update(
                "insert into aggregator_last_update (id,last_run_ts) values('daily_management_report', :last_run_ts)",
                params);
        externalDwNamedJdbcTemplate.update(
                "insert into aggregator_last_update (id,last_run_ts) values('adnet_bands', :last_run_ts)",
                params);


    }

    @Test
    public void dataShouldBePopulated() throws InterruptedException {
        final HashMap<String, Object> params = newHashMap();

        createPlayer(ANDY).withReferrer(YESTERDAY).whoRegistered(yesterday).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(BOB).withReferrer(DAY_BEFORE).whoRegistered(dayBeforeYesterday).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(CHAZ).withReferrer(A_BIT_AGO).whoRegistered(aBitAgo).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(DAVE).withReferrer(A_WEEK).whoRegistered(lastWeek).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(ERNIE).withReferrer(A_MONTH).whoRegistered(lastMonth).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(FRANK).withReferrer(LT_MONTH).whoRegistered(lastMonth.plusDays(1)).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(GEORGE).withReferrer(A_YEAR).whoRegistered(lastYear).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(HENRY).withReferrer(GT_YEAR).whoRegistered(lastYear.minusDays(1)).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(IAIN).withReferrer(YESTERDAY).whoRegistered(yesterday).storeIn(externalDwNamedJdbcTemplate);

        //first run dmr to create the registrations
        dmrAggregator.update();
        //then run the adnet
        adNetworkAggregator.update();

        //then finally push the adNetBands
        underTest.update();
        final List<Map<String, Object>> rows = externalDwNamedJdbcTemplate.queryForList(
                "select * from adnet_registration_bands order by registration_date desc, registration_platform;",
                params);
        assertThat(rows.size(), is(36));

        int rowcounter = 0;
        checkRow(rows, rowcounter++, yesterday, LT_MONTH, TWO_TO_TWELVE_MONTHS, 1);
        checkRow(rows, rowcounter++, yesterday, GT_YEAR, ONE_YEAR, 1);
        checkRow(rows, rowcounter++, yesterday, A_BIT_AGO, FIRST_WEEK, 1);
        checkRow(rows, rowcounter++, yesterday, A_MONTH, TWO_TO_TWELVE_MONTHS, 1);
        checkRow(rows, rowcounter++, yesterday, A_WEEK, FIRST_WEEK, 1);
        checkRow(rows, rowcounter++, yesterday, A_YEAR, TWO_TO_TWELVE_MONTHS, 1);
        checkRow(rows, rowcounter++, yesterday, DAY_BEFORE, ONE_DAY, 1);
        checkRow(rows, rowcounter++, yesterday, YESTERDAY, ZERO_DAYS, 2);

        checkRow(rows, rowcounter++, dayBeforeYesterday, LT_MONTH, TWO_TO_TWELVE_MONTHS, 1);
        checkRow(rows, rowcounter++, dayBeforeYesterday, GT_YEAR, TWO_TO_TWELVE_MONTHS, 1);
        checkRow(rows, rowcounter++, dayBeforeYesterday, A_BIT_AGO, FIRST_WEEK, 1);
        checkRow(rows, rowcounter++, dayBeforeYesterday, A_MONTH, TWO_TO_TWELVE_MONTHS, 1);
        checkRow(rows, rowcounter++, dayBeforeYesterday, A_WEEK, FIRST_WEEK, 1);
        checkRow(rows, rowcounter++, dayBeforeYesterday, A_YEAR, TWO_TO_TWELVE_MONTHS, 1);
        checkRow(rows, rowcounter++, dayBeforeYesterday, DAY_BEFORE, ZERO_DAYS, 1);

        checkRow(rows, rowcounter++, aBitAgo, LT_MONTH, "2-4 wks", 1);
        // and so on...
        /*
         2014-01-27 00:00:00 | <1 month              | 2-12 m  |                 1
         2014-01-27 00:00:00 | >1 year               | 1+ y    |                 1
         2014-01-27 00:00:00 | a bit ago             | 1st wk  |                 1
         2014-01-27 00:00:00 | a month               | 2-12 m  |                 1
         2014-01-27 00:00:00 | a week                | 1st wk  |                 1
         2014-01-27 00:00:00 | a year                | 2-12 m  |                 1
         2014-01-27 00:00:00 | day before            | 1 day   |                 1
         2014-01-27 00:00:00 | yesterday             | 0 days  |                 2
         2014-01-26 00:00:00 | <1 month              | 2-12 m  |                 1
         2014-01-26 00:00:00 | >1 year               | 2-12 m  |                 1
         2014-01-26 00:00:00 | a bit ago             | 1st wk  |                 1
         2014-01-26 00:00:00 | a month               | 2-12 m  |                 1
         2014-01-26 00:00:00 | a week                | 1st wk  |                 1
         2014-01-26 00:00:00 | a year                | 2-12 m  |                 1
         2014-01-26 00:00:00 | day before            | 0 days  |                 1
         2014-01-24 00:00:00 | <1 month              | 2-4 wks |                 1
         2014-01-24 00:00:00 | >1 year               | 2-12 m  |                 1
         2014-01-24 00:00:00 | a bit ago             | 0 days  |                 1
         2014-01-24 00:00:00 | a month               | 2-4 wks |                 1
         2014-01-24 00:00:00 | a week                | 1st wk  |                 1
         2014-01-24 00:00:00 | a year                | 2-12 m  |                 1
         2014-01-21 00:00:00 | <1 month              | 2-4 wks |                 1
         2014-01-21 00:00:00 | >1 year               | 2-12 m  |                 1
         2014-01-21 00:00:00 | a month               | 2-4 wks |                 1
         2014-01-21 00:00:00 | a week                | 0 days  |                 1
         2014-01-21 00:00:00 | a year                | 2-12 m  |                 1
         2013-12-29 00:00:00 | <1 month              | 0 days  |                 1
         2013-12-29 00:00:00 | >1 year               | 2-12 m  |                 1
         2013-12-29 00:00:00 | a month               | 1 day   |                 1
         2013-12-29 00:00:00 | a year                | 2-12 m  |                 1
         2013-12-28 00:00:00 | >1 year               | 2-12 m  |                 1
         2013-12-28 00:00:00 | a month               | 0 days  |                 1
         2013-12-28 00:00:00 | a year                | 2-12 m  |                 1
         2013-01-28 00:00:00 | >1 year               | 1 day   |                 1
         2013-01-28 00:00:00 | a year                | 0 days  |                 1
         2013-01-27 00:00:00 | >1 year               | 0 days  |                 1
        */
    }

    private void checkRow(final List<Map<String, Object>> rows,
                          final int rowcounter,
                          DateTime date,
                          String registrationPlatform,
                          String band,
                          long numRegs) {
        final Map<String, Object> row = rows.get(rowcounter);
        assertThat((Timestamp) row.get("registration_date"), is(equalTo(toTS(date))));
        assertThat((String) row.get("registration_platform"), is(equalTo(registrationPlatform)));
        assertThat((String) row.get("band"), is(band));
        assertThat((Long) row.get("num_registrations"), is(numRegs));
    }

}
