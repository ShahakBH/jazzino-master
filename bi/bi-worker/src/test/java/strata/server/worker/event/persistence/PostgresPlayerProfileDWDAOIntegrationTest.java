package strata.server.worker.event.persistence;

import com.google.common.collect.Lists;
import com.yazino.platform.Partner;
import com.yazino.platform.event.message.PlayerProfileEvent;
import com.yazino.platform.player.PlayerProfileStatus;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import static com.yazino.platform.Partner.YAZINO;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext

public class PostgresPlayerProfileDWDAOIntegrationTest {

    private static final BigDecimal ID_1 = BigDecimal.valueOf(-1);
    private static final BigDecimal ID_2 = BigDecimal.valueOf(-2);

    @Autowired
    private JdbcTemplate jdbc;

    private PostgresPlayerProfileDWDAO underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        underTest = new PostgresPlayerProfileDWDAO(jdbc);
    }

    @Before
    @After
    public void cleanUp() {
        jdbc.update("DELETE FROM LOBBY_USER WHERE PLAYER_ID IN (?,?)", ID_1, ID_2);
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);

        underTest = new PostgresPlayerProfileDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(Lists.newArrayList(aPlayerProfile(ID_1)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresPlayerProfileDWDAO(mockTemplate);
        when(mockTemplate.update(Mockito.anyString(), Mockito.<PreparedStatementSetter>any())).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(Lists.newArrayList(aPlayerProfile(ID_1)));
    }

    @Test
    public void anEventIsSavedToTheDatabase() {
        underTest.saveAll(Lists.newArrayList(aPlayerProfile(ID_1)));

        verifyRecordMatches(aPlayerProfile(ID_1), readRecordByAccountId(ID_1));
    }


    @Test
    public void anEventWithoutDateOfBirthIsSavedToTheDatabase() {
        final PlayerProfileEvent expected = aPlayerProfile(ID_1);
        expected.setDateOfBirth(null);
        underTest.saveAll(Lists.newArrayList(expected));

        verifyRecordMatches(expected, readRecordByAccountId(ID_1));
    }

    @Test
    public void anEventWithoutGenderIsSavedToTheDatabase() {
        final PlayerProfileEvent expected = aPlayerProfile(ID_1);
        expected.setGender(null);
        underTest.saveAll(Lists.newArrayList(expected));

        verifyRecordMatches(expected, readRecordByAccountId(ID_1));
    }

    @Test
    public void genderIsTruncatedBeforeSavingToDatabase() {
        final PlayerProfileEvent original = aPlayerProfile(ID_1);
        original.setGender("Macho");
        final PlayerProfileEvent expected = aPlayerProfile(ID_1);
        expected.setGender("M");
        underTest.saveAll(Lists.newArrayList(original));

        verifyRecordMatches(expected, readRecordByAccountId(ID_1));
    }

    @Test
    public void multipleEventsMayBeSavedToTheDatabase() {
        underTest.saveAll(Lists.newArrayList(aPlayerProfile(ID_1), aPlayerProfile(ID_2)));

        verifyRecordMatches(aPlayerProfile(ID_1), readRecordByAccountId(ID_1));
        verifyRecordMatches(aPlayerProfile(ID_2), readRecordByAccountId(ID_2));
    }

    @Test
    public void anEventCanBeUpdated() {
        final PlayerProfileEvent original = aPlayerProfile(ID_1);

        underTest.saveAll(Lists.newArrayList(original));
        final PlayerProfileEvent updated = aPlayerProfile(ID_1);

        updated.setNewPlayer(Boolean.FALSE);
        updated.setDisplayName("display");
        updated.setRealName("real");
        updated.setFirstName("first");
        updated.setPictureLocation("new pic");
        updated.setEmail("new email");
        updated.setCountry("NZ");
        updated.setExternalId("new ext");
        updated.setVerificationIdentifier("another-verification-identifier");
        updated.setProviderName("new prov");
        updated.setStatus(PlayerProfileStatus.BLOCKED);
        updated.setDateOfBirth(new DateTime(1985, 1, 1, 0, 0, 0, 0));
        updated.setGender("F");
        updated.setInviteReferrerId("new ref");
        updated.setGuestStatus("C");
        updated.setPartnerId(Partner.TANGO);
        underTest.saveAll(Lists.newArrayList(updated));

        verifyRecordMatches(updated, readRecordByAccountId(ID_1));
    }

    @Test
    public void updatingAnEventShouldChangeRpxProviderField() {
        final PlayerProfileEvent expected = aPlayerProfile(ID_1);
        expected.setProviderName("ORIGINAL_PROVIDER");
        underTest.saveAll(Lists.newArrayList(expected));
        final PlayerProfileEvent updated = aPlayerProfile(ID_1);
        updated.setNewPlayer(Boolean.FALSE);
        updated.setProviderName("NEW_PROVIDER");
        underTest.saveAll(Lists.newArrayList(updated));
        final Map<String, Object> record = readRecordByAccountId(ID_1);
        assertEquals("NEW_PROVIDER", record.get("PROVIDER_NAME"));
        assertEquals("NEW_PROVIDER", record.get("RPX_PROVIDER"));
    }

    @Test
    public void registrationTimeCannotBeUpdated() throws Exception {
        final PlayerProfileEvent expected = aPlayerProfile(ID_1);
        underTest.saveAll(Lists.newArrayList(expected));
        final PlayerProfileEvent updated = aPlayerProfile(ID_1);
        updated.setNewPlayer(Boolean.FALSE);
        updated.setRegistrationTime(new DateTime(2014, 11, 11, 11, 11, 11, 0));
        underTest.saveAll(Lists.newArrayList(updated));

        verifyRecordMatches(expected, readRecordByAccountId(ID_1));
    }

    private PlayerProfileEvent aPlayerProfile(final BigDecimal id) {
        return new PlayerProfileEvent(id,
                new DateTime(2011, 11, 11, 11, 11, 11, 0),
                "aPlayer",
                "The Player",
                "Player",
                "pic",
                "email",
                "GB",
                "extId",
                "a-verification-identifier",
                "prov",
                PlayerProfileStatus.ACTIVE,
                YAZINO,
                new DateTime(1980, 11, 11, 0, 0, 0, 0),
                "M",
                "player_ref",
                "127.0.0.1",
                true,
                null,
                "G");
    }

    private void verifyRecordMatches(final PlayerProfileEvent playerEvent, final Map<String, Object> record) {
        assertThat(new BigDecimal(record.get("PLAYER_ID").toString()), is(comparesEqualTo(playerEvent.getPlayerId())));
        assertThat(getDateTime(record, "REG_TS"), is(equalTo(playerEvent.getRegistrationTime())));
        assertThat(getStringField(record, "DISPLAY_NAME"), is(equalTo(playerEvent.getDisplayName())));
        assertThat(getStringField(record, "REAL_NAME"), is(equalTo(playerEvent.getRealName())));
        assertThat(getStringField(record, "FIRST_NAME"), is(equalTo(playerEvent.getFirstName())));
        assertThat(getStringField(record, "PICTURE_LOCATION"), is(equalTo(playerEvent.getPictureLocation())));
        assertThat(getStringField(record, "EMAIL_ADDRESS"), is(equalTo(playerEvent.getEmail())));
        assertThat(getStringField(record, "COUNTRY"), is(equalTo(playerEvent.getCountry())));
        assertThat(getStringField(record, "EXTERNAL_ID"), is(equalTo(playerEvent.getExternalId())));
        assertThat(getStringField(record, "VERIFICATION_IDENTIFIER"), is(equalTo(playerEvent.getVerificationIdentifier())));
        assertThat(getStringField(record, "PROVIDER_NAME"), is(equalTo(playerEvent.getProviderName())));
        assertThat(PlayerProfileStatus.forId(getStringField(record, "STATUS")), is(equalTo(playerEvent.getStatus())));
        assertThat(getDate(record, "DATE_OF_BIRTH"), is(equalTo(playerEvent.getDateOfBirth())));
        assertThat(getStringField(record, "GENDER"), is(equalTo(playerEvent.getGender())));
        assertThat(getStringField(record, "GUEST_STATUS"), is(equalTo(playerEvent.getGuestStatus())));
        assertThat(getStringField(record, "PARTNER_ID"), is(equalTo(playerEvent.getPartnerId().name())));
    }


    private String getStringField(final Map<String, Object> record, final String fieldName) {
        final Object field = record.get(fieldName);
        if (field == null) {
            return null;
        }
        return field.toString();
    }

    private DateTime getDate(final Map<String, Object> record, final String fieldName) {
        final Date field = (Date) record.get(fieldName);
        if (field == null) {
            return null;
        }
        return new DateTime(field.getTime());
    }

    private DateTime getDateTime(final Map<String, Object> record, final String fieldName) {
        final Timestamp field = (Timestamp) record.get(fieldName);
        if (field == null) {
            return null;
        }
        return new DateTime(field.getTime());
    }

    private Map<String, Object> readRecordByAccountId(final BigDecimal playerId) {
        return jdbc.queryForMap("SELECT * FROM LOBBY_USER WHERE PLAYER_ID=?", playerId);
    }
}

