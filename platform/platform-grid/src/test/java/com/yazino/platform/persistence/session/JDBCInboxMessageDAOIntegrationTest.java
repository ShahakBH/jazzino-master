package com.yazino.platform.persistence.session;

import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.persistence.SequenceGenerator;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.game.api.time.SettableTimeSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCInboxMessageDAOIntegrationTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(45348L);
    private static final NewsEvent NEWS_EVENT = new NewsEvent.Builder(PLAYER_ID, new ParameterisedMessage("You got %s messages", "38")).setType(NewsEventType.NEWS).setImage("image").build();

    private BigDecimal playerAccountId;

    @Autowired(required = true)
    private InboxMessageDAO inboxMessageDAO;

    @Autowired
    private SequenceGenerator sequenceGenerator;

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private final NewsEventSerializer newsSerializer = new NewsEventSerializer();

    private SettableTimeSource timeSource = new SettableTimeSource();

    @Before
    @Transactional
    public void setUp() {
        playerAccountId = sequenceGenerator.next();
        jdbcTemplate.update("INSERT INTO ACCOUNT(ACCOUNT_ID,NAME) values (?,'inboxtestaccount1')", playerAccountId);
        jdbcTemplate.update("INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME) values (?,?)", PLAYER_ID, "YAZINO");
        jdbcTemplate.update("insert into PLAYER(PLAYER_ID,ACCOUNT_ID) values (?,?)", PLAYER_ID, playerAccountId);
    }

    @Test
    @Transactional
    public void shouldSaveMessage() {
        inboxMessageDAO.save(new InboxMessage(PLAYER_ID, NEWS_EVENT, new DateTime()));
        final Map persistedMessage = jdbcTemplate.queryForMap("SELECT * FROM PLAYER_INBOX WHERE PLAYER_ID=?", PLAYER_ID);

        assertThat(persistedMessage, is(not(nullValue())));
        assertThat(((BigDecimal) persistedMessage.get("PLAYER_ID")).longValue(), is(equalTo(PLAYER_ID.longValue())));
        assertThat((Boolean) persistedMessage.get("IS_READ"), is(equalTo(false)));
        assertThat(persistedMessage.get("RECEIVED_TIME"), is(not(nullValue())));
        assertThat((String) persistedMessage.get("MESSAGE"), is(equalTo(newsSerializer.serialize(NEWS_EVENT))));
    }

    @Test
    @Transactional
    public void shouldGetMessagesOrderedByReceivedTime() {
        InboxMessage message1 = createNewMessage(PLAYER_ID, NEWS_EVENT);
        InboxMessage message2 = createNewMessage(PLAYER_ID, NEWS_EVENT);
        InboxMessage message3 = createNewMessage(PLAYER_ID, NEWS_EVENT);
        final List<InboxMessage> expected = Arrays.asList(message1, message2, message3);
        final List<InboxMessage> actual = inboxMessageDAO.findUnreadMessages(PLAYER_ID);
        assertEquals(expected, actual);
    }

    private InboxMessage createNewMessage(BigDecimal playerId, NewsEvent newsEvent) {
        timeSource.addMillis(5000);
        final InboxMessage message = new InboxMessage(playerId, newsEvent, new DateTime(timeSource.getCurrentTimeStamp()));
        inboxMessageDAO.save(message);
        return message;
    }

    @After
    @Transactional
    public void tearDown() {
        jdbcTemplate.update("delete from PLAYER_INBOX where PLAYER_ID=?", PLAYER_ID);
        jdbcTemplate.update("delete from PLAYER where PLAYER_ID=?", PLAYER_ID);
        jdbcTemplate.update("delete from ACCOUNT where ACCOUNT_ID=?", playerAccountId);
    }
}
