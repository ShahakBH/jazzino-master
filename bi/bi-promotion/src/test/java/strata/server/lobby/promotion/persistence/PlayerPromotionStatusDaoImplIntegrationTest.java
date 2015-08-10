package strata.server.lobby.promotion.persistence;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;
import strata.server.lobby.api.promotion.domain.builder.PlayerPromotionStatusBuilder;
import strata.server.lobby.api.promotion.domain.builder.PlayerPromotionStatusTestBuilder;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@TransactionConfiguration
@Transactional
public class PlayerPromotionStatusDaoImplIntegrationTest {

    public static final BigDecimal PLAYER_ID = new BigDecimal(-10);

    @Autowired
    @Qualifier("marketingJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private PlayerPromotionStatusDaoImplJdbc underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new PlayerPromotionStatusDaoImplJdbc(jdbcTemplate);
        jdbcTemplate.execute("DELETE FROM PLAYER_PROMOTION_STATUS WHERE PLAYER_ID IN (-10,-15,-17)");
    }

    @Test
    public void getShouldReturnCorrectExistingPlayerPromotionStatus() throws Exception {

        jdbcTemplate.execute("INSERT INTO PLAYER_PROMOTION_STATUS (PLAYER_ID, LAST_TOPUP_DATE, LAST_PLAYED_DATE, CONSECUTIVE_PLAY_DAYS, TOP_UP_ACKNOWLEDGED) "
                + "VALUES (-10, '2012-03-02 18:50:00', '2012-03-05 18:50:00', 3, true)");

        final PlayerPromotionStatus expectedPlayerPromotionStatus = PlayerPromotionStatusTestBuilder.create().withTopUpAcknowledged(true).build();

        assertEquals(expectedPlayerPromotionStatus, underTest.get(expectedPlayerPromotionStatus.getPlayerId()));
    }

    @Test
    public void getShouldReturnCorrectExistingPlayerPromotionStatusWhenLastTopupDateIsNull() throws Exception {
        jdbcTemplate.execute("INSERT INTO PLAYER_PROMOTION_STATUS (PLAYER_ID, LAST_TOPUP_DATE, LAST_PLAYED_DATE, CONSECUTIVE_PLAY_DAYS, TOP_UP_ACKNOWLEDGED) "
                + "VALUES (-15, null , '2012-03-05 18:50:00', 3, true)");

        final PlayerPromotionStatus expectedPlayerPromotionStatus = PlayerPromotionStatusTestBuilder.create().withTopUpAcknowledged(true).withPlayerId(
                new BigDecimal(-15))
                .withLastTopupDateAsTimestamp(null).build();

        assertEquals(expectedPlayerPromotionStatus, underTest.get(expectedPlayerPromotionStatus.getPlayerId()));
    }

    @Test
    public void getShouldReturnCorrectExistingPlayerPromotionStatusWhenLastPlayedIsNull() throws Exception {
        jdbcTemplate.execute("INSERT INTO PLAYER_PROMOTION_STATUS (PLAYER_ID, LAST_TOPUP_DATE, LAST_PLAYED_DATE, CONSECUTIVE_PLAY_DAYS, TOP_UP_ACKNOWLEDGED) "
                + "VALUES (-17, '2012-03-02 18:50:00', null , 3, true)");

        final PlayerPromotionStatus expectedPlayerPromotionStatus = PlayerPromotionStatusTestBuilder.create().withTopUpAcknowledged(true).withPlayerId(new BigDecimal(-17))
                .withLastPlayedDateAsTimestamp(null).build();

        assertEquals(expectedPlayerPromotionStatus, underTest.get(expectedPlayerPromotionStatus.getPlayerId()));
    }

    @Test
    public void getShouldReturnCorrectExistingPlayerPromotionStatusWhenTopUpAcknowlegedIsFalse() throws Exception {
        jdbcTemplate.execute("INSERT INTO PLAYER_PROMOTION_STATUS (PLAYER_ID, LAST_TOPUP_DATE, LAST_PLAYED_DATE, CONSECUTIVE_PLAY_DAYS, TOP_UP_ACKNOWLEDGED) "
                + "VALUES (-17, '2012-03-02 18:50:00', null , 3, false)");

        final PlayerPromotionStatus expectedPlayerPromotionStatus = PlayerPromotionStatusTestBuilder.create()
                .withPlayerId(new BigDecimal(-17))
                .withTopUpAcknowledged(false)
                .withLastPlayedDateAsTimestamp(null)
                .build();

        assertEquals(expectedPlayerPromotionStatus, underTest.get(expectedPlayerPromotionStatus.getPlayerId()));
    }

    @Test
    public void getShouldReturnDefaultPlayerPromotionStatus() throws Exception {
        final PlayerPromotionStatus expectedPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(new BigDecimal(-2342))
                .build();

        assertEquals(expectedPlayerPromotionStatus, underTest.get(expectedPlayerPromotionStatus.getPlayerId()));
    }

    @Test
    public void saveShouldInsertCorrectValuesIntoPlayerPromotionStatus() throws Exception {

        final PlayerPromotionStatus expectedPlayerPromotionStatus = PlayerPromotionStatusTestBuilder.create().build();
        underTest.save(expectedPlayerPromotionStatus);

        final SqlRowSet rowSet = jdbcTemplate.queryForRowSet("SELECT * FROM PLAYER_PROMOTION_STATUS WHERE PLAYER_ID = -10");
        assertTrue(rowSet.next());
        assertThat(rowSet.getBigDecimal("PLAYER_ID"), is(comparesEqualTo(expectedPlayerPromotionStatus.getPlayerId())));
        assertEquals(expectedPlayerPromotionStatus.getLastPlayed(), new DateTime(rowSet.getTimestamp("LAST_PLAYED_DATE")));
        assertEquals(expectedPlayerPromotionStatus.getLastTopup(), new DateTime(rowSet.getTimestamp("LAST_TOPUP_DATE")));
        assertEquals(expectedPlayerPromotionStatus.getConsecutiveDaysPlayed(), rowSet.getInt("CONSECUTIVE_PLAY_DAYS"));
    }

    @Test
    public void saveShouldInsertAcknowledgeFalseIntoPlayerPromotionStatus() throws Exception {
        final PlayerPromotionStatus expectedPlayerPromotionStatus = PlayerPromotionStatusTestBuilder
                .create()
                .withTopUpAcknowledged(false)
                .build();
        underTest.save(expectedPlayerPromotionStatus);

        final SqlRowSet rowSet = jdbcTemplate.queryForRowSet("SELECT * FROM PLAYER_PROMOTION_STATUS WHERE PLAYER_ID = -10");
        assertTrue(rowSet.next());
        assertFalse(rowSet.getBoolean("TOP_UP_ACKNOWLEDGED"));
    }

    @Test
    public void saveShouldInsertAcknowledgeTrueIntoPlayerPromotionStatus() throws Exception {
        final PlayerPromotionStatus expectedPlayerPromotionStatus = PlayerPromotionStatusTestBuilder
                .create()
                .withTopUpAcknowledged(true)
                .build();
        underTest.save(expectedPlayerPromotionStatus);

        final SqlRowSet rowSet = jdbcTemplate.queryForRowSet("SELECT * FROM PLAYER_PROMOTION_STATUS WHERE PLAYER_ID = -10");
        assertTrue(rowSet.next());
        assertTrue(rowSet.getBoolean("TOP_UP_ACKNOWLEDGED"));
    }

    @Test
    public void saveShouldUpdateExistingPlayerPromotionStatus() throws Exception {
        jdbcTemplate.execute("INSERT INTO PLAYER_PROMOTION_STATUS (PLAYER_ID, LAST_TOPUP_DATE, LAST_PLAYED_DATE, CONSECUTIVE_PLAY_DAYS, TOP_UP_ACKNOWLEDGED) "
                + "VALUES (-10, '2012-03-04 13:50:47', '2012-03-04 18:50:00', 1, FALSE )");

        final PlayerPromotionStatus playerPromotionStatus = PlayerPromotionStatusTestBuilder.create().build();
        PlayerPromotionStatus expectedPlayerPromotionStatus = underTest.save(playerPromotionStatus);

        final SqlRowSet rowSet = jdbcTemplate.queryForRowSet("SELECT * FROM PLAYER_PROMOTION_STATUS WHERE PLAYER_ID = -10");
        assertTrue(rowSet.next());
        assertThat(rowSet.getBigDecimal("PLAYER_ID"), is(comparesEqualTo(expectedPlayerPromotionStatus.getPlayerId())));
        assertEquals(expectedPlayerPromotionStatus.getLastPlayed(), new DateTime(rowSet.getTimestamp("LAST_PLAYED_DATE")));
        assertEquals(expectedPlayerPromotionStatus.getLastTopup(), new DateTime(rowSet.getTimestamp("LAST_TOPUP_DATE")));
        assertEquals(expectedPlayerPromotionStatus.getConsecutiveDaysPlayed(), rowSet.getInt("CONSECUTIVE_PLAY_DAYS"));
    }

    @Test
    public void saveShouldSaveNullAsLastTopupIfLastTopupIsNull() throws Exception {

        final PlayerPromotionStatus expectedPlayerPromotionStatus = PlayerPromotionStatusTestBuilder.create()
                .withLastTopupDate(null)
                .build();

        underTest.save(expectedPlayerPromotionStatus);

        final SqlRowSet rowSet = jdbcTemplate.queryForRowSet("SELECT * FROM PLAYER_PROMOTION_STATUS WHERE PLAYER_ID = -10");
        assertTrue(rowSet.next());
        assertThat(rowSet.getBigDecimal("PLAYER_ID"), is(comparesEqualTo(expectedPlayerPromotionStatus.getPlayerId())));
        assertEquals(expectedPlayerPromotionStatus.getLastPlayed(), new DateTime(rowSet.getTimestamp("LAST_PLAYED_DATE")));
        assertEquals(expectedPlayerPromotionStatus.getLastTopup(), rowSet.getObject("LAST_TOPUP_DATE"));
        assertEquals(expectedPlayerPromotionStatus.getConsecutiveDaysPlayed(), rowSet.getInt("CONSECUTIVE_PLAY_DAYS"));
    }

    @Test
    public void saveShouldSaveNullAsLastPlayedIfLastTopUpIsNull() throws Exception {

        final PlayerPromotionStatus expectedPlayerPromotionStatus = PlayerPromotionStatusTestBuilder.create()
                .withLastPlayed(null)
                .build();

        underTest.save(expectedPlayerPromotionStatus);

        final SqlRowSet rowSet = jdbcTemplate.queryForRowSet("SELECT * FROM PLAYER_PROMOTION_STATUS WHERE PLAYER_ID = -10");
        assertTrue(rowSet.next());
        assertThat(rowSet.getBigDecimal("PLAYER_ID"), is(comparesEqualTo(expectedPlayerPromotionStatus.getPlayerId())));
        assertEquals(expectedPlayerPromotionStatus.getLastPlayed(), rowSet.getObject("LAST_PLAYED_DATE"));
        assertEquals(expectedPlayerPromotionStatus.getLastTopup(), new DateTime(rowSet.getTimestamp("LAST_TOPUP_DATE")));
        assertEquals(expectedPlayerPromotionStatus.getConsecutiveDaysPlayed(), rowSet.getInt("CONSECUTIVE_PLAY_DAYS"));
    }


    @Test
    public void saveAcknowledgeRequestForPlayerShouldUpdateDatabase() {
        final DateTime lastTopUpDate = new DateTime().withMillisOfSecond(0);
        jdbcTemplate.update("INSERT INTO PLAYER_PROMOTION_STATUS (PLAYER_ID, LAST_TOPUP_DATE, LAST_PLAYED_DATE, CONSECUTIVE_PLAY_DAYS, TOP_UP_ACKNOWLEDGED) "
                + "VALUES (-10, ?, '2012-03-04 18:50:00', 0, 0)", lastTopUpDate.toDate());

        underTest.saveAcknowledgeTopUpForPlayer(PLAYER_ID, lastTopUpDate);


        assertTrue(jdbcTemplate.queryForObject("SELECT TOP_UP_ACKNOWLEDGED FROM PLAYER_PROMOTION_STATUS WHERE PLAYER_ID=-10", Boolean.class));
    }

    @Test
    public void saveAcknowledgeRequestforPlayerShouldIgnoreRequestIfLastTopUpDateIsDifferent() {
        final DateTime lastTopUpDate = new DateTime();
        jdbcTemplate.update("INSERT INTO PLAYER_PROMOTION_STATUS (PLAYER_ID, LAST_TOPUP_DATE, LAST_PLAYED_DATE, CONSECUTIVE_PLAY_DAYS, TOP_UP_ACKNOWLEDGED) "
                + "VALUES (-10, ?, '2012-03-04 18:50:00', 0, 0)", lastTopUpDate.toDate());

        underTest.saveAcknowledgeTopUpForPlayer(PLAYER_ID, lastTopUpDate.plusHours(3));

        assertFalse(jdbcTemplate.queryForObject(
                "SELECT TOP_UP_ACKNOWLEDGED FROM PLAYER_PROMOTION_STATUS WHERE PLAYER_ID=-10", Boolean.class));
    }
}
