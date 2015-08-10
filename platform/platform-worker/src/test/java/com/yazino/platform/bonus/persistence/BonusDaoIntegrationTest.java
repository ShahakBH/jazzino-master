package com.yazino.platform.bonus.persistence;

import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.sql.Timestamp;

import static java.math.BigDecimal.valueOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class BonusDaoIntegrationTest {

    private static final BigDecimal PLAYER_ID = valueOf(666);
    Long now = now().getMillis();

    @Autowired
    JdbcTemplate jdbcTemplate;

    private BonusDao underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(now().withMillisOfSecond(0).getMillis());
        underTest = new BonusDao(jdbcTemplate);
        jdbcTemplate.update("delete from LOCKOUT_BONUS where player_id=666");
    }

    @Test
    public void getLockoutShouldLoadLockoutForPlayerId() {

        jdbcTemplate.update("insert into LOCKOUT_BONUS (player_id, last_bonus) values(666,'2014-01-01');");
        final DateTime lockoutTime = underTest.getLastBonusTime(PLAYER_ID);
        assertThat(lockoutTime, equalTo(new DateTime(2014, 1, 1, 0, 0).withTimeAtStartOfDay()));
    }

    @Test
    public void noRecordShouldReturnNullLockoutForPlayer() {
        final DateTime lockoutTime = underTest.getLastBonusTime(PLAYER_ID);
        assertThat(lockoutTime, nullValue());
    }

    @Test
    public void setLockoutShouldInsertNewLockout() {
        final DateTime lockoutTime = now().plusHours(4);
        underTest.setLockoutTime(PLAYER_ID, lockoutTime);
        Timestamp ts = (Timestamp) jdbcTemplate.queryForMap("select * from LOCKOUT_BONUS where PLAYER_ID =666").get("last_bonus");
        assertThat(new DateTime(ts.getTime()), equalTo(lockoutTime));
    }

    @Test
    public void setLockoutShouldUpdateNewLockout() {
        jdbcTemplate.update("insert into LOCKOUT_BONUS (player_id, last_bonus) values(666,'2014-01-01');");
        final DateTime nowTime = now();
        final Long lockoutTime = nowTime.getMillis();
        underTest.setLockoutTime(PLAYER_ID, nowTime);
        Timestamp ts = (Timestamp) jdbcTemplate.queryForMap("select * from LOCKOUT_BONUS where PLAYER_ID =666").get("last_bonus");
        assertThat(ts.getTime(), equalTo(lockoutTime));
    }

}
