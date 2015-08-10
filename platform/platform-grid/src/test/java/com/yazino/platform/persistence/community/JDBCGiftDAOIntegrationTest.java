package com.yazino.platform.persistence.community;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.community.Gift;
import com.yazino.platform.util.BigDecimals;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@Transactional
public class JDBCGiftDAOIntegrationTest {
    private static final BigDecimal SENDER_ID = BigDecimal.valueOf(32000);
    private static final BigDecimal RECIPIENT_ID = BigDecimal.valueOf(64000);
    private static final int EXPIRY_HOURS = 2;
    private static final int GIFT_RETENTION = 168;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JDBCGiftDAO underTest;

    private DateTime firstCreated;
    private DateTime secondCreated;

    @Before
    public void setUp() {
        jdbcTemplate.update("DELETE FROM GIFTS");

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(341587562700L);

        firstCreated = new DateTime().withMillisOfSecond(0);
        secondCreated = new DateTime().withMillisOfSecond(0).minusHours(1);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void allGiftsCanBeReadFromTheDatabase() {
        jdbcTemplate.update("INSERT INTO GIFTS (GIFT_ID, SENDER_ID, RECEIVER_ID, CREATED_TS, EXPIRY_TS, COLLECTED_TS, ACKNOWLEDGED) "
                + "VALUES (?,?,?,?,?,?,?)", -1, SENDER_ID, RECIPIENT_ID, firstCreated.toDate(), firstCreated.plusHours(EXPIRY_HOURS).toDate(), null, false);
        jdbcTemplate.update("INSERT INTO GIFTS (GIFT_ID, SENDER_ID, RECEIVER_ID, CREATED_TS, EXPIRY_TS, COLLECTED_TS, ACKNOWLEDGED) "
                + "VALUES (?,?,?,?,?,?,?)", -2, SENDER_ID, RECIPIENT_ID, secondCreated.toDate(), secondCreated.plusHours(EXPIRY_HOURS).toDate(), null, false);

        final Set<Gift> gifts = extractFrom(underTest.iterateAll());

        assertThat(gifts, containsInAnyOrder(aGift(-1, firstCreated), aGift(-2, secondCreated)));
    }

    @Test
    public void allGiftsBeyondTheRetentionThresholdAreNotRead() {
        final DateTime retentionDateTime = new DateTime().minusHours(GIFT_RETENTION + 1).withMillisOfSecond(0);
        jdbcTemplate.update("INSERT INTO GIFTS (GIFT_ID, SENDER_ID, RECEIVER_ID, CREATED_TS, EXPIRY_TS, COLLECTED_TS, ACKNOWLEDGED) "
                + "VALUES (?,?,?,?,?,?,?)", -1, SENDER_ID, RECIPIENT_ID, firstCreated.toDate(), firstCreated.plusHours(EXPIRY_HOURS).toDate(), null, false);
        jdbcTemplate.update("INSERT INTO GIFTS (GIFT_ID, SENDER_ID, RECEIVER_ID, CREATED_TS, EXPIRY_TS, COLLECTED_TS, ACKNOWLEDGED) "
                + "VALUES (?,?,?,?,?,?,?)", -2, SENDER_ID, RECIPIENT_ID, retentionDateTime.toDate(), retentionDateTime.plusHours(EXPIRY_HOURS).toDate(), null, false);

        final Set<Gift> gifts = extractFrom(underTest.iterateAll());

        assertThat(gifts, containsInAnyOrder(aGift(-1, firstCreated)));
    }

    @Test
    public void aCollectedGiftCanBeReadFromTheDatabase() {
        jdbcTemplate.update("INSERT INTO GIFTS (GIFT_ID, SENDER_ID, RECEIVER_ID, CREATED_TS, EXPIRY_TS, COLLECTED_TS, ACKNOWLEDGED) "
                + "VALUES (?,?,?,?,?,?,?)", -1, SENDER_ID, RECIPIENT_ID, firstCreated.toDate(), firstCreated.plusHours(EXPIRY_HOURS).toDate(),
                firstCreated.plusHours(1).toDate(), false);

        final Set<Gift> gifts = extractFrom(underTest.iterateAll());

        final Gift expectedGift = aGift(-1, firstCreated);
        expectedGift.setCollected(firstCreated.plusHours(1));
        assertThat(gifts, contains(expectedGift));
    }

    @Test
    public void anAcknowledgedGiftCanBeReadFromTheDatabase() {
        jdbcTemplate.update("INSERT INTO GIFTS (GIFT_ID, SENDER_ID, RECEIVER_ID, CREATED_TS, EXPIRY_TS, COLLECTED_TS, ACKNOWLEDGED) "
                + "VALUES (?,?,?,?,?,?,?)", -1, SENDER_ID, RECIPIENT_ID, firstCreated.toDate(), firstCreated.plusHours(EXPIRY_HOURS).toDate(),
                null, true);

        final Set<Gift> gifts = extractFrom(underTest.iterateAll());

        final Gift expectedGift = aGift(-1, firstCreated);
        expectedGift.setAcknowledged(true);
        assertThat(gifts, contains(expectedGift));
    }

    @Test
    public void aGiftCanBeSaved() {
        underTest.save(aGift(-1, firstCreated));

        final Gift expectedGift = aGift(-1, firstCreated);
        final Map<String,Object> dbGift = jdbcTemplate.queryForMap("SELECT * FROM GIFTS WHERE GIFT_ID=?", expectedGift.getId());
        assertThat(((Number) dbGift.get("gift_id")).longValue(), is(equalTo((Number) expectedGift.getId().longValue())));
        assertThat(BigDecimals.strip((BigDecimal) dbGift.get("sender_id")), is(equalTo(expectedGift.getSendingPlayer())));
        assertThat(BigDecimals.strip((BigDecimal) dbGift.get("receiver_id")), is(equalTo(expectedGift.getRecipientPlayer())));
        assertThat(((Date) dbGift.get("created_ts")).getTime(), is(equalTo(expectedGift.getCreated().getMillis())));
        assertThat(((Date) dbGift.get("expiry_ts")).getTime(), is(equalTo(expectedGift.getExpiry().getMillis())));
        assertThat(dbGift.get("collected_ts"), is(nullValue()));
        assertThat((Boolean) dbGift.get("acknowledged"), is(equalTo(expectedGift.getAcknowledged())));
    }

    @Test
    public void aGiftCanBeUpdated() {
        final Gift gift = aGift(-1, firstCreated);
        underTest.save(gift);

        gift.setAcknowledged(true);
        gift.setCollected(new DateTime().withMillisOfSecond(0).plusHours(1));
        underTest.save(gift);

        final Map<String,Object> dbGift = jdbcTemplate.queryForMap("SELECT * FROM GIFTS WHERE GIFT_ID=?", gift.getId());
        assertThat(((Number) dbGift.get("gift_id")).longValue(), is(equalTo((Number) gift.getId().longValue())));
        assertThat(BigDecimals.strip((BigDecimal) dbGift.get("sender_id")), is(equalTo(gift.getSendingPlayer())));
        assertThat(BigDecimals.strip((BigDecimal) dbGift.get("receiver_id")), is(equalTo(gift.getRecipientPlayer())));
        assertThat(((Date) dbGift.get("created_ts")).getTime(), is(equalTo(gift.getCreated().getMillis())));
        assertThat(((Date) dbGift.get("expiry_ts")).getTime(), is(equalTo(gift.getExpiry().getMillis())));
        assertThat(((Date) dbGift.get("collected_ts")).getTime(), is(equalTo(gift.getCollected().getMillis())));
        assertThat((Boolean) dbGift.get("acknowledged"), is(equalTo(gift.getAcknowledged())));
    }

    @Test
    public void aGiftsSenderRecipientCreatedAndExpiryCannotBeUpdated() {
        final Gift gift = aGift(-1, firstCreated);
        underTest.save(gift);

        gift.setAcknowledged(true);
        gift.setCollected(new DateTime().withMillisOfSecond(0).plusHours(1));
        gift.setSendingPlayer(BigDecimal.valueOf(8888));
        gift.setRecipientPlayer(BigDecimal.valueOf(9999));
        gift.setCreated(secondCreated);
        gift.setExpiry(secondCreated.plusHours(EXPIRY_HOURS));
        underTest.save(gift);

        final Gift expectedGift = aGift(-1, firstCreated);
        expectedGift.setAcknowledged(true);
        expectedGift.setCollected(new DateTime().withMillisOfSecond(0).plusHours(1));
        final Map<String,Object> dbGift = jdbcTemplate.queryForMap("SELECT * FROM GIFTS WHERE GIFT_ID=?", expectedGift.getId());
        assertThat(((Number) dbGift.get("gift_id")).longValue(), is(equalTo((Number) expectedGift.getId().longValue())));
        assertThat(BigDecimals.strip((BigDecimal) dbGift.get("sender_id")), is(equalTo(expectedGift.getSendingPlayer())));
        assertThat(BigDecimals.strip((BigDecimal) dbGift.get("receiver_id")), is(equalTo(expectedGift.getRecipientPlayer())));
        assertThat(((Date) dbGift.get("created_ts")).getTime(), is(equalTo(expectedGift.getCreated().getMillis())));
        assertThat(((Date) dbGift.get("expiry_ts")).getTime(), is(equalTo(expectedGift.getExpiry().getMillis())));
        assertThat(((Date) dbGift.get("collected_ts")).getTime(), is(equalTo(expectedGift.getCollected().getMillis())));
        assertThat((Boolean) dbGift.get("acknowledged"), is(equalTo(expectedGift.getAcknowledged())));
    }

    private Set<Gift> extractFrom(final DataIterator<Gift> iterator) {
        final Set<Gift> gifts = new HashSet<>();
        while (iterator.hasNext()) {
            gifts.add(iterator.next());
        }
        return gifts;
    }

    private Gift aGift(final long id,
                       final DateTime created) {
        return new Gift(BigDecimal.valueOf(id), SENDER_ID, RECIPIENT_ID, created, created.plusHours(EXPIRY_HOURS), null, false);
    }
}
