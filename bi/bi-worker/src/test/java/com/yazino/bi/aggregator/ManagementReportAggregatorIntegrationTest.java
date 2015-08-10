package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import utils.PlayerBuilder;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yazino.test.ThreadLocalDateTimeUtils.setCurrentMillisFixed;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static utils.PlayerBuilder.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
@Ignore
public class ManagementReportAggregatorIntegrationTest {

    private static final String JON = "JON";
    @Autowired
    @Qualifier("managementReportAggregator")
    private Aggregator underTest;

    @Autowired
    private YazinoConfiguration configuration;

    @Autowired
    private NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;

    @Autowired
    AggregatorLastUpdateDAO aggregatorLastUpdateDAO;

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Before
    public void setUp() throws Exception {
        setCurrentMillisFixed(0l);
        PlayerBuilder.initialise(externalDwNamedJdbcTemplate);


        externalDwNamedJdbcTemplate.update("delete from aggregator_lock where id='management_report'", new HashMap());
        externalDwNamedJdbcTemplate.update("delete from management_report", new HashMap());
        externalDwNamedJdbcTemplate.update("delete from currency_rates where currency_code='JON'", new HashMap());
        externalDwNamedJdbcTemplate.update("insert into currency_rates values ('JON',66.6)", new HashMap());


        aggregatorLastUpdateDAO.updateLastRunFor(underTest.getAggregatorId(),
                new Timestamp(new DateTime().minusDays(10).getMillis()));


        PlayerBuilder.createPlayer(ANDY).whoPlayed(today).whoRegistered(today).whoBoughtChips(new Purchase(yesterday, 7)).storeIn(
                externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(BOB).whoPlayed(yesterday).whoRegistered(yesterday).storeIn(externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(CHAZ).whoPlayed(dayBeforeYesterday).whoRegistered(dayBeforeYesterday)
                .whoBoughtChips(new Purchase(yesterday, 5), new Purchase(dayBeforeYesterday, 732).withCurrency(JON)).storeIn(externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(DAVE).whoPlayed(aBitAgo).whoRegistered(aBitAgo).storeIn(externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(ERNIE).whoPlayed(lastWeek).whoRegistered(lastWeek).storeIn(externalDwNamedJdbcTemplate);
        refreshTransactionView();
    }

    private void refreshTransactionView() {
        externalDwNamedJdbcTemplate.update("refresh MATERIALIZED VIEW external_transaction_mv", new HashMap<String, String>());
    }

    @Test
    public void lastRunOfTodayShouldRunForTodayAndYesterday() {
        setCurrentMillisFixed(new DateTime(System.currentTimeMillis()).withHourOfDay(10).getMillis());
        PlayerBuilder.initialise(externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(BOB).whoPlayed(yesterday).whoRegistered(yesterday).storeIn(externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(ANDY).whoPlayed(today).whoRegistered(today)
                .whoBoughtChips(new Purchase(new DateTime(), 7)).storeIn(externalDwNamedJdbcTemplate);
        aggregatorLastUpdateDAO.updateLastRunFor(underTest.getAggregatorId(),
                new Timestamp(new DateTime().minusMinutes(10).getMillis()));

        refreshTransactionView();
        underTest.update();

        List<Map<String, Object>> reportRows = externalDwNamedJdbcTemplate.queryForList(
                "select * from management_report order by activity_date desc",
                new HashMap<String, Integer>());
        assertThat(reportRows.size(),is(2));
        assertThat((Date) reportRows.get(0).get("activity_date"), is(equalTo(new Date(today.getMillis()))));
        assertThat((Integer) reportRows.get(0).get("registrations"), is(equalTo(1)));
        assertThat((Integer) reportRows.get(0).get("players"), is(equalTo(1)));
        assertThat((Integer) reportRows.get(0).get("purchases"), is(equalTo(1)));
        assertThat((BigDecimal) reportRows.get(0).get("revenue"), is(comparesEqualTo(valueOf(7))));

        PlayerBuilder.createPlayer(CHAZ).whoPlayed(today).whoRegistered(today)
                .whoBoughtChips(new Purchase(today, 5)).storeIn(externalDwNamedJdbcTemplate);
        refreshTransactionView();
        underTest.update();

        reportRows = externalDwNamedJdbcTemplate.queryForList(
                "select * from management_report order by activity_date desc",
                new HashMap<String, Integer>());
        assertThat((Date) reportRows.get(0).get("activity_date"), is(equalTo(new Date(today.getMillis()))));
        assertThat((Integer) reportRows.get(0).get("registrations"), is(equalTo(2)));
        assertThat((Integer) reportRows.get(0).get("purchases"), is(equalTo(2)));
        assertThat((BigDecimal) reportRows.get(0).get("revenue"), is(comparesEqualTo(valueOf(12))));
    }

    @Test
    public void registrationsShouldBeAggregated() {

        underTest.update();

        final List<Map<String, Object>> registrations = externalDwNamedJdbcTemplate.queryForList(
                "select activity_date, registrations from management_report order by activity_date desc",
                new HashMap<String, Integer>());
        assertThat(registrations.size(), is(12));

        assertThat((Date) registrations.get(0).get("activity_date"), is(equalTo(new Date(today.getMillis()))));
        assertThat((Integer) registrations.get(0).get("registrations"), is(equalTo(1)));
        assertThat((Integer) registrations.get(1).get("registrations"), is(equalTo(1)));
        assertThat((Integer) registrations.get(2).get("registrations"), is(equalTo(1)));
        assertThat((Integer) registrations.get(3).get("registrations"), is(equalTo(0)));
        assertThat((Integer) registrations.get(4).get("registrations"), is(equalTo(1)));
        assertThat((Integer) registrations.get(5).get("registrations"), is(equalTo(0)));
        assertThat((Integer) registrations.get(6).get("registrations"), is(equalTo(0)));
        assertThat((Integer) registrations.get(7).get("registrations"), is(equalTo(1)));
    }

    @Test
    public void playersShouldBeAggregated() {
        underTest.update();

        final List<Map<String, Object>> players = externalDwNamedJdbcTemplate.queryForList(
                "select activity_date, players from management_report order by activity_date desc",
                new HashMap<String, Integer>());

        assertThat(players.size(), is(12));
        int i = 0;
        assertThat((Date) players.get(i).get("activity_date"), is(equalTo(new Date(today.getMillis()))));
        assertThat((Integer) players.get(i++).get("players"), is(equalTo(1)));
        assertThat((Integer) players.get(i++).get("players"), is(equalTo(1)));
        assertThat((Integer) players.get(i++).get("players"), is(equalTo(1)));
        assertThat((Integer) players.get(i++).get("players"), is(equalTo(0)));
        assertThat((Integer) players.get(i++).get("players"), is(equalTo(1)));
        assertThat((Integer) players.get(i++).get("players"), is(equalTo(0)));
        assertThat((Integer) players.get(i++).get("players"), is(equalTo(0)));
        assertThat((Integer) players.get(i++).get("players"), is(equalTo(1)));
        assertThat((Integer) players.get(i++).get("players"), is(equalTo(0)));

    }

    @Test
    public void revenueShouldBeAggregated() {
        underTest.update();

        final List<Map<String, Object>> revenues = externalDwNamedJdbcTemplate.queryForList(
                "select activity_date, revenue from management_report order by activity_date desc",
                new HashMap<String, Integer>());
        int day = 1;
        assertThat((Date) revenues.get(day).get("activity_date"), is(equalTo(new Date(yesterday.getMillis()))));
        assertThat((BigDecimal) revenues.get(day++).get("revenue"), is(comparesEqualTo(valueOf(12))));
        assertThat((BigDecimal) revenues.get(day++).get("revenue"), is(comparesEqualTo(valueOf(10.9910))));
        assertThat((BigDecimal) revenues.get(day++).get("revenue"), nullValue());
    }

    @Test
    public void purchasesShouldBeAggregated() {
        underTest.update();

        final List<Map<String, Object>> purchases = externalDwNamedJdbcTemplate.queryForList(
                "select activity_date, purchases from management_report order by activity_date desc",
                new HashMap<String, Integer>());
        int day = 1;
        assertThat((Date) purchases.get(day).get("activity_date"), is(equalTo(new Date(yesterday.getMillis()))));
        assertThat((Integer) purchases.get(day++).get("purchases"), is(comparesEqualTo((2))));
        assertThat((Integer) purchases.get(day++).get("purchases"), is(comparesEqualTo((1))));
        assertThat((Integer) purchases.get(day++).get("purchases"), is(comparesEqualTo((0))));
    }


}
