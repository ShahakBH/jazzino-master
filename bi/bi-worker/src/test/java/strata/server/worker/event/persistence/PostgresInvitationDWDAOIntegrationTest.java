package strata.server.worker.event.persistence;

import com.yazino.platform.event.message.InvitationEvent;
import com.yazino.platform.invitation.InvitationSource;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static utils.PostgresTestValueHelper.createAPlayer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
@TransactionConfiguration
@Transactional
public class PostgresInvitationDWDAOIntegrationTest {
    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresInvitationDWDAO underTest;

    private static final BigDecimal PLAYER_ID_1 = BigDecimal.valueOf(-1);
    private static final BigDecimal PLAYER_ID_2 = BigDecimal.valueOf(-2);

    @Before
    @After
    public void cleanUp() {
        jdbc.update("DELETE FROM invitations WHERE player_id IN (?, ?)", PLAYER_ID_1, PLAYER_ID_2);
        createAPlayer(jdbc, PLAYER_ID_1, PLAYER_ID_2);
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresInvitationDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(newArrayList(anEvent(PLAYER_ID_1, "ide")));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresInvitationDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(newArrayList(anEvent(PLAYER_ID_1, "ide")));
    }

    @Test
    public void shouldSaveInvitation() {
        final InvitationEvent event = anEvent(PLAYER_ID_1, "ide");
        underTest.saveAll(newArrayList(event));
        verifySavedEvent(event, PLAYER_ID_1, "ide");
    }

    @Test
    public void shouldSaveMinimalInvitation() {
        final InvitationEvent event = new InvitationEvent(PLAYER_ID_1,
                "recipient",
                InvitationSource.FACEBOOK,
                "status",
                null,
                null,
                null,
                null,
                null);
        underTest.saveAll(newArrayList(event));
        verifySavedEvent(event, PLAYER_ID_1, "recipient");
    }

    @Test
    public void shouldUpdateInvitation() {
        final InvitationEvent initialEvent = anEvent(PLAYER_ID_1, "ide");
        underTest.saveAll(newArrayList(initialEvent));
        final InvitationEvent updatedEvent = anEvent(PLAYER_ID_1, "ide");
        updatedEvent.setSource(InvitationSource.EMAIL);
        updatedEvent.setStatus("updated status");
        updatedEvent.setReward(BigDecimal.TEN);
        updatedEvent.setGameType("other game type");
        updatedEvent.setScreenSource("other screen source");
        updatedEvent.setCreatedTime(new DateTime(2012, 1, 2, 1, 1, 1, 0));
        updatedEvent.setUpdatedTime(new DateTime(2012, 1, 3, 1, 1, 1, 0));

        underTest.saveAll(newArrayList(updatedEvent));
        final Map<String, Object> result = readEventByPlayerIdAndRecipient(PLAYER_ID_1, "ide");
        //the following shouldn't be updated
        assertThat(new BigDecimal(result.get("player_id").toString()), comparesEqualTo(initialEvent.getIssuingPlayerId()));
        assertThat((String) result.get("recipient_identifier"), equalTo(initialEvent.getRecipientIdentifier()));
        assertThat((String) result.get("invited_from"), equalTo(initialEvent.getSource().name()));
        assertThat((Timestamp) result.get("created_ts"), equalTo(new Timestamp(initialEvent.getCreatedTime().getMillis())));
        assertThat((String) result.get("game_type"), equalTo(initialEvent.getGameType()));
        assertThat((String) result.get("screen_source"), equalTo(initialEvent.getScreenSource()));
        //the following should have been updated
        assertThat((String) result.get("status"), equalTo(updatedEvent.getStatus()));
        assertThat((Integer) result.get("reward"), equalTo(updatedEvent.getReward().intValue()));
        assertThat((Timestamp) result.get("updated_ts"), equalTo(new Timestamp(updatedEvent.getUpdatedTime().getMillis())));
    }

    private Map<String, Object> readEventByPlayerIdAndRecipient(final BigDecimal playerId, final String recipient) {
        return jdbc.queryForMap("select * from invitations where player_id = ? and recipient_identifier = ?", playerId, recipient);
    }

    private InvitationEvent anEvent(final BigDecimal playerId, String recipient) {
        return new InvitationEvent(playerId,
                recipient,
                InvitationSource.EMAIL,
                "status",
                null,
                "gameType",
                "screenSource",
                new DateTime(2012, 1, 1, 1, 1, 1, 0),
                new DateTime(2012, 1, 1, 1, 1, 1, 0));
    }

    private void verifySavedEvent(final InvitationEvent event, BigDecimal playerId, String recipient) {
        final Map<String, Object> result = readEventByPlayerIdAndRecipient(playerId, recipient);
        assertThat((BigDecimal) result.get("player_id"), comparesEqualTo(event.getIssuingPlayerId()));
        assertThat((String) result.get("recipient_identifier"), equalTo(event.getRecipientIdentifier()));
        assertThat((String) result.get("invited_from"), equalTo(event.getSource().name()));
        assertThat((String) result.get("status"), equalTo(event.getStatus()));
        assertThat((Integer) result.get("reward"), equalTo(event.getReward() == null ? null : event.getReward().intValue()));
        if (event.getCreatedTime() != null) {
            assertThat((Timestamp) result.get("created_ts"), equalTo(new Timestamp(event.getCreatedTime().getMillis())));
        }
        if (event.getUpdatedTime() != null) {
            assertThat((Timestamp) result.get("updated_ts"), equalTo(new Timestamp(event.getUpdatedTime().getMillis())));
        }
        assertThat((String) result.get("game_type"), equalTo(event.getGameType()));
        assertThat((String) result.get("screen_source"), equalTo(event.getScreenSource()));
    }


}
