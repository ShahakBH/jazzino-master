package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import utils.PlayerBuilder;

import java.sql.Date;
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

public class AdNetworkAggregatorIntegrationTest {

    private static final String OTHER_REFERRER = "other referrer";
    private static final String DEFAULT_REFERRER = "default referrer";
    @Autowired
    private NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;

    @Autowired
    private YazinoConfiguration configuration;

    @Autowired
    AdNetworkAggregator adNetworkAggregator;


    private String mappingSql = "update dmr_player_activity_and_purchases" +
            " set registration_adnet = adnet_mappings.registration_adnet" +
            " from adnet_mappings" +
            " where lower(substring(dmr_player_activity_and_purchases.referrer, 1, char_length(adnet_mappings.referrer))) = lower(adnet_mappings.referrer) and activity_date > now() - interval '1 month';";

    @Autowired
    private DmrAggregator dmrAggregator;

    @Before
    public void setUp() throws Exception {

        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
        MockitoAnnotations.initMocks(this);

        Map<String, Object> params = newHashMap();
        externalDwNamedJdbcTemplate.update("delete from aggregator_last_update", params);
        externalDwNamedJdbcTemplate.update("delete from dmr_player_activity_and_purchases", params);
        externalDwNamedJdbcTemplate.update("delete from adnet_mappings", params);
        PlayerBuilder.initialise(externalDwNamedJdbcTemplate);

    }

    @Test
    public void mappingSqlShouldBeCurrent() {
        assertThat(configuration.getString("strata.aggregators.adnet.mapping"), is(equalTo(mappingSql)));
    }

    @Test
    public void aggregatorShouldRun() {
        adNetworkAggregator.update();
        // <excuse>
        //      yes. this is the weakest integration test ever. but the spec was to "run this query" nothing more.
        // </excuse>
    }

    @Test
    public void aggregatorShouldMapUsingWildcards() {
        int rowPlayer1 = insertDmrRowWithReferrer("referrer");
        int rowPlayer2 = insertDmrRowWithReferrer("referrer_stuffs");
        addMapping("referrer", "refizzle my nizzle");
        adNetworkAggregator.update();
        assertThat(getReferrer(rowPlayer1), equalTo("refizzle my nizzle"));
        assertThat(getReferrer(rowPlayer2), equalTo("refizzle my nizzle"));
    }

    @Test
    public void aggregatorShouldBeCaseInsensitive() {
        int rowPlayer1 = insertDmrRowWithReferrer("REFERRER");
        int rowPlayer2 = insertDmrRowWithReferrer("blobs");
        addMapping("referrer", "refizzle my nizzle");
        addMapping("BLOBS", "refizzle my nizzle2");
        adNetworkAggregator.update();
        assertThat(getReferrer(rowPlayer1), equalTo("refizzle my nizzle"));
        assertThat(getReferrer(rowPlayer2), equalTo("refizzle my nizzle2"));
    }

    @Test
    public void adnetRegistrationsShouldBeFilled() {
        Map<String, Object> params = newHashMap();
        params.put("last_run_ts", toTS(lastWeek));
        params.put("today", toTS(today));
        externalDwNamedJdbcTemplate.update(
                "insert into aggregator_last_update (id,last_run_ts) values('player_activity_daily', :today)",
                params);
        externalDwNamedJdbcTemplate.update(
                "insert into aggregator_last_update (id,last_run_ts) values('daily_management_report', :last_run_ts)",
                params);
        addMapping("def", DEFAULT_REFERRER);
        addMapping("otherdef", OTHER_REFERRER);

        createPlayer(ANDY).withReferrer("Your Mum").whoRegistered(yesterday).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(BOB).withReferrer("def").whoRegistered(dayBeforeYesterday).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(CHAZ).withReferrer("def").whoRegistered(aBitAgo).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(DAVE).withReferrer("otherdef").whoRegistered(lastWeek).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(ERNIE).withReferrer("Your Mum").whoRegistered(yesterday).storeIn(externalDwNamedJdbcTemplate);

        dmrAggregator.update();
        adNetworkAggregator.update();
        final List<Map<String, Object>> rows = externalDwNamedJdbcTemplate.queryForList("select * from adnet_registrations order by registration_date desc , days_ago,registration_adnet asc, num_registrations", params);
        assertThat(rows.size(), is(10));
        int rowcounter = 0;
        checkRow(rows, rowcounter++, yesterday, "Your Mum", 0, 2);
        checkRow(rows, rowcounter++, yesterday, DEFAULT_REFERRER, 1, 1);
        checkRow(rows, rowcounter++, yesterday, DEFAULT_REFERRER, 3, 1);
        checkRow(rows, rowcounter++, yesterday, OTHER_REFERRER, 6, 1);
        checkRow(rows, rowcounter++, dayBeforeYesterday, DEFAULT_REFERRER, 0, 1);
        checkRow(rows, rowcounter++, dayBeforeYesterday, DEFAULT_REFERRER, 2, 1);
        checkRow(rows, rowcounter++, dayBeforeYesterday, OTHER_REFERRER, 5, 1);
        checkRow(rows, rowcounter++, aBitAgo, DEFAULT_REFERRER, 0, 1);
        checkRow(rows, rowcounter++, aBitAgo, OTHER_REFERRER, 3, 1);
        checkRow(rows, rowcounter++, lastWeek, OTHER_REFERRER, 0, 1);
    }

    private void checkRow(final List<Map<String, Object>> rows,
                          final int rowcounter,
                          DateTime date,
                          String referrer,
                          int daysAgo,
                          long numRegs) {
        final Map<String, Object> row = rows.get(rowcounter);
        assertThat((Date) row.get("registration_date"), is(equalTo(date.toDate())));
        assertThat((String) row.get("registration_adnet"), is(equalTo(referrer)));
        assertThat((Integer) row.get("days_ago"), is(daysAgo));
        assertThat((Long) row.get("num_registrations"), is(numRegs));
    }

    private void addMapping(String from, String to) {
        Map<String, Object> params = newHashMap();
        params.put("from", from);
        params.put("to", to);
        externalDwNamedJdbcTemplate.update("insert into adnet_mappings (referrer,registration_adnet)values(:from,:to)", params);
    }

    private String getReferrer(final int playerId) {
        final HashMap<String, Object> params = newHashMap();
        params.put("playerId", playerId);

        return (String) externalDwNamedJdbcTemplate.queryForMap(
                "select registration_adnet from dmr_player_activity_and_purchases where player_id=:playerId", params)
                .get("registration_adnet");
    }

    private int insertDmrRowWithReferrer(String referrer) {
        final int playerId = (int) (Math.random() * 1000);
        externalDwNamedJdbcTemplate.update(
                String.format(
                        "insert into dmr_player_activity_and_purchases (player_id, game, platform, activity_date, registration_date, referrer) " +
                                "values (%s,'SLOTS', 'your_mum', now(), now(), '%s');",
                        playerId, referrer),
                new HashMap<String, String>());
        return playerId;
    }

}

