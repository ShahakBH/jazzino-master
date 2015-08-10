package com.yazino.bi.aggregator;

import com.google.common.collect.Maps;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.Boolean.TRUE;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static utils.PlayerBuilder.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
public class DmrAggregatorIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;


    @Autowired
    private YazinoConfiguration configuration;

    @Autowired
    private DmrAggregator dmrAggregator;

    @Autowired
    AggregatorLastUpdateDAO aggregatorLastUpdateDAO;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(1000000000L);

        configuration.setProperty("data-warehouse.write.enabled", TRUE);
        configuration.setProperty("data-warehouse.aggregators.enabled", TRUE);
        aggregatorLastUpdateDAO.updateLastRunFor(dmrAggregator.getAggregatorId(),
                new Timestamp(new DateTime().minusDays(1).getMillis()));

        Map<String, Object> params = newHashMap();
        externalDwNamedJdbcTemplate.update("delete from aggregator_lock", params);
        externalDwNamedJdbcTemplate.update("delete from audit_command", params);
        externalDwNamedJdbcTemplate.update("delete from aggregator_last_update", params);

        externalDwNamedJdbcTemplate.update("delete from dmr_registrations", params);
        externalDwNamedJdbcTemplate.update("delete from dmr_player_activity_and_purchases", params);
        externalDwNamedJdbcTemplate.update("delete from currency_rates", params);
        externalDwNamedJdbcTemplate.update("insert into currency_rates values ('GBP',1)", params);

        PlayerBuilder.initialise(externalDwNamedJdbcTemplate);

    }

    @After
    public void resetDateTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void dayZeroDataShouldShowTodaysRegistrationsThatPlayed() {
        createPlayer(BOB).whoRegistered(yesterday).whoPlayed(yesterday).storeIn(externalDwNamedJdbcTemplate);

        dmrAggregator.update();

        assertThat(dmrRegistrations().size(), is(equalTo(1)));
        assertThat(getNumOfRegistrations(yesterday, 0), equalTo(1));

        assertThat(getDmrRows().size(), is(equalTo(1)));
        forPlayerWhoPlayedOn(BOB, yesterday).regDateShouldBe(yesterday);
    }

    @Test
    public void testRunTwiceOnADayShouldShowTodaysRegistrationsThatPlayed() {
        createPlayer(BOB).whoRegistered(yesterday).whoPlayed(yesterday).storeIn(externalDwNamedJdbcTemplate);

        dmrAggregator.update();
        aggregatorLastUpdateDAO.updateLastRunFor(dmrAggregator.getAggregatorId(), new Timestamp(yesterday.getMillis()));
        dmrAggregator.update();

        assertThat(dmrRegistrations().size(), is(equalTo(1)));
        assertThat(getNumOfRegistrations(yesterday, 0), equalTo(1));

        assertThat(getDmrRows().size(), is(equalTo(1)));
        forPlayerWhoPlayedOn(BOB, yesterday).regDateShouldBe(yesterday);
    }

    @Test
    public void playerWhoDoesNotPlayShouldNotShowup() {

        createPlayer(ANDY).whoRegistered(yesterday).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(BOB).whoRegistered(yesterday).whoPlayed(yesterday).storeIn(externalDwNamedJdbcTemplate);

        dmrAggregator.update();

        assertThat(dmrRegistrations().size(), is(equalTo(1)));
        assertThat(getNumOfRegistrations(yesterday, 0), equalTo(2));

        assertThat(getDmrRows().size(), is(equalTo(1)));
        forPlayerWhoPlayedOn(BOB, yesterday).regDateShouldBe(yesterday);
    }


    @Test
    public void dayShouldShowAllDaysRegistrationsThatPlayed() {

        createPlayer(ANDY).whoRegistered(yesterday).whoPlayed(yesterday).whoBoughtChips(new Purchase(yesterday, 10)).storeIn(
                externalDwNamedJdbcTemplate);
        createPlayer(BOB).whoRegistered(yesterday).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(CHAZ).whoRegistered(dayBeforeYesterday).whoPlayed(dayBeforeYesterday).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(DAVE).whoRegistered(dayBeforeYesterday).whoPlayed(dayBeforeYesterday, yesterday)
                .storeIn(externalDwNamedJdbcTemplate);
        createPlayer(ERNIE).whoRegistered(lastWeek).whoPlayed(lastWeek, aBitAgo, yesterday).whoBoughtChips(new Purchase(yesterday, 5))
                .storeIn(externalDwNamedJdbcTemplate);

        externalDwNamedJdbcTemplate.update("refresh MATERIALIZED VIEW external_transaction_mv", new HashMap<String, String>());
        dmrAggregator.update();
        aggregatorLastUpdateDAO.updateLastRunFor(dmrAggregator.getAggregatorId(), new Timestamp(yesterday.getMillis()));
        dmrAggregator.update();


        assertThat(dmrRegistrations().size(), is(equalTo(6)));

        assertThat(getNumOfRegistrations(yesterday, 0), equalTo(2));
        assertThat(getNumOfRegistrations(yesterday, 1), equalTo(2));
        assertThat(getNumOfRegistrations(yesterday, 6), equalTo(1));
        assertThat(getNumOfRegistrations(dayBeforeYesterday, 0), equalTo(2));
        assertThat(getNumOfRegistrations(dayBeforeYesterday, 5), equalTo(1));
        assertThat(getNumOfRegistrations(lastWeek, 0), equalTo(1));

        assertThat(getDmrRows().size(), is(equalTo(7)));
        forPlayerWhoPlayedOn(ANDY, yesterday).regDateShouldBe(yesterday).numPurchasesAre(1).totalAmountGbpIs(10);
        forPlayerWhoPlayedOn(CHAZ, dayBeforeYesterday).regDateShouldBe(dayBeforeYesterday);
        forPlayerWhoPlayedOn(DAVE, yesterday, dayBeforeYesterday).regDateShouldBe(dayBeforeYesterday);
        forPlayerWhoPlayedOn(ERNIE, yesterday, aBitAgo, lastWeek).numPurchasesAre(1).totalAmountGbpIs(5).regDateShouldBe(lastWeek);
    }

    @Test
    public void dayZeroAndDayOneDataShouldShowTodaysAndYesterdaysRegistrationsThatPlayed() {

        createPlayer(BOB).whoRegistered(yesterday).whoPlayed(yesterday).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(CHAZ).whoRegistered(dayBeforeYesterday).whoPlayed(yesterday).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(DAVE).whoRegistered(dayBeforeYesterday).whoPlayed(dayBeforeYesterday).storeIn(externalDwNamedJdbcTemplate);

        dmrAggregator.update();

        assertThat(dmrRegistrations().size(), is(equalTo(3)));
        assertThat(getNumOfRegistrations(yesterday, 0), equalTo(1));
        assertThat(getNumOfRegistrations(yesterday, 1), equalTo(2));
        assertThat(getNumOfRegistrations(dayBeforeYesterday, 0), equalTo(2));

        assertThat(getDmrRows().size(), is(equalTo(3)));
        forPlayerWhoPlayedOn(BOB, yesterday).regDateShouldBe(yesterday);
        forPlayerWhoPlayedOn(CHAZ, yesterday).regDateShouldBe(dayBeforeYesterday);
        forPlayerWhoPlayedOn(DAVE, dayBeforeYesterday).regDateShouldBe(dayBeforeYesterday);

    }

    @Test
    public void runsFrom6Til6ShouldNotShowTodaysData() {
        createPlayer(ANDY).whoRegistered(yesterday).whoPlayed(yesterday).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(BOB).whoRegistered(today).whoPlayed(today).storeIn(externalDwNamedJdbcTemplate);

        dmrAggregator.update();
        assertThat(dmrRegistrations().size(), is(equalTo(1)));

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().plusDays(1).getMillis());
        dmrAggregator.update();
        assertThat(dmrRegistrations().size(), is(equalTo(3)));

        forPlayerWhoPlayedOn(ANDY, yesterday).regDateShouldBe(yesterday);
        forPlayerWhoPlayedOn(BOB, today).regDateShouldBe(today);
    }

    @Test
    public void personWhoPlaysAndPurchasesOnTwoDaysShouldHaveTwoRows() {

        createPlayer(ANDY)
                .whoRegistered(dayBeforeYesterday)
                .whoPlayed(dayBeforeYesterday, yesterday)
                .whoBoughtChips(
                        new Purchase(dayBeforeYesterday, 10),
                        new Purchase(dayBeforeYesterday, 3),
                        new Purchase(yesterday, 5),
                        new Purchase(yesterday, 11))
                .storeIn(externalDwNamedJdbcTemplate);

        externalDwNamedJdbcTemplate.update("refresh MATERIALIZED VIEW external_transaction_mv", new HashMap<String, String>());
        dmrAggregator.update();

        assertThat(dmrRegistrations().size(), is(equalTo(1)));
        assertThat(getNumOfRegistrations(dayBeforeYesterday, 0), equalTo(1));

        assertThat(getDmrRows().size(), is(equalTo(2)));
        forPlayerWhoPlayedOn(ANDY, yesterday).regDateShouldBe(dayBeforeYesterday).numPurchasesAre(2).totalAmountGbpIs(16);
        forPlayerWhoPlayedOn(ANDY, dayBeforeYesterday).regDateShouldBe(dayBeforeYesterday).numPurchasesAre(2).totalAmountGbpIs(13);

    }

    @Test
    public void successiveRunsShouldNotCreateDuplicates() {


        createPlayer(BOB).whoRegistered(yesterday).whoPlayed(yesterday).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(CHAZ).whoRegistered(dayBeforeYesterday).whoPlayed(yesterday).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(DAVE).whoRegistered(dayBeforeYesterday).whoPlayed(dayBeforeYesterday).storeIn(externalDwNamedJdbcTemplate);

        dmrAggregator.update();

        createPlayer(ERNIE).whoRegistered(today).whoPlayed(today).storeIn(externalDwNamedJdbcTemplate);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(today.plusDays(1).getMillis());

        dmrAggregator.update();

        assertThat(dmrRegistrations().size(), is(equalTo(6)));
        assertThat(getNumOfRegistrations(today, 0), equalTo(1));
        assertThat(getNumOfRegistrations(today, 1), equalTo(1));
        assertThat(getNumOfRegistrations(today, 2), equalTo(2));
        assertThat(getNumOfRegistrations(yesterday, 0), equalTo(1));
        assertThat(getNumOfRegistrations(yesterday, 1), equalTo(2));
        assertThat(getNumOfRegistrations(dayBeforeYesterday, 0), equalTo(2));

        assertThat(getDmrRows().size(), is(equalTo(4)));
        forPlayerWhoPlayedOn(BOB, yesterday).regDateShouldBe(yesterday);
        forPlayerWhoPlayedOn(CHAZ, yesterday).regDateShouldBe(dayBeforeYesterday);
        forPlayerWhoPlayedOn(DAVE, dayBeforeYesterday).regDateShouldBe(dayBeforeYesterday);
        forPlayerWhoPlayedOn(ERNIE, today).regDateShouldBe(today);
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();

    }

    @Test
    public void playerWhoPlaysTwoGamesShouldOnlyShowUpOnce() {
        createPlayer(BOB).whoRegistered(yesterday).whoPlayed(yesterday).aGameOf("HIGH_STAKES").whoBoughtChips(new Purchase(yesterday, 10)).storeIn(externalDwNamedJdbcTemplate);
        getPlayer(BOB).whoPlayed(yesterday).aGameOf("BLACKJACK").storeIn(externalDwNamedJdbcTemplate);
        dmrAggregator.update();
        assertThat(getDmrRows().size(), is(equalTo(1)));

    }

    @Test
    public void amountsOfPurchasesShouldBeCorrect() {

        createPlayer(ANDY).whoRegistered(yesterday).whoPlayed(yesterday).whoBoughtChips(new Purchase(yesterday, 10)).storeIn(
                externalDwNamedJdbcTemplate);

        dmrAggregator.update();

        assertThat(dmrRegistrations().size(), is(equalTo(1)));
        assertThat(getNumOfRegistrations(yesterday, 0), equalTo(1));

        assertThat(getDmrRows().size(), is(equalTo(1)));
        forPlayerWhoPlayedOn(ANDY, yesterday).regDateShouldBe(yesterday).numPurchasesAre(1).totalAmountGbpIs(10);
    }

    private RowTester forPlayerWhoPlayedOn(final int playerId, final DateTime... playedOn) {
        return new RowTester(playerId, playedOn);
    }

    private Map<String, Object> getDmrRowForPlayerWhenTheyPlayedOn(final int playerId, final DateTime played) {
        final HashMap<String, Object> paramMap = newHashMap();
        paramMap.put("player_id", playerId);
        paramMap.put("played", toTS(played));
        final List<Map<String, Object>> rows = externalDwNamedJdbcTemplate.queryForList(
                "select * from dmr_player_activity_and_purchases where player_id=:player_id and activity_date=:played",
                paramMap);
        assertThat(rows.size(), is(1));
        return rows.get(0);
    }

    private Integer getNumOfRegistrations(DateTime date, int daysAgo) {
        return (Integer) getRegistration(date, daysAgo).get("num_registrations");
    }

    private Map<String, Object> getRegistration(DateTime date, int daysAgo) {
        final HashMap<String, Object> params = Maps.newHashMap();
        params.put("reg", new Timestamp(date.getMillis()));
        params.put("days_ago", daysAgo);

        return externalDwNamedJdbcTemplate.queryForList(
                "select * from dmr_registrations where registration_date = :reg and days_ago= :days_ago",
                params).get(0);
    }

    private List<Map<String, Object>> dmrRegistrations() {
        return externalDwNamedJdbcTemplate.queryForList("select * from dmr_registrations order by registration_date, days_ago",
                new HashMap<String, String>());

    }

    private List<Map<String, Object>> getDmrRows() {
        return externalDwNamedJdbcTemplate.queryForList("select * from dmr_player_activity_and_purchases", new HashMap<String, String>());
    }

    private class RowTester {

        private final Map<DateTime, Map<String, Object>> dmrRowsForPlayer;
        private final DateTime[] playedDates;

        public RowTester(final int playerId, final DateTime... playedDates) {
            this.playedDates = playedDates;
            dmrRowsForPlayer = newHashMap();
            for (DateTime played : playedDates) {
                final Map<String, Object> row = getDmrRowForPlayerWhenTheyPlayedOn(playerId, played);
                assertNotNull(row);
                dmrRowsForPlayer.put(played, row);

            }
        }

        public RowTester regDateShouldBe(final DateTime regDate) {
            for (DateTime playedDate : playedDates) {
                assertThat(((Timestamp) dmrRowsForPlayer.get(playedDate).get("registration_date")), equalTo(toTS(regDate)));

            }
            return this;
        }

        public RowTester numPurchasesAre(final int purchases) {
            int count = 0;
            for (DateTime playedDate : playedDates) {
                final Integer numPurchases = (Integer) dmrRowsForPlayer.get(playedDate).get("num_purchases");
                count += numPurchases == null ? 0 : numPurchases;
            }
            assertThat(count, comparesEqualTo(purchases));
            return this;
        }

        public RowTester totalAmountGbpIs(final int amount) {
            BigDecimal total = BigDecimal.ZERO;
            for (DateTime playedDate : playedDates) {
                BigDecimal totalAmountGbp = (BigDecimal) dmrRowsForPlayer.get(playedDate).get("total_amount_gbp");
                total = total.add(totalAmountGbp == null ? BigDecimal.ZERO : totalAmountGbp);
            }
            assertThat(total,
                    comparesEqualTo(BigDecimal.valueOf(amount)));
            return this;
        }
    }


}
