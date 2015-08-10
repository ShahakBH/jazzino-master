package strata.server.worker.event.persistence;

import com.yazino.promotion.PromoRewardEvent;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
public class PostgresPromotionRewardDWDAOIntegrationTest {

    @Autowired
    private PostgresPromotionRewardDWDAO underTest;

    @Autowired
    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        jdbc.execute("delete from promo_reward");
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis());

    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();

    }

    @Test
    public void saveAllShouldSaveRecord() {
        final List<PromoRewardEvent> rewards = newArrayList();
        final PromoRewardEvent event = new PromoRewardEvent(BigDecimal.ONE, 2l, new DateTime());
        rewards.add(event);
        underTest.saveAll(rewards);
        assertThat(jdbc.queryForInt("select count(*) from PROMO_REWARD"), is(1));
        final PromoRewardEvent promoRewardEvent = jdbc.queryForObject("select * from promo_reward", mapRowToPromoReward());
        assertThat(promoRewardEvent, equalTo(event));
    }

    @Test
    public void saveTwiceShouldSaveRecordAndNotDuplicate() {
        final List<PromoRewardEvent> rewards = newArrayList();
        final PromoRewardEvent event = new PromoRewardEvent(BigDecimal.ONE, 2l, new DateTime());
        rewards.add(event);
        underTest.saveAll(rewards);
        underTest.saveAll(rewards);
        assertThat(jdbc.queryForInt("select count(*) from PROMO_REWARD"), is(1));
        final PromoRewardEvent promoRewardEvent = jdbc.queryForObject("select * from promo_reward", mapRowToPromoReward());
        assertThat(promoRewardEvent, equalTo(event));
    }

    @Test
    public void saveTwoPromoRewardsShouldSaveRecords() {
        final List<PromoRewardEvent> rewards = newArrayList();
        final PromoRewardEvent event = new PromoRewardEvent(BigDecimal.ONE, 2l, new DateTime());
        final PromoRewardEvent eventTwo = new PromoRewardEvent(BigDecimal.ONE, 3l, new DateTime());

        rewards.add(event);
        rewards.add(eventTwo);

        underTest.saveAll(rewards);

        assertThat(jdbc.queryForInt("select count(*) from PROMO_REWARD"), is(2));
        final List<PromoRewardEvent> promoRewardEvents = jdbc.query("select * from promo_reward", mapRowToPromoReward());
        assertThat(promoRewardEvents.get(0), equalTo(event));
        assertThat(promoRewardEvents.get(1), equalTo(eventTwo));

    }

    @Test
    public void savePromoRewardsForDifferentPlayersShouldSaveRecords() {
        final List<PromoRewardEvent> rewards = newArrayList();
        final PromoRewardEvent event = new PromoRewardEvent(BigDecimal.ONE, 2l, new DateTime());
        final PromoRewardEvent eventTwo = new PromoRewardEvent(BigDecimal.TEN, 2l, new DateTime());

        rewards.add(event);
        rewards.add(eventTwo);

        underTest.saveAll(rewards);

        assertThat(jdbc.queryForInt("select count(*) from PROMO_REWARD"), is(2));
        final List<PromoRewardEvent> promoRewardEvents = jdbc.query("select * from promo_reward", mapRowToPromoReward());
        assertThat(promoRewardEvents.get(0), equalTo(event));
        assertThat(promoRewardEvents.get(1), equalTo(eventTwo));
    }

    @Test
    public void saveTwoPromoRewardsForSamePlayersDifferentTimeShouldSaveRecord() {
        final List<PromoRewardEvent> rewards = newArrayList();
        final PromoRewardEvent event = new PromoRewardEvent(BigDecimal.ONE, 2l, new DateTime());
        final PromoRewardEvent eventTwo = new PromoRewardEvent(BigDecimal.ONE, 2l, new DateTime().plusHours(1));

        rewards.add(event);
        rewards.add(eventTwo);

        underTest.saveAll(rewards);
        underTest.saveAll(rewards);

        assertThat(jdbc.queryForInt("select count(*) from PROMO_REWARD"), is(2));
        final List<PromoRewardEvent> promoRewardEvents = jdbc.query("select * from promo_reward", mapRowToPromoReward());
        assertThat(promoRewardEvents.get(0), equalTo(event));
        assertThat(promoRewardEvents.get(1), equalTo(eventTwo));
    }

    private RowMapper<PromoRewardEvent> mapRowToPromoReward() {
        return new RowMapper<PromoRewardEvent>() {
            @Override
            public PromoRewardEvent mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final PromoRewardEvent event = new PromoRewardEvent(rs.getBigDecimal(1), rs.getLong(2), new DateTime(rs.getTimestamp(3)));
                return event;
            }
        };
    }

}
