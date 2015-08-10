package com.yazino.platform.player.persistence;

import com.yazino.platform.player.PasswordType;
import com.yazino.platform.player.YazinoLogin;
import org.junit.After;
import org.junit.Assert;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
public class JDBCYazinoLoginDaoIntegrationTest {
    private static final String SELECT_ALL = "SELECT * FROM YAZINO_LOGIN WHERE PLAYER_ID=?";
    private static final BigDecimal BASE_PLAYER_ID = BigDecimal.valueOf(-1263);
    private static final BigDecimal BASE_ACCOUNT_ID = BigDecimal.valueOf(-1263);

    @Autowired
    private YazinoLoginDao yazinoLoginDao;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbc;

    private static final AtomicInteger idIncrement = new AtomicInteger(0);

    private final Set<BigDecimal> accountsToDelete = new HashSet<BigDecimal>();
    private final Set<BigDecimal> playersToDelete = new HashSet<BigDecimal>();

    private BigDecimal playerId;

    @Before
    public void setUp() {
        playerId = insertTestLobbyUser();
    }

    private BigDecimal insertTestLobbyUser() {
        final BigDecimal increment = BigDecimal.valueOf(idIncrement.getAndIncrement());
        final BigDecimal playerId = BASE_PLAYER_ID.add(increment);
        final BigDecimal accountId = BASE_ACCOUNT_ID.add(increment);
        jdbc.update("INSERT INTO ACCOUNT (ACCOUNT_ID) VALUES (?)", accountId);
        accountsToDelete.add(accountId);
        jdbc.update("INSERT INTO PLAYER (PLAYER_ID, ACCOUNT_ID) VALUES (?,?)", playerId, accountId);
        playersToDelete.add(playerId);
        jdbc.update("INSERT INTO LOBBY_USER(PROVIDER_NAME,PLAYER_ID) values ('YAZINO',?)", playerId);
        return playerId;
    }

    @After
    public void deleteTestData() {
        for (BigDecimal playerId : playersToDelete) {
            jdbc.update("DELETE FROM YAZINO_LOGIN WHERE PLAYER_ID=?", playerId);
            jdbc.update("DELETE FROM LOBBY_USER WHERE PLAYER_ID=?", playerId);
            jdbc.update("DELETE FROM PLAYER WHERE PLAYER_ID=?", playerId);
        }
        for (BigDecimal accountId : accountsToDelete) {
            jdbc.update("DELETE FROM ACCOUNT WHERE ACCOUNT_ID=?", accountId);
        }
    }

    @Test
    @Transactional
    public void shouldCreateLogin() {
        final YazinoLogin yazinoLogin = getExpectedYazinoLogin();

        yazinoLoginDao.save(yazinoLogin);

        @SuppressWarnings({"unchecked"}) Map<String, Object> map = jdbc.queryForMap(SELECT_ALL, playerId);
        assertThat(BigDecimal.valueOf(((BigDecimal) map.get("PLAYER_ID")).longValue()),
                is(equalTo(yazinoLogin.getPlayerId())));
        assertThat(map.get("EMAIL_ADDRESS").toString(), is(equalTo(yazinoLogin.getEmail())));
        assertThat(map.get("PASSWORD_HASH").toString(), is(equalTo(yazinoLogin.getPasswordHash())));
        assertThat(PasswordType.valueOf(map.get("PASSWORD_TYPE").toString()), is(equalTo(yazinoLogin.getPasswordType())));
        assertThat((byte[]) map.get("SALT"), is(equalTo(yazinoLogin.getSalt())));
        assertThat((Integer) map.get("LOGIN_ATTEMPTS"), is(equalTo(yazinoLogin.getLoginAttempts())));
    }

    @Test
    @Transactional
    public void shouldUpdateLoginWithId() {
        final YazinoLogin yazinoLogin = getExpectedYazinoLogin();
        final YazinoLogin updatedUser = new YazinoLogin(playerId, "newEmail", "newPasswordHash",
                PasswordType.PBKDF2, "aNewSalt".getBytes(), 7);
        yazinoLoginDao.save(yazinoLogin);

        yazinoLoginDao.save(updatedUser);

        @SuppressWarnings({"unchecked"}) Map<String, Object> map = jdbc.queryForMap(SELECT_ALL, playerId);
        assertThat(BigDecimal.valueOf(((BigDecimal) map.get("PLAYER_ID")).longValue()),
                is(equalTo(updatedUser.getPlayerId())));
        assertThat(map.get("EMAIL_ADDRESS").toString(), is(equalTo(updatedUser.getEmail())));
        assertThat(map.get("PASSWORD_HASH").toString(), is(equalTo(updatedUser.getPasswordHash())));
        assertThat(PasswordType.valueOf(map.get("PASSWORD_TYPE").toString()),
                is(equalTo(updatedUser.getPasswordType())));
        assertThat((byte[]) map.get("SALT"), is(equalTo(updatedUser.getSalt())));
        assertThat((Integer) map.get("LOGIN_ATTEMPTS"), is(equalTo(updatedUser.getLoginAttempts())));
    }

    @Test
    @Transactional
    public void shouldIncrementLoginAttemptsForAUser() {
        final YazinoLogin user = getExpectedYazinoLogin();
        yazinoLoginDao.save(user);

        yazinoLoginDao.incrementLoginAttempts(user.getEmail());

        @SuppressWarnings({"unchecked"}) Map<String, Object> map = jdbc.queryForMap(SELECT_ALL, playerId);
        assertThat(BigDecimal.valueOf(((BigDecimal) map.get("PLAYER_ID")).longValue()),
                is(equalTo(user.getPlayerId())));
        assertThat(map.get("EMAIL_ADDRESS").toString(), is(equalTo(user.getEmail())));
        assertThat(map.get("PASSWORD_HASH").toString(), is(equalTo(user.getPasswordHash())));
        assertThat(PasswordType.valueOf(map.get("PASSWORD_TYPE").toString()),
                is(equalTo(user.getPasswordType())));
        assertThat((Integer) map.get("LOGIN_ATTEMPTS"), is(equalTo(user.getLoginAttempts() + 1)));
    }

    @Test
    @Transactional
    public void shouldResetLoginAttemptsForAUser() {
        final YazinoLogin user = getExpectedYazinoLogin();
        yazinoLoginDao.save(user);

        yazinoLoginDao.resetLoginAttempts(user.getEmail());

        @SuppressWarnings({"unchecked"}) Map<String, Object> map = jdbc.queryForMap(SELECT_ALL, playerId);
        assertThat(BigDecimal.valueOf(((BigDecimal) map.get("PLAYER_ID")).longValue()),
                is(equalTo(user.getPlayerId())));
        assertThat(map.get("EMAIL_ADDRESS").toString(), is(equalTo(user.getEmail())));
        assertThat(map.get("PASSWORD_HASH").toString(), is(equalTo(user.getPasswordHash())));
        assertThat(PasswordType.valueOf(map.get("PASSWORD_TYPE").toString()),
                is(equalTo(user.getPasswordType())));
        assertThat((Integer) map.get("LOGIN_ATTEMPTS"), is(equalTo(0)));
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional
    public void shouldNotUpdateUserWithInvalidId() {
        final YazinoLogin user = new YazinoLogin(BigDecimal.valueOf(4543589347543743933L),
                "email", "passwordHash", PasswordType.MD5, null, 0);
        yazinoLoginDao.save(user);
    }

    @Test
    @Transactional
    public void shouldCheckIfLoginExistsUsingVerifiedEmail() {
        final YazinoLogin user = getExpectedYazinoLogin();
        yazinoLoginDao.save(user);
        Assert.assertFalse(yazinoLoginDao.existsWithEmailAddress("test3@example.com"));
    }

    @Test
    @Transactional
    public void shouldCheckIfLoginExistsWithEmailAddress() {
        final YazinoLogin user = getExpectedYazinoLogin();
        yazinoLoginDao.save(user);
        Assert.assertTrue(yazinoLoginDao.existsWithEmailAddress("email"));
    }

    @Test
    @Transactional
    public void shouldFindByEmailAddress() {
        final YazinoLogin user = getExpectedYazinoLogin();
        yazinoLoginDao.save(user);
        Assert.assertEquals(user, yazinoLoginDao.findByEmailAddress("email"));
    }

    @Test
    @Transactional
    public void shouldFindNullForInvalidEmailAddress() {
        Assert.assertNull(yazinoLoginDao.findByEmailAddress("test3@example.com"));
    }

    @Test
    @Transactional
    public void shouldGetYazinoLoginForGivenPlayerId() {
        final YazinoLogin expectedYazinoLogin = getExpectedYazinoLogin();
        yazinoLoginDao.save(expectedYazinoLogin);
        YazinoLogin actualYazinoLogin = yazinoLoginDao.findByPlayerId(playerId);
        Assert.assertEquals(expectedYazinoLogin, actualYazinoLogin);
    }

    @Test(expected = NullPointerException.class)
    @Transactional
    public void findByPLayerIdThrowsExceptionWhenPlayerIdNull() {
        yazinoLoginDao.findByPlayerId(null);
    }

    @Test
    @Transactional
    public void findRegisteredEmailAddresses_shouldReturnMatchingAddresses_noMatches() {
        Map<String, BigDecimal> matches = yazinoLoginDao.findRegisteredEmailAddresses("no such email");
        assertTrue(matches.isEmpty());
    }

    @Test
    @Transactional
    public void findRegisteredEmailAddresses_shouldReturnMatchingAddresses_oneMatch() {
        String registeredEmail = "email1@example.com";
        YazinoLogin login1 = YazinoLogin.withPlayerId(playerId)
                .withEmail(registeredEmail).withPasswordHash("aPassword").asLogin();
        yazinoLoginDao.save(login1);

        Map<String, BigDecimal> expected = new HashMap<String, BigDecimal>();
        expected.put(registeredEmail, playerId.setScale(2));

        Map<String, BigDecimal> actual = yazinoLoginDao.findRegisteredEmailAddresses(registeredEmail, "no such email 1", "no such email 2");
        assertEquals(expected, actual);
    }

    @Test
    @Transactional
    public void findRegisteredEmailAddresses_shouldReturnMatchingAddresses_multipleMatch() {
        String registeredEmail1 = "email1@example.com";
        BigDecimal playerId = insertTestLobbyUser();
        YazinoLogin login1 = YazinoLogin.withPlayerId(playerId).withEmail(registeredEmail1)
                .withPasswordHash("aPassword").asLogin();
        yazinoLoginDao.save(login1);

        String registeredEmail2 = "email2@example.com";
        BigDecimal playerProfileId2 = insertTestLobbyUser();
        YazinoLogin login2 = YazinoLogin.withPlayerId(playerProfileId2).withEmail(registeredEmail2)
                .withPasswordHash("aPassword").asLogin();
        yazinoLoginDao.save(login2);

        String unregisteredEmail = "email3@example.com";

        Map<String, BigDecimal> expected = new HashMap<String, BigDecimal>();
        expected.put(registeredEmail1, playerId.setScale(2));
        expected.put(registeredEmail2, playerProfileId2.setScale(2));

        Map<String, BigDecimal> actual = yazinoLoginDao.findRegisteredEmailAddresses(registeredEmail1, registeredEmail2, unregisteredEmail);
        assertEquals(expected, actual);
    }

    @Test
    @Transactional
    public void findRegisteredEmailAddresses_shouldIgnoreNonMatchingAddresses() {
        String registeredEmail1 = "email1@example.com";
        YazinoLogin login1 = YazinoLogin.withPlayerId(playerId).withEmail(registeredEmail1)
                .withPasswordHash("aPassword").asLogin();
        yazinoLoginDao.save(login1);

        String unregisteredEmail = "email3@example.com";

        Map<String, BigDecimal> actual = yazinoLoginDao.findRegisteredEmailAddresses(unregisteredEmail);
        assertTrue(actual.isEmpty());
    }

    @Test
    @Transactional
    public void deleteByPlayerIdShouldDeleteMatchingRecord() {
        yazinoLoginDao.save(YazinoLogin.withPlayerId(playerId).withEmail("player@example.com").withPasswordHash("password-hash").asLogin());
        assertNotNull(yazinoLoginDao.findByPlayerId(playerId));

        yazinoLoginDao.deleteByPlayerId(playerId);

        assertNull(yazinoLoginDao.findByPlayerId(playerId));
    }

    private YazinoLogin getExpectedYazinoLogin() {
        return YazinoLogin.withPlayerId(playerId)
                .withPasswordHash("passwordHash")
                .withPasswordType(PasswordType.MD5)
                .withLoginAttempts(3)
                .withEmail("email")
                .withSalt(new byte[]{1, 2, 3, 4, 5, 6, 7, 8})
                .asLogin();
    }

}
