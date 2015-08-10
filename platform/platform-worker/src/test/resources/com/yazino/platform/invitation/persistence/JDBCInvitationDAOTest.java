package com.yazino.platform.invitation.persistence;

import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.invitation.InvitationStatus;
import org.hamcrest.MatcherAssert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;

import static com.yazino.platform.invitation.InvitationStatus.NOT_ACCEPTED;
import static com.yazino.platform.invitation.InvitationStatus.WAITING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "transactionManager")
public class JDBCInvitationDAOTest {

    static final String SQL_INSERT =
            "INSERT IGNORE INTO INVITATIONS "
                    + "(PLAYER_ID,RECIPIENT_IDENTIFIER,INVITED_FROM,STATUS,"
                    + "REWARD,CREATED_TS,UPDATED_TS,GAME_TYPE,SCREEN_SOURCE) VALUES (?,?,?,?,?,?,?,?,?)";

    private static final BigDecimal ISSUER_PLAYER_ID = BigDecimal.valueOf(100L);
    private static final BigDecimal ANOTHER_ISSUER_PLAYER_ID = BigDecimal.valueOf(101L);
    private static final String RECIPIENT_IDENTIFIER = "auser@somewhere.com";
    private static final InvitationSource RECIPIENT_SOURCE = InvitationSource.EMAIL;
    private static final DateTime CREATE_TIME = new DateTime(2009, 3, 30, 23, 45, 59, 0);
    private static final DateTime UPDATE_TIME = new DateTime(2010, 10, 9, 10, 3, 2, 0);
    private static final String GAME_TYPE = "GAME_TYPE";
    private static final String SCREEN_SOURCE = "FB_SCREEN";

    private static final InvitationStatus STATUS = WAITING;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    @Qualifier("invitationDAO")
    private JDBCInvitationDAO underTest;

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    @Transactional
    public void creatingANullInvitationShouldThrowANullPointerException() {
        underTest.save(null);
    }

    private Timestamp asTimestamp(final DateTime dateTime) {
        return new Timestamp(dateTime.getMillis());
    }

    @Test
    @Transactional
    public void shouldSaveInvitation() {
        final Invitation invitation = new Invitation(ISSUER_PLAYER_ID, RECIPIENT_IDENTIFIER + "testSave",
                RECIPIENT_SOURCE, STATUS, null,
                CREATE_TIME, UPDATE_TIME, GAME_TYPE, SCREEN_SOURCE);
        underTest.save(invitation);
        verifySavedEvent(invitation, ISSUER_PLAYER_ID, RECIPIENT_IDENTIFIER + "testSave");
    }

    @Test
    @Transactional
    public void shouldUpdateInvitation() {
        final Invitation initialEvent = new Invitation(ISSUER_PLAYER_ID, RECIPIENT_IDENTIFIER + "testUpdate",
                RECIPIENT_SOURCE, STATUS, null,
                CREATE_TIME, UPDATE_TIME, GAME_TYPE, null);
        underTest.save(initialEvent);
        final Invitation updatedEvent = new Invitation(ISSUER_PLAYER_ID, RECIPIENT_IDENTIFIER + "testUpdate",
                RECIPIENT_SOURCE, NOT_ACCEPTED, 2500l,
                CREATE_TIME.plusDays(1), UPDATE_TIME.plusDays(5), "another game type", SCREEN_SOURCE);
        underTest.save(updatedEvent);
        final Map<String, Object> result = readEventByPlayerIdAndRecipient(ISSUER_PLAYER_ID,
                RECIPIENT_IDENTIFIER + "testUpdate");
        //the following shouldn't be updated
        MatcherAssert.assertThat((Long) result.get("PLAYER_ID"), equalTo(initialEvent.getIssuingPlayerId().longValue
                ()));
        MatcherAssert.assertThat((String) result.get("RECIPIENT_IDENTIFIER"),
                equalTo(initialEvent.getRecipientIdentifier()));
        MatcherAssert.assertThat((String) result.get("INVITED_FROM"), equalTo(initialEvent.getSource().name()));
        MatcherAssert.assertThat((Timestamp) result.get("CREATED_TS"), equalTo(new Timestamp(initialEvent
                .getCreateTime().getMillis())));
        MatcherAssert.assertThat((String) result.get("GAME_TYPE"), equalTo(initialEvent.getGameType()));
        MatcherAssert.assertThat((String) result.get("SCREEN_SOURCE"), equalTo(initialEvent.getScreenSource()));
        //the following should have been updated
        MatcherAssert.assertThat((String) result.get("STATUS"), equalTo(updatedEvent.getStatus().name()));
        MatcherAssert.assertThat((Integer) result.get("REWARD"), equalTo(updatedEvent.getRewardAmount().intValue()));
        MatcherAssert.assertThat((Timestamp) result.get("UPDATED_TS"), equalTo(new Timestamp(updatedEvent
                .getUpdateTime().getMillis())));
    }

    private Map<String, Object> readEventByPlayerIdAndRecipient(final BigDecimal playerId, final String recipient) {
        return jdbc.queryForMap("SELECT * FROM INVITATIONS WHERE PLAYER_ID=? AND RECIPIENT_IDENTIFIER=?", playerId,
                recipient);
    }

    private void verifySavedEvent(final Invitation event, BigDecimal playerId, String recipient) {
        final Map<String, Object> result = readEventByPlayerIdAndRecipient(playerId, recipient);
        MatcherAssert.assertThat((Long) result.get("PLAYER_ID"), equalTo(event.getIssuingPlayerId().longValue()));
        MatcherAssert.assertThat((String) result.get("RECIPIENT_IDENTIFIER"), equalTo(event.getRecipientIdentifier()));
        MatcherAssert.assertThat((String) result.get("INVITED_FROM"), equalTo(event.getSource().name()));
        MatcherAssert.assertThat((String) result.get("STATUS"), equalTo(event.getStatus().name()));
        MatcherAssert.assertThat((Long) result.get("REWARD"), equalTo(event.getRewardAmount() == null ? null : event
                .getRewardAmount()));
        if (event.getCreateTime() != null) {
            MatcherAssert.assertThat((Timestamp) result.get("CREATED_TS"), equalTo(new Timestamp(event.getCreateTime
                    ().getMillis())));
        }
        if (event.getUpdateTime() != null) {
            MatcherAssert.assertThat((Timestamp) result.get("UPDATED_TS"), equalTo(new Timestamp(event.getUpdateTime
                    ().getMillis())));
        }
        MatcherAssert.assertThat((String) result.get("GAME_TYPE"), equalTo(event.getGameType()));
        MatcherAssert.assertThat((String) result.get("SCREEN_SOURCE"), equalTo(event.getScreenSource()));
    }

    @Test
    @Transactional
    public void shouldRetrieveInvitationsByRecipient() {
        final Invitation invitation1 = new Invitation(ISSUER_PLAYER_ID, RECIPIENT_IDENTIFIER + "testRetrieve",
                RECIPIENT_SOURCE, STATUS, null,
                CREATE_TIME, UPDATE_TIME, GAME_TYPE, null);
        final Invitation invitation2 = new Invitation(ANOTHER_ISSUER_PLAYER_ID,
                RECIPIENT_IDENTIFIER + "testRetrieve", RECIPIENT_SOURCE, STATUS, null,
                CREATE_TIME, UPDATE_TIME, null, SCREEN_SOURCE);
        final Invitation invitation3 = new Invitation(ANOTHER_ISSUER_PLAYER_ID,
                RECIPIENT_IDENTIFIER + "testRetrieve", InvitationSource.FACEBOOK, STATUS, null,
                CREATE_TIME, UPDATE_TIME, GAME_TYPE, SCREEN_SOURCE);
        final Invitation invitation4 = new Invitation(ANOTHER_ISSUER_PLAYER_ID,
                RECIPIENT_IDENTIFIER + "testRetrieve", RECIPIENT_SOURCE, STATUS, null,
                CREATE_TIME, UPDATE_TIME, GAME_TYPE, SCREEN_SOURCE);
        underTest.save(invitation1);
        underTest.save(invitation2);
        underTest.save(invitation3);
        underTest.save(invitation4);
        final Collection<Invitation> invitations = underTest.getInvitations(RECIPIENT_IDENTIFIER + "testRetrieve",
                RECIPIENT_SOURCE);
        assertThat(invitations, contains(invitation1, invitation2));
        assertThat(invitations, not(contains(invitation3, invitation4)));
    }

    @Test
    @Transactional
    public void shouldRetrieveInvitationsBySender() {
        final Invitation invitation1 = createInvitationFrom(ISSUER_PLAYER_ID).withSuffix(1);
        final Invitation invitation2 = createInvitationFrom(ISSUER_PLAYER_ID).withSuffix(2);
        final Invitation invitation3 = createInvitationFrom(ANOTHER_ISSUER_PLAYER_ID).withSuffix(3);

        underTest.save(invitation1);
        underTest.save(invitation2);
        underTest.save(invitation3);

        final Collection<Invitation> invitations = underTest.findInvitationsByIssuingPlayerId(ISSUER_PLAYER_ID);

        assertThat(invitations, contains(invitation1, invitation2));
        assertThat(invitations, not(contains(invitation3)));
    }

    @Test
    @Transactional
    public void findInvitationsByIssuingPlayerRecipientAndSource_shouldRetrieveMatchingInvite() {
        Invitation invitation = new Invitation(ISSUER_PLAYER_ID, RECIPIENT_IDENTIFIER, InvitationSource.FACEBOOK,
                STATUS, null, CREATE_TIME, UPDATE_TIME, GAME_TYPE, null);
        Invitation anotherRecipient = new Invitation(ISSUER_PLAYER_ID, RECIPIENT_IDENTIFIER + "2",
                InvitationSource.FACEBOOK,
                STATUS, null, CREATE_TIME, UPDATE_TIME, GAME_TYPE, null);
        Invitation anotherIssuer = new Invitation(ANOTHER_ISSUER_PLAYER_ID, RECIPIENT_IDENTIFIER,
                InvitationSource.FACEBOOK,
                STATUS, null, CREATE_TIME, UPDATE_TIME, GAME_TYPE, null);
        Invitation anotherSource = new Invitation(ISSUER_PLAYER_ID, RECIPIENT_IDENTIFIER, InvitationSource.EMAIL,
                STATUS, null, CREATE_TIME, UPDATE_TIME, GAME_TYPE, null);

        underTest.save(invitation);
        underTest.save(anotherRecipient);
        underTest.save(anotherIssuer);
        underTest.save(anotherSource);

        Invitation found = underTest.findInvitationsByIssuingPlayerRecipientAndSource(ISSUER_PLAYER_ID,
                RECIPIENT_IDENTIFIER, InvitationSource.FACEBOOK);

        assertEquals(invitation, found);
    }

    @Test
    @Transactional
    public void findInvitationsByIssuingPlayerRecipientAndSource_shouldReturnNullWhenNoMatchingInvite() {
        Invitation found = underTest.findInvitationsByIssuingPlayerRecipientAndSource(ISSUER_PLAYER_ID,
                RECIPIENT_IDENTIFIER, InvitationSource.FACEBOOK);

        assertNull(found);
    }

    @Test
    @Transactional
    public void getNumberOfAcceptedInvitations() {
        final Invitation invitation1 = createInvitationFrom(ISSUER_PLAYER_ID).withSuffix(1);
        invitation1.marksAsAccepted(5000l, UPDATE_TIME);
        final Invitation invitation2 = createInvitationFrom(ISSUER_PLAYER_ID).withSuffix(2);
        invitation2.marksAsAccepted(5000l, UPDATE_TIME);
        final Invitation invitation3 = createInvitationFrom(ISSUER_PLAYER_ID).withSuffix(3);
        invitation3.notAccepted(UPDATE_TIME);

        underTest.save(invitation1);
        underTest.save(invitation2);
        underTest.save(invitation3);

        int numberOfAcceptedInvitations = underTest.getNumberOfAcceptedInvites(ISSUER_PLAYER_ID, CREATE_TIME);
        assertThat(numberOfAcceptedInvitations, is(equalTo(2)));
    }


    private InvitationCreator createInvitationFrom(final BigDecimal playerId) {
        return new InvitationCreator(playerId);
    }

    private class InvitationCreator {
        private final BigDecimal playerId;

        private InvitationCreator(final BigDecimal playerId) {
            this.playerId = playerId;
        }

        public Invitation withSuffix(final int suffix) {
            final Invitation invitation =
                    new Invitation(playerId, RECIPIENT_IDENTIFIER + suffix, RECIPIENT_SOURCE, STATUS, null,
                            CREATE_TIME, UPDATE_TIME, GAME_TYPE, SCREEN_SOURCE);

            jdbc.update(SQL_INSERT, invitation.getIssuingPlayerId(), invitation.getRecipientIdentifier(), invitation
                    .getSource().name(), invitation.getStatus().name(), invitation.getRewardAmount(),
                    asTimestamp(invitation.getCreateTime()), asTimestamp(invitation.getUpdateTime()), GAME_TYPE,
                    SCREEN_SOURCE);

            return invitation;
        }
    }

}
