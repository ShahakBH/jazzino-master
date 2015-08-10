package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import utils.ParamBuilder;
import utils.PlayerBuilder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static utils.ParamBuilder.emptyParams;
import static utils.ParamBuilder.params;
import static utils.PlayerBuilder.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
//@Transactional
@DirtiesContext
public class DayZeroAggregatorIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;

    @Autowired
    private YazinoConfiguration configuration;

    @Autowired
    DayZeroAggregator dayZeroAggregator;
    private DateTime reg;

    @Before
    public void setUp() throws Exception {
        PlayerBuilder.initialise(externalDwNamedJdbcTemplate);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(
                now().withHourOfDay(12).withMinuteOfHour(0)
                        .withSecondOfMinute(0)
                        .withMillisOfSecond(0).getMillis()
        );
        externalDwNamedJdbcTemplate.update("delete from day_zero", emptyParams());
        reg = now().minusHours(23).minusSeconds(1);
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();

    }

    @Test
    public void aggregatorShouldRunForPeopleWhoPlayed25To24HoursAgo() {
        PlayerBuilder.createPlayer(ANDY).whoRegistered(now().minusHours(23)).storeIn(
                externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(BOB).whoRegistered(now().minusHours(23).minusSeconds(1)).storeIn(
                externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(CHAZ).whoRegistered(now().minusHours(24)).storeIn(
                externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(DAVE).whoRegistered(now().minusHours(24).minusSeconds(1)).storeIn(
                externalDwNamedJdbcTemplate);
        dayZeroAggregator.update();
        final List<Map<String, Object>> dayZeros = getDayZeros();

        System.out.println(dayZeros);
        assertThat(dayZeros.size(), is(2));
        assertThat((BigDecimal) dayZeros.get(0).get("player_id"), comparesEqualTo(BigDecimal.valueOf(BOB)));
        assertThat((BigDecimal) dayZeros.get(1).get("player_id"), comparesEqualTo(BigDecimal.valueOf(CHAZ)));
    }

    @Test
    public void aggregatorShouldFillBasicValues() {

        PlayerBuilder.createPlayer(BOB).withLevel(5).withOtherLevel(2).withBalance(666L).whoRegistered(reg).whoLoggedIn(
                now().minusHours(23),
                now().minusHours(22),
                now().minusHours(21))
                .storeIn(
                        externalDwNamedJdbcTemplate);

        dayZeroAggregator.update();

        final Map<String, Object> dayZero = getDayZero();

        assertThat((BigDecimal) dayZero.get("player_id"), comparesEqualTo(BigDecimal.valueOf(BOB)));
        assertThat((Timestamp) dayZero.get("reg_ts"), is(toTS(reg)));
        assertThat((Integer) dayZero.get("level"), is(5));
        assertThat((BigDecimal) dayZero.get("account_id"), comparesEqualTo(BigDecimal.valueOf(10+BOB)));
        assertThat((Long) dayZero.get("sessions"), is(3L));
        assertThat((BigDecimal) dayZero.get("balance"), comparesEqualTo(BigDecimal.valueOf(666)));
        assertThat((String) dayZero.get("registration_platform"), equalTo("IOS"));
        assertThat((String) dayZero.get("registration_game_type"), equalTo("SLOTS"));

    }

    private Map<String, Object> getDayZero() {
        return externalDwNamedJdbcTemplate.queryForMap(
                "select * from day_zero order by player_id", ParamBuilder.params());
    }

    @Test
    public void aggregatorShouldFillBets() {
        PlayerBuilder.createPlayer(BOB).withSomeBets(10).whoRegistered(reg).storeIn(externalDwNamedJdbcTemplate);

        dayZeroAggregator.update();

        final Map<String, Object> dayZero = getDayZero();
        assertThat((Integer) dayZero.get("stakes"), equalTo(10));
    }

    @Test
    public void aggregatorShouldDefaultLevelToOne() {
        PlayerBuilder.createPlayer(BOB).withSomeBets(10).whoRegistered(reg).storeIn(externalDwNamedJdbcTemplate);

        dayZeroAggregator.update();

        final Map<String, Object> dayZero = getDayZero();
        assertThat((Integer) dayZero.get("level"), equalTo(1));
    }

    @Test
    public void aggregatorShouldFillBonii() {

        PlayerBuilder.createPlayer(BOB)
                .withBonusCollections(reg.plusHours(1), reg.plusHours(6)).whoRegistered(reg).storeIn(
                externalDwNamedJdbcTemplate);

        dayZeroAggregator.update();

        final Map<String, Object> dayZero = getDayZero();
        assertThat((Integer) dayZero.get("bonus_collections"), equalTo(2));
    }

    @Test
    public void aggregatorShouldReturnMaxLevel() {
        PlayerBuilder.createPlayer(BOB)
                .whoRegistered(reg).withLevel(5).withOtherLevel(2).storeIn(
                externalDwNamedJdbcTemplate);
        dayZeroAggregator.update();
        final Map<String, Object> dayZero = getDayZero();
        assertThat((Integer) dayZero.get("level"), is(5));

    }

    @Test
    public void aggregatorShouldCountPurchases() {
        createPlayer(BOB).whoRegistered(reg).whoBoughtChips(
                new Purchase(reg.plusMinutes(10), 600), new Purchase(reg.plusHours(1), 10)).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(CHAZ).whoRegistered(reg).whoBoughtChips(
                new Purchase(reg.minusHours(10), 600), new Purchase(reg.plusHours(1), 10)).storeIn(externalDwNamedJdbcTemplate);
        dayZeroAggregator.update();
        final List<Map<String, Object>> dayZeros = getDayZeros();
        assertThat(dayZeros.size(), is(2));
        assertThat((Integer) dayZeros.get(0).get("purchases"), is(2));
        assertThat((Integer) dayZeros.get(1).get("purchases"), is(1));
    }

    @Test
    public void aggregatorShouldFillPayout() {

        PlayerBuilder.createPlayer(BOB).whoRegistered(reg).storeIn(externalDwNamedJdbcTemplate);
        externalDwNamedJdbcTemplate.update(
                "INSERT INTO TRANSACTION_LOG(ACCOUNT_ID,AMOUNT,TRANSACTION_TYPE,TRANSACTION_TS,REFERENCE, TABLE_ID, GAME_ID) " +
                        "VALUES(12,-1000,'Stake',:time, 'test_tranny', -777.01, 0.00 )",
                params().param("time", toTS(reg.plusHours(1)))
        );
        externalDwNamedJdbcTemplate.update(
                "INSERT INTO TRANSACTION_LOG(ACCOUNT_ID,AMOUNT,TRANSACTION_TYPE,TRANSACTION_TS,REFERENCE, TABLE_ID, GAME_ID) " +
                        "VALUES(12,-1000,'Stake',:time, 'test_tranny', -777.01, 0.00 )",
                params().param("time", toTS(reg.plusHours(1)))
        );
        externalDwNamedJdbcTemplate.update(
                "INSERT INTO TRANSACTION_LOG(ACCOUNT_ID,AMOUNT,TRANSACTION_TYPE,TRANSACTION_TS,REFERENCE, TABLE_ID, GAME_ID) " +
                        "VALUES(12,1000,'Return',:time, 'test_tranny', -777.01, 0.00 )",
                params().param("time", toTS(reg.plusHours(1)))
        );

        dayZeroAggregator.update();

        final Map<String, Object> dayZero = getDayZero();
        assertThat((BigDecimal) dayZero.get("payout"), comparesEqualTo(new BigDecimal("0.5")));
    }

    @Test
    public void aggregatorShouldFillDataFromTransactionLogBonii() {
        PlayerBuilder.createPlayer(ANDY).whoRegistered(reg)
                .withBonusCollections(reg.plusHours(1), reg.plusHours(6))
                .storeIn(externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(BOB).whoRegistered(reg)
                .withBonusCollections(reg.plusHours(6), reg.plusHours(2)).withSomeBets(3)
                .storeIn(externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(CHAZ).whoRegistered(reg).withSomeBets(2)
                .storeIn(externalDwNamedJdbcTemplate);

        dayZeroAggregator.update();

        final List<Map<String, Object>> dayZeros = getDayZeros();
        final Map<String, Object> andy = dayZeros.get(0);
        final Map<String, Object> bob = dayZeros.get(1);
        final Map<String, Object> chaz = dayZeros.get(2);

        assertThat((Integer) andy.get("bonus_collections"), equalTo(2));
        assertThat((Integer) bob.get("bonus_collections"), equalTo(2));
        assertThat((Integer) chaz.get("bonus_collections"), equalTo(0));

        assertThat((Integer) andy.get("stakes"), equalTo(0));
        assertThat((Integer) bob.get("stakes"), equalTo(3));
        assertThat((Integer) chaz.get("stakes"), equalTo(2));

    }

    private List<Map<String, Object>> getDayZeros() {
        return externalDwNamedJdbcTemplate.queryForList(
                "select * from day_zero order by player_id", ParamBuilder.params());
    }

}