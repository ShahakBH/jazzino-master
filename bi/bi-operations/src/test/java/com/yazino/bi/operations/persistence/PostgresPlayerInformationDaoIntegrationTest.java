package com.yazino.bi.operations.persistence;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import com.yazino.bi.operations.model.Dashboard;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration
public class PostgresPlayerInformationDaoIntegrationTest {
    private static final BigDecimal ACCOUNT_ID = new BigDecimal("-7070.01");
    @Autowired
    private PlayerInformationDao underTest;

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate dwTemplate;

    @Before
    public void populate() {
        cleanUp();

        dwTemplate.update(
                "INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME,RPX_PROVIDER,EXTERNAL_ID,ACCOUNT_ID) VALUES(-7000.01,'FACEBOOK','RPX_PROVIDER','ABC',-7070.01)");
        dwTemplate.update("INSERT INTO ACCOUNT(ACCOUNT_ID, BALANCE) VALUES (-7070.01, 0.0100)");

        dwTemplate.update(
                "INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME,RPX_PROVIDER,EXTERNAL_ID,ACCOUNT_ID) VALUES(-7001.01,'FACEBOOK','RPX_PROVIDER','ABD',-7071)");
        dwTemplate.update("INSERT INTO ACCOUNT(ACCOUNT_ID, BALANCE) VALUES (-7071.01, 0.0100)");

        dwTemplate.update(
                "INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME,RPX_PROVIDER,EXTERNAL_ID,ACCOUNT_ID) VALUES(-7002.01,'YAZINO','RPX_PROVIDER','ABE',-7072.01)");
        dwTemplate.update("INSERT INTO ACCOUNT(ACCOUNT_ID, BALANCE) VALUES (-7072.01, 0.0100)");

        dwTemplate.update(
                "INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME,RPX_PROVIDER,EXTERNAL_ID,ACCOUNT_ID) VALUES(-7003.01,'YAZINO','RPX_PROVIDER','ABF',-7073.01)");
        dwTemplate.update("INSERT INTO ACCOUNT(ACCOUNT_ID, BALANCE) VALUES (-7073.01, 0.0100)");

        // NOTE this players external id matches several player ids
        dwTemplate.update(
                "INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME,RPX_PROVIDER,EXTERNAL_ID,ACCOUNT_ID) VALUES(-7004.01,'FACEBOOK','RPX_PROVIDER','twoplayerids',-7074.01)");
        dwTemplate.update("INSERT INTO ACCOUNT(ACCOUNT_ID, BALANCE) VALUES (-7074.01, 0.0100)");

        dwTemplate.update(
                "INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME,RPX_PROVIDER,EXTERNAL_ID,ACCOUNT_ID) VALUES(-7005.01,'FACEBOOK','RPX_PROVIDER','twoplayerids',-7075.01)");
        dwTemplate.update("INSERT INTO ACCOUNT(ACCOUNT_ID, BALANCE) VALUES (-7075.01, 0.0100)");

        // These players have registration errors
        dwTemplate.update(
                "INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME,RPX_PROVIDER,EXTERNAL_ID,ACCOUNT_ID) VALUES(-7006.01,'FACEBOOK','RPX_PROVIDER','facebookId',-7076.01)");
        dwTemplate.update("INSERT INTO ACCOUNT(ACCOUNT_ID, BALANCE) VALUES (-7076.01, 0.0100)");

        dwTemplate.update(
                "INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME,RPX_PROVIDER,EXTERNAL_ID,ACCOUNT_ID) VALUES(-7007.01,'YAZINO','RPX_PROVIDER',NULL,-7077.01)");
        dwTemplate.update("INSERT INTO ACCOUNT(ACCOUNT_ID, BALANCE) VALUES (-7077.01, 0.0100)");

    }

    @After
    public void cleanUp() {
        dwTemplate.update("DELETE FROM TRANSACTION_LOG");
        dwTemplate.update("DELETE FROM TABLE_DEFINITION WHERE TABLE_ID=-777.01");
        dwTemplate.update("DELETE FROM GAME_VARIATION_TEMPLATE WHERE GAME_VARIATION_TEMPLATE_ID=-778.01 ");
        dwTemplate.update("DELETE FROM LOBBY_USER WHERE PLAYER_ID < 0");
        dwTemplate.update("DELETE FROM EXTERNAL_TRANSACTION ");
        dwTemplate.update("DELETE FROM ACCOUNT WHERE ACCOUNT_ID < 0");
    }

    @Test
    public void shouldGetExternalPlayers() {
        // GIVEN the populated database

        // WHEN asking the DAO for a player ID
        final BigDecimal playerId = underTest.getPlayerId("FACEBOOK", "ABC");

        // THEN a correct internal ID is returned
        assertThat(playerId, is(equalTo(BigDecimal.valueOf(-7000.01))));
    }

    @Test
    public void shouldGetExternalPlayersWithLowercaseRpxId() {
        // GIVEN the populated database

        // WHEN asking the DAO for a player ID
        final BigDecimal playerId = underTest.getPlayerId("Facebook", "ABD");

        // THEN a correct internal ID is returned
        assertThat(playerId, is(BigDecimal.valueOf(-7001.01)));
    }

    @Test
    public void shouldGetExternalPlayersWithLowercaseExternalId() {
        // GIVEN the populated database

        // WHEN asking the DAO for a player ID
        final BigDecimal playerId = underTest.getPlayerId("FACEBOOK", "abd");

        // THEN a correct internal ID is returned
        assertThat(playerId, is(BigDecimal.valueOf(-7001.01)));
    }

    @Test
    public void shouldGetInternalPlayers() {
        // GIVEN the populated database

        // WHEN asking the DAO for a player ID
        final BigDecimal playerId = underTest.getPlayerId("YAZINO", "-7002.01");

        // THEN a correct internal ID is returned
        assertThat(playerId, is(BigDecimal.valueOf(-7002.01)));
    }

    @Test
    public void shouldGetInternalPlayersWithLowercaseRpx() {
        // GIVEN the populated database

        // WHEN asking the DAO for a player ID
        final BigDecimal playerId = underTest.getPlayerId("yazinO", "-7003.01");

        // THEN a correct internal ID is returned
        assertThat(playerId, is(BigDecimal.valueOf(-7003.01)));
    }

    @Test
    public void shouldReturnNullForNonExistingPlayer() {
        // GIVEN the populated database

        // WHEN asking the DAO for a player ID
        final BigDecimal playerId = underTest.getPlayerId("FAZEBOOX7", "wRoNg");

        // THEN a correct internal ID is returned
        assertThat(playerId, nullValue());
    }

    @Test
    public void shouldThrowTooManyPlayersMatchedToExternalIdExceptionWhenExternalIdMatchesManyPlayers() {
        // GIVEN the populated database

        // WHEN asking the DAO for a player ID
        try {
            underTest.getPlayerId("facebook", "twoplayerids");

            fail("Expected TooManyPlayerstwoplayeridsMatchedToExternalIdException");
        } catch (final TooManyPlayersMatchedToExternalIdException e) {
            assertThat(e.getProvider(), is("facebook"));
            assertThat(e.getExternalId(), is("twoplayerids"));
            assertThat(e.getPlayerIdsMatched(),
                    hasItems(BigDecimal.valueOf(-7004.01), BigDecimal.valueOf(-7005.01)));
        }
    }

    @Test
    public void shouldGetStatementDetailsForHugeTransactionLogId() {
        // GIVEN a transaction log record with the ID above the maximum integer
        dwTemplate.update("INSERT INTO GAME_VARIATION_TEMPLATE(GAME_VARIATION_TEMPLATE_ID,GAME_TYPE,NAME) VALUES(-778.01,'SLOTS','TEST')");
        dwTemplate.update("INSERT INTO TABLE_DEFINITION(TABLE_ID,GAME_VARIATION_TEMPLATE_ID) VALUES(-777.01,-778.01)");
        dwTemplate.update(
                "INSERT INTO TRANSACTION_LOG(ACCOUNT_ID,AMOUNT,TRANSACTION_TYPE,TRANSACTION_TS,REFERENCE, TABLE_ID, GAME_ID) " +
                        "VALUES(-7070.01,1000,'Test',?, '', -777.01, 0.00 )",
                new DateTime(2021, 7, 1, 0, 0, 0, 0).toDate());
        dwTemplate.update(
                "INSERT INTO TRANSACTION_LOG(ACCOUNT_ID,AMOUNT,TRANSACTION_TYPE,TRANSACTION_TS,REFERENCE, TABLE_ID, GAME_ID) " +
                        "VALUES(-7070.01,1000,'Test',?,'', -777.01, 0.00)",
                new DateTime(2021, 7, 2, 0, 0, 0, 0).toDate());

        // WHEN requesting the statement details
        final Dashboard dashboard = underTest.getStatementDetails(ACCOUNT_ID, "", 0, 20, new DateTime(2021, 1, 1, 0, 0, 0, 0).toDate(),
                new DateTime(2021, 12, 1, 0, 0, 0, 0).toDate(), new BigDecimal("-777.01"), "", "Test");

        // THEN the result is returned as expected
        Map<String, Object> dashboardMap = dashboard.getResults().get(0);
        assertThat(dashboardMap.get("Table").toString(), is("-777.01"));
        assertThat(dashboardMap.get("Date/Time").toString(), is("2021-07-02 00:00:00.0"));
    }

    private void createExtTxn(Double accountId, String internalTxnId, String message, String currencyCode, int amount, String status) {
        dwTemplate.update("INSERT INTO EXTERNAL_TRANSACTION(ACCOUNT_ID, INTERNAL_TRANSACTION_ID, MESSAGE, "
                + "MESSAGE_TS, CURRENCY_CODE, AMOUNT, CREDIT_CARD_NUMBER, CASHIER_NAME, "
                + "EXTERNAL_TRANSACTION_STATUS, AMOUNT_CHIPS, VERSION) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                BigDecimal.valueOf(accountId), internalTxnId, message, new DateTime().toDate(), currencyCode, amount, "CC1", "Any Cashier",
                status, 1000, 1);
    }

}
