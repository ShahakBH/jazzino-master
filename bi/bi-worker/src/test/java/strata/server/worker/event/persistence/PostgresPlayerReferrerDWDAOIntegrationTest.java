package strata.server.worker.event.persistence;

import com.yazino.platform.Platform;
import com.yazino.platform.event.message.PlayerReferrerEvent;
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
import utils.PostgresTestValueHelper;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@Transactional
@DirtiesContext
public class PostgresPlayerReferrerDWDAOIntegrationTest {

    private static final BigDecimal ID_1 = BigDecimal.valueOf(-1);
    private static final BigDecimal ID_2 = BigDecimal.valueOf(-2);

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresPlayerReferrerDWDAO underTest;

    @Before
    public void cleanUp() {
//        jdbc.update("DELETE FROM PLAYER_REFERRER WHERE PLAYER_ID IN (?,?)", ID_1, ID_2);
        jdbc.update("DELETE FROM LOBBY_USER WHERE PLAYER_ID IN (?,?)", ID_1, ID_2);
    }

    @Before
    public void init() {
        PostgresTestValueHelper.createAPlayer(jdbc, ID_1, ID_2);
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);

        underTest = new PostgresPlayerReferrerDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(newArrayList(aPlayerReferrerEvent(ID_1)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresPlayerReferrerDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(newArrayList(aPlayerReferrerEvent(ID_1)));
    }

    @Test
    public void anEventIsSavedToTheDatabase() {
        underTest.saveAll(newArrayList(aPlayerReferrerEvent(ID_1)));

        verifyRecordMatches(aPlayerReferrerEvent(ID_1), readRecordByAccountId(ID_1));
    }

    @Test
    public void multipleEventsMayBeSavedToTheDatabase() {
        underTest.saveAll(newArrayList(aPlayerReferrerEvent(ID_1), aPlayerReferrerEvent(ID_2)));

        verifyRecordMatches(aPlayerReferrerEvent(ID_1), readRecordByAccountId(ID_1));
        verifyRecordMatches(aPlayerReferrerEvent(ID_2), readRecordByAccountId(ID_2));
    }

    @Test
    public void updatesAreSavedToTheDatabase() {
        PlayerReferrerEvent firstPlayerReferrerEvent = new PlayerReferrerEvent(ID_1, "facebook-ref", nameOf(Platform.IOS), "WHEELDEAL");
        PlayerReferrerEvent secondPlayerReferrerEvent = new PlayerReferrerEvent(ID_1, "somethingelse-ref", nameOf(Platform.ANDROID), "POKER");

        underTest.saveAll(newArrayList(firstPlayerReferrerEvent));
        underTest.saveAll(newArrayList(secondPlayerReferrerEvent));

        verifyRecordMatches(secondPlayerReferrerEvent, readRecordByAccountId(ID_1));
    }

    // for situations where we receive the mobile ad provider callback message before the mobile registration reg
    @Test
    public void updateShouldUpdateRecordButNotOverrideExistingNonNullGameType() {
        PlayerReferrerEvent firstPlayerReferrerEvent = new PlayerReferrerEvent(ID_1, "facebook-ref", nameOf(Platform.ANDROID), "WheelDeal");
        PlayerReferrerEvent secondPlayerReferrerEvent = new PlayerReferrerEvent(ID_1, null, nameOf(Platform.ANDROID), "WheelDeal");

        underTest.saveAll(newArrayList(firstPlayerReferrerEvent));
        underTest.saveAll(newArrayList(secondPlayerReferrerEvent));

        verifyRecordMatches(firstPlayerReferrerEvent, readRecordByAccountId(ID_1));

    }

    // for situations where we receive the mobile registration reg before the mobile add provider callback event
    @Test
    public void updateShouldUpdateRecordWithTheRefIfTheRefIsThere() {
        PlayerReferrerEvent firstPlayerReferrerEvent = new PlayerReferrerEvent(ID_1, null, nameOf(Platform.ANDROID), "WheelDeal");
        PlayerReferrerEvent secondPlayerReferrerEvent = new PlayerReferrerEvent(ID_1, "facebook-red", nameOf(Platform.ANDROID), "WheelDeal");

        underTest.saveAll(newArrayList(firstPlayerReferrerEvent));
        underTest.saveAll(newArrayList(secondPlayerReferrerEvent));

        verifyRecordMatches(secondPlayerReferrerEvent, readRecordByAccountId(ID_1));
    }

    @Test
    public void playerWithInviteReferrerShouldHaveTheirReferrerOverwritten() {
        PlayerReferrerEvent firstPlayerReferrerEvent = new PlayerReferrerEvent(ID_1, PlayerReferrerEvent.INVITE, null, null);
        PlayerReferrerEvent secondPlayerReferrerEvent = new PlayerReferrerEvent(ID_1, "adref", nameOf(Platform.ANDROID), "WheelDeal");

        underTest.saveAll(newArrayList(firstPlayerReferrerEvent));
        underTest.saveAll(newArrayList(secondPlayerReferrerEvent));

        verifyRecordMatches(secondPlayerReferrerEvent, readRecordByAccountId(ID_1));
    }

    @Test
    public void playerWithaNonInviteReferrerShouldNotBeOverwrittenByInvite() {
        PlayerReferrerEvent firstPlayerReferrerEvent = new PlayerReferrerEvent(ID_1, "", nameOf(Platform.ANDROID), "");
        PlayerReferrerEvent secondPlayerReferrerEvent = new PlayerReferrerEvent(ID_1, PlayerReferrerEvent.INVITE, null, null);

        underTest.saveAll(newArrayList(firstPlayerReferrerEvent));
        underTest.saveAll(newArrayList(secondPlayerReferrerEvent));

        verifyRecordMatches(new PlayerReferrerEvent(ID_1, PlayerReferrerEvent.INVITE, nameOf(Platform.ANDROID), ""), readRecordByAccountId(ID_1));

    }

    @Test
    public void playerWithNonInviteReferrerShouldHaveReferrerOverwrittenByNonInvite() {
        PlayerReferrerEvent firstPlayerReferrerEvent = new PlayerReferrerEvent(ID_1, "ADREF", nameOf(Platform.ANDROID), "WheelDeal");
        PlayerReferrerEvent secondPlayerReferrerEvent = new PlayerReferrerEvent(ID_1, "facebook-red", nameOf(Platform.ANDROID), "WheelDeal");

        underTest.saveAll(newArrayList(firstPlayerReferrerEvent));
        underTest.saveAll(newArrayList(secondPlayerReferrerEvent));

        verifyRecordMatches(secondPlayerReferrerEvent, readRecordByAccountId(ID_1));
    }

    private PlayerReferrerEvent aPlayerReferrerEvent(final BigDecimal id) {
        return new PlayerReferrerEvent(id, "facebook-ref", nameOf(Platform.IOS), "WHEELDEAL");
    }

    private void verifyRecordMatches(final PlayerReferrerEvent playerReferrerEvent, final Map<String, Object> record) {
        assertThat(new BigDecimal(record.get("PLAYER_ID").toString()), is(comparesEqualTo(playerReferrerEvent.getPlayerId())));
        assertThat(getStringField(record, "REGISTRATION_PLATFORM"), is(equalTo(playerReferrerEvent.getPlatform())));
        assertThat(getStringField(record, "REGISTRATION_GAME_TYPE"), is(equalTo(playerReferrerEvent.getGameType())));
        assertThat(getStringField(record, "REGISTRATION_REFERRER"), is(equalTo(playerReferrerEvent.getRef())));
    }

    private String nameOf(final Platform platform) {
        if (platform != null) {
            return platform.name();
        }
        return null;
    }

    private String getStringField(final Map<String, Object> record, final String fieldName) {
        final Object field = record.get(fieldName);
        if (field == null) {
            return null;
        }
        return field.toString();
    }

    private Map<String, Object> readRecordByAccountId(final BigDecimal playerId) {
        return jdbc.queryForMap("SELECT * FROM LOBBY_USER WHERE PLAYER_ID=?", playerId);
    }
}


