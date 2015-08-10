package com.yazino.platform.player.persistence;

import com.google.common.base.Optional;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.player.*;
import com.yazino.platform.util.BigDecimals;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.player.PlayerProfileStatus.*;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
public class JDBCPlayerProfileDaoIntegrationTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(-10004);
    private static final BigDecimal PLAYER_ID_2 = BigDecimal.valueOf(-10003);
    private static final String PLAYER_PROFILE_SELECT = "SELECT * FROM LOBBY_USER WHERE PLAYER_ID=?";
    private static final String VERIFICATION_IDENTIFIER = "a verification identifier";
    private static final DateTime REGISTRATION_TIME = new DateTime(2012, 3, 20, 10, 9, 30, 0);

    @Autowired
    private PlayerProfileDao underTest;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @Transactional
    public void shouldCreatePlayerProfileWithAPlayerId() {
        final PlayerProfile profile = aPlayerProfile();

        underTest.save(profile);

        final Map<String, Object> map = jdbc.queryForMap(PLAYER_PROFILE_SELECT, PLAYER_ID);
        checkPlayerProfile(profile, profile, map);
    }

    @Test
    @Transactional
    public void aNewPlayerProfileWithoutAnRpxProviderShouldHaveItSetFromTheProviderName() {
        final PlayerProfile profile = aPlayerProfile();
        profile.setRpxProvider(null);

        underTest.save(profile);

        final Map<String, Object> map = jdbc.queryForMap(PLAYER_PROFILE_SELECT, PLAYER_ID);
        assertThat((String) map.get("RPX_PROVIDER"), is(equalTo(profile.getProviderName())));
    }

    @Test
    @Transactional
    public void aNewPlayerProfileWithOptInShouldHaveItSet() {
        final PlayerProfile profile = aPlayerProfile();
        profile.setOptIn(true);

        underTest.save(profile);

        final Map<String, Object> map = jdbc.queryForMap(PLAYER_PROFILE_SELECT, PLAYER_ID);
        assertThat((Boolean) map.get("EMAIL_OPT_IN"), is(equalTo(TRUE)));
    }

    @Test
    @Transactional
    public void aNewPlayerProfileWithNullOptInShouldNotHaveItSet() {
        final PlayerProfile profile = aPlayerProfile();
        profile.setOptIn(null);

        underTest.save(profile);

        final Map<String, Object> map = jdbc.queryForMap(PLAYER_PROFILE_SELECT, PLAYER_ID);
        assertThat(map.get("EMAIL_OPT_IN"), is(nullValue()));
    }

    @Test
    @Transactional
    public void shouldUpdateExistingPlayerProfile() {
        final PlayerProfile originalPlayerProfile = aPlayerProfile();
        underTest.save(originalPlayerProfile);
        final PlayerProfile updatedProfile = PlayerProfile.copy(originalPlayerProfile)
                .withExternalId("updatedExternalId")
                .withEmailAddress("updatedEmail")
                .withDisplayName("updatedDisplayName")
                .withRealName("updatedRealName")
                .withGender(Gender.FEMALE)
                .withDateOfBirth(new DateTime(1961, 10, 20, 0, 0))
                .withCountry("JP")
                .withFirstName("updatedFirstName")
                .withLastName("updatedLastName")
                .withReferralIdentifier("updatedReferralId")
                .withProviderName("updatedProviderName")
                .withSyncProfile(false)
                .withGuestStatus(GuestStatus.CONVERTED)
                .withPartnerId(YAZINO)
                .asProfile();
        underTest.save(updatedProfile);

        final Map<String, Object> map = jdbc.queryForMap(PLAYER_PROFILE_SELECT, PLAYER_ID);
        assertPlayerProfile(updatedProfile, map);
    }

    @Test
    @Transactional
    public void whenUpdatingAPlayerProfileTheRegistrationDateShouldNotBeChanged() {
        final PlayerProfile originalProfile = aPlayerProfile();
        underTest.save(originalProfile);
        final PlayerProfile updatedProfile = PlayerProfile.copy(originalProfile)
                .withRegistrationTime(REGISTRATION_TIME.plusMonths(1))
                .asProfile();
        underTest.save(updatedProfile);

        final Map<String, Object> map = jdbc.queryForMap(PLAYER_PROFILE_SELECT, PLAYER_ID);
        assertPlayerProfile(originalProfile, map);
    }


    @Test
    @Transactional
    public void whenSavingAPlayerProfileTheOptInShouldBeSet() {
        final PlayerProfile originalProfile = aPlayerProfile();
        originalProfile.setOptIn(true);

        underTest.save(originalProfile);

        final Map<String, Object> map = jdbc.queryForMap(PLAYER_PROFILE_SELECT, PLAYER_ID);
        assertTrue(map.containsKey("email_opt_in"));
        assertEquals(map.get("email_opt_in"), true);
    }

    @Test
    @Transactional
    public void whenSavingAPlayerProfileTheGuestStatusShouldBeSet() {
        final PlayerProfile originalProfile = aPlayerProfile();
        originalProfile.setGuestStatus(GuestStatus.GUEST);

        underTest.save(originalProfile);

        final Map<String, Object> map = jdbc.queryForMap(PLAYER_PROFILE_SELECT, PLAYER_ID);
        assertTrue(map.containsKey("guest_status"));
        assertEquals(map.get("guest_status"), GuestStatus.GUEST.getId());
    }

    @Test
    @Transactional
    public void updatingAPlayerProfileWithANonNullRpxProviderShouldNotChangeIt() {
        final PlayerProfile originalPlayerProfileProfile = aPlayerProfile();
        underTest.save(originalPlayerProfileProfile);

        final PlayerProfile updatedProfile = PlayerProfile.copy(originalPlayerProfileProfile)
                .withProviderName("updatedProviderName")
                .asProfile();
        underTest.save(updatedProfile);

        final Map<String, Object> map = jdbc.queryForMap(PLAYER_PROFILE_SELECT, PLAYER_ID);
        assertThat((String) map.get("RPX_PROVIDER"), is(equalTo(updatedProfile.getRpxProvider())));
    }

    @Test
    @Transactional
    public void updatingAPlayerProfileWithANullRpxProviderShouldSetItToTheProviderName() {
        final PlayerProfile originalPlayerProfile = aPlayerProfile();
        originalPlayerProfile.setRpxProvider(null);
        underTest.save(originalPlayerProfile);

        final PlayerProfile updatedProfile = PlayerProfile.copy(originalPlayerProfile)
                .withRpxProvider(null)
                .withProviderName("updatedProviderName")
                .asProfile();
        underTest.save(updatedProfile);

        final Map<String, Object> map = jdbc.queryForMap(PLAYER_PROFILE_SELECT, PLAYER_ID);
        assertThat((String) map.get("RPX_PROVIDER"), is(equalTo(updatedProfile.getProviderName())));
    }

    @Test
    @Transactional
    public void queryingByProviderIdAndExternalIdShouldReturnTheMatchingRecord() {
        final PlayerProfile profile = aSimplePlayerProfile();

        underTest.save(profile);

        assertThat(underTest.findByProviderNameAndExternalId("providerName", "externalId"), is(equalTo(profile)));
    }

    @Test
    @Transactional
    public void queryingByProviderAndExternalIdShouldReturnTheMecordWithTheHighestPlayerIdIfMultipleMatchesExist()
            throws InterruptedException {
        final PlayerProfile profile1 = aSimplePlayerProfile();
        final PlayerProfile profile2 = aSimplePlayerProfile();
        profile2.setPlayerId(PLAYER_ID_2);

        underTest.save(profile1);
        underTest.save(profile2);

        assertThat(underTest.findByProviderNameAndExternalId("providerName", "externalId"), is(equalTo(profile2)));
    }

    @Test
    @Transactional
    public void queryingANonExistentProviderAndExternalIdShouldReturnNull() {
        assertThat(underTest.findByProviderNameAndExternalId("wrongProviderName", "externalId"),
                Matchers.is(Matchers.nullValue()));
    }

    @Test
    @Transactional
    public void playersCanBeFoundByPlayerId() {
        final PlayerProfile profile = aPlayerProfile();
        underTest.save(profile);

        final PlayerProfile profileForPlayerId = underTest.findByPlayerId(PLAYER_ID);

        assertThat(profileForPlayerId, is(equalTo(profile)));
    }

    @Test
    @Transactional
    public void aPlayerWithNoRegistrationTimeCanBePersistedCorrectly() {
        final PlayerProfile profile = aPlayerProfile();
        profile.setRegistrationTime(null);

        underTest.save(profile);

        final PlayerProfile profileForPlayerId = underTest.findByPlayerId(PLAYER_ID);
        assertThat(profileForPlayerId, is(equalTo(profile)));
    }

    @Test
    @Transactional
    public void findingANonExistentPlayerProfileByPlayerIdReturnsNull() {
        final PlayerProfile profileForPlayerId = underTest.findByPlayerId(BigDecimal.valueOf(-10345));

        assertThat(profileForPlayerId, is(nullValue()));
    }

    @Test
    @Transactional
    public void shouldFindByProviderAndExternalIdWithBlockedStatus() {
        final PlayerProfile profile = aSimplePlayerProfile();
        underTest.save(profile);

        underTest.updateStatus(profile.getPlayerId(), BLOCKED, "aUser", "aReason");

        profile.setStatus(BLOCKED);
        assertNull(underTest.findByProviderNameAndExternalId("wrongProviderName", "externalId"));
        assertNull(underTest.findByProviderNameAndExternalId("providerName", "WrongExternalId"));
        assertEquals(profile, underTest.findByProviderNameAndExternalId("providerName", "externalId"));
    }

    @Test
    @Transactional
    public void shouldGetByPlayerId() {
        final PlayerProfile expected = aSimplePlayerProfile();

        underTest.save(expected);

        final PlayerProfile actual = underTest.findByPlayerId(expected.getPlayerId());

        assertThat(expected.getPlayerId(), is(comparesEqualTo(actual.getPlayerId())));
        assertEquals(expected.getEmailAddress(), actual.getEmailAddress());
        assertEquals(expected.getRealName(), actual.getRealName());
        assertEquals(expected.getDisplayName(), actual.getDisplayName());
        assertEquals(expected.getCountry(), actual.getCountry());
        assertEquals(expected.getGender(), actual.getGender());
        assertEquals(expected.getGuestStatus(), actual.getGuestStatus());
        assertEquals(expected.getProviderName(), actual.getProviderName());
        assertEquals(expected.getExternalId(), actual.getExternalId());
    }

    @Test
    @Transactional
    public void updatingToCustomerRoleShouldEnsureInsiderIsSetToFalse() {
        save(aSimplePlayerProfile());

        underTest.updateRole(PLAYER_ID, PlayerProfileRole.CUSTOMER);

        final Optional<PlayerSummary> summary = underTest.findSummaryById(PLAYER_ID);
        assertThat(summary.get().getRole(), is(equalTo(PlayerProfileRole.CUSTOMER)));
    }

    @Test
    @Transactional
    public void updatingToInsiderRoleShouldEnsureInsiderIsSetToTrue() {
        save(aSimplePlayerProfile());

        underTest.updateRole(PLAYER_ID, PlayerProfileRole.INSIDER);

        final Optional<PlayerSummary> summary = underTest.findSummaryById(PLAYER_ID);
        assertThat(summary.get().getRole(), is(equalTo(PlayerProfileRole.INSIDER)));
    }

    @Test
    @Transactional
    public void shouldGetByPlayerIdWithBlockedStatus() {
        final PlayerProfile expected = aSimplePlayerProfile();
        underTest.save(expected);

        underTest.updateStatus(expected.getPlayerId(), BLOCKED, "aUser", "aReason");

        expected.setStatus(BLOCKED);
        final PlayerProfile actual = underTest.findByPlayerId(expected.getPlayerId());
        assertEquals(expected, actual);
    }

    @Test
    @Transactional
    public void updatingTheStatusOfAPlayerShouldWriteAnAuditRecord() {
        final PlayerProfile expected = aSimplePlayerProfile();
        underTest.save(expected);

        underTest.updateStatus(expected.getPlayerId(), BLOCKED, "aUser", "aReason");

        final List<PlayerProfileAudit> auditRecords = underTest.findAuditRecordsFor(PLAYER_ID);
        assertThat(auditRecords, is(not(empty())));
        final PlayerProfileAudit audit = auditRecords.get(0);
        assertThat(audit.getPlayerId(), is(equalTo(PLAYER_ID)));
        assertThat(audit.getOldStatus(), is(equalTo(PlayerProfileStatus.ACTIVE)));
        assertThat(audit.getNewStatus(), is(equalTo(PlayerProfileStatus.BLOCKED)));
        assertThat(audit.getChangedBy(), is(equalTo("aUser")));
        assertThat(audit.getReason(), is(equalTo("aReason")));
    }

    @Test
    @Transactional
    public void queryingAuditRecordsForAPlayerWithNoneReturnsAnEmptyList() {
        final List<PlayerProfileAudit> auditRecords = underTest.findAuditRecordsFor(PLAYER_ID);

        assertThat(auditRecords, is(not(nullValue())));
        assertThat(auditRecords, is(empty()));
    }

    @Test
    @Transactional
    public void queryingAuditRecordsForAPlayerReturnsATheRecordsSortedByDate() {
        jdbc.update("INSERT INTO PLAYER_PROFILE_STATUS_AUDIT (PLAYER_ID,OLD_STATUS,NEW_STATUS,CHANGED_BY,REASON,CHANGED_TS) VALUES "
                + "(-7000,'A','B','test1','reason1','2013-04-01 10:00:00'),"
                + "(" + PLAYER_ID + ",'A','B','test2','reason2','2013-04-01 09:00:00'),"
                + "(" + PLAYER_ID + ",'B','C','test2','reason3','2013-04-01 11:00:00'),"
                + "(" + PLAYER_ID + ",'C','A','test3','reason4','2013-04-03 10:00:00')");

        final List<PlayerProfileAudit> auditRecords = underTest.findAuditRecordsFor(PLAYER_ID);

        assertThat(auditRecords.size(), is(equalTo(3)));
        assertThat(auditRecords, is(equalTo(asList(
                new PlayerProfileAudit(PLAYER_ID, ACTIVE, BLOCKED, "test2", "reason2", new DateTime(2013, 4, 1, 9, 0, 0, 0)),
                new PlayerProfileAudit(PLAYER_ID, BLOCKED, CLOSED, "test2", "reason3", new DateTime(2013, 4, 1, 11, 0, 0, 0)),
                new PlayerProfileAudit(PLAYER_ID, CLOSED, ACTIVE, "test3", "reason4", new DateTime(2013, 4, 3, 10, 0, 0, 0))))));
    }

    @Test
    @Transactional
    public void findPlayerProfileIdsByExternalIdAndProvider_returnsEmptySetWhenExternalIdSetIsNull() {
        final Set<BigDecimal> actualProfileIds =
                underTest.findPlayerIdsByProviderNameAndExternalIds(null, "not google");
        assertTrue(actualProfileIds.isEmpty());
    }

    @Test
    @Transactional
    public void findPlayerProfileIdsByExternalIdAndProvider_returnsEmptySetWhenExternalIdSetIsEmpty() {
        final Set<String> externalIds = Collections.emptySet();
        final Set<BigDecimal> actualProfileIds =
                underTest.findPlayerIdsByProviderNameAndExternalIds(externalIds, "not google");
        assertTrue(actualProfileIds.isEmpty());
    }

    @Test
    @Transactional
    public void findPlayerProfileIdsByExternalIdAndProvider_returnsEmptySetWithNoMatchesOnProvider() {
        final String externalId = "8980989";
        final String provider = "google";
        final PlayerProfile googleProfile =
                new PlayerProfile(PLAYER_ID, "email", "displayName", "realName", Gender.MALE, "UK",
                        null, null, null, "referralId", provider, "rpx", externalId, true);
        googleProfile.setGuestStatus(GuestStatus.NON_GUEST);
        underTest.save(googleProfile);
        final Set<String> externalIds = new HashSet<>(asList(externalId, "another"));
        final Set<BigDecimal> actualProfileIds =
                underTest.findPlayerIdsByProviderNameAndExternalIds(externalIds, "not google");
        assertTrue(actualProfileIds.isEmpty());
    }

    @Test
    @Transactional
    public void findPlayerProfileIdsByExternalIdAndProvider_returnsEmptySetWithNoMatchesOnExternalIds() {
        final String externalId = "8980989";
        final String provider = "google";
        final PlayerProfile googleProfile = new PlayerProfile(PLAYER_ID, "email", "displayName", "realName", Gender.MALE, "UK",
                null, null, null, "referralId", provider, "rpx", externalId, true);
        googleProfile.setGuestStatus(GuestStatus.NON_GUEST);
        underTest.save(googleProfile);
        final Set<String> externalIds = new HashSet<>(asList("another extern id"));
        final Set<BigDecimal> actualProfileIds =
                underTest.findPlayerIdsByProviderNameAndExternalIds(externalIds, provider);
        assertTrue(actualProfileIds.isEmpty());
    }

    @Test
    @Transactional
    public void findPlayerProfileIdsByExternalIdAndProvider_returnsPlayerProfileIds() {
        final BigDecimal[] playerIds = {new BigDecimal("-10.00"), new BigDecimal("-20.01"), BigDecimal.valueOf(-30),
                BigDecimal.valueOf(-40), BigDecimal.valueOf(-50)};
        final String[] externalIds = {"e1", "e2", "e3", "e4", "e5"};
        final String[] providers = {"p1", "p2", "p1", "p1", "e2"};
        for (int i = 0; i < externalIds.length; i++) {
            final PlayerProfile profile = new PlayerProfile("email", "displayName", "realName", Gender.MALE, "UK",
                    null, null, null, "referralId", providers[i], "rpx", externalIds[i], true);
            profile.setPlayerId(playerIds[i]);
            profile.setGuestStatus(GuestStatus.NON_GUEST);
            underTest.save(profile);
        }
        final List<BigDecimal> expectedPlayerIds = asList(playerIds[0].setScale(0), playerIds[2], playerIds[3]);
        final Set<String> p1ExternalIds = new HashSet<>(asList(externalIds[0], externalIds[2], externalIds[3]));
        final Set<BigDecimal> actualPlayerIds =
                underTest.findPlayerIdsByProviderNameAndExternalIds(p1ExternalIds, "p1");
        assertEquals(expectedPlayerIds.size(), actualPlayerIds.size());
        assertTrue(actualPlayerIds.containsAll(expectedPlayerIds));
    }

    @Test
    @Transactional
    public void findByEmailAddress_shouldReturnNullWhenEmailAddressIsNull() {
        assertNull(underTest.findByEmailAddress(null));
    }

    @Test
    @Transactional
    public void findByEmailAddress_shouldReturnNullWhenEmailAddressUnkown() {
        assertNull(underTest.findByEmailAddress(UUID.randomUUID().toString()));
    }

    @Test
    @Transactional
    public void findByEmailAddress_shouldReturnPlayerProfile() {
        final PlayerProfile profile = aPlayerProfile();
        underTest.save(profile);
        final PlayerProfile actual = underTest.findByEmailAddress("email");
        assertEquals(profile, actual);
    }

    @Test
    @Transactional
    public void findByEmailAddress_shouldReturnPlayerProfileWithBlocked() {
        final PlayerProfile profile = aPlayerProfile();
        underTest.save(profile);

        underTest.updateStatus(profile.getPlayerId(), BLOCKED, "aUser", "aReason");

        profile.setStatus(BLOCKED);
        final PlayerProfile actual = underTest.findByEmailAddress("email");
        assertEquals(profile, actual);
    }

    @Test
    @Transactional
    public void shouldUpdateBlockedStatus() {
        final PlayerProfile profile =
                new PlayerProfile("email", "displayName", "realName", Gender.MALE, "UK",
                        "firstName", "lastName", new DateTime(1970, 3, 20, 0, 0, 0, 0), "referralId",
                        "Google", "rpx", null, true);
        profile.setPlayerId(BigDecimal.valueOf(-10003));
        profile.setGuestStatus(GuestStatus.NON_GUEST);
        underTest.save(profile);

        assertThat(profile.getStatus(), is(equalTo(ACTIVE)));

        underTest.updateStatus(profile.getPlayerId(), BLOCKED, "aUser", "aReason");

        final PlayerProfileStatus actualStatus = PlayerProfileStatus.forId(
                jdbc.queryForObject("SELECT STATUS FROM LOBBY_USER WHERE PLAYER_ID=?", String.class, profile.getPlayerId()));
        assertEquals(PlayerProfileStatus.BLOCKED, actualStatus);
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional
    public void shouldNotUpdateBlockedStatusForUnknownPlayerProfile() {
        final int maxId = jdbc.queryForInt("SELECT max(PLAYER_ID) FROM LOBBY_USER");

        underTest.updateStatus(new BigDecimal(maxId + 1), BLOCKED, "aUser", "aReason");
    }

    @Test
    @Transactional
    public void countShouldReturnTheNumberOfRowsInTheLobbyPlayerProfileTable() {
        final int initialCount = jdbc.queryForInt("SELECT COUNT(*) FROM LOBBY_USER");
        underTest.save(aSimplePlayerProfile());

        final int count = underTest.count();

        Assert.assertThat(count, Matchers.is(Matchers.equalTo(initialCount + 1)));
    }

    @Test
    @Transactional
    public void findRegisteredEmailAddresses_shouldReturnMatchingAddresses_noMatches() {
        Map<String, BigDecimal> matches = underTest.findRegisteredEmailAddresses("no such email");
        assertTrue(matches.isEmpty());
    }

    @Test
    @Transactional
    public void findRegisteredEmailAddresses_shouldReturnMatchingAddresses_oneMatch() {
        String registeredEmail = "email1@example.com";
        PlayerProfile profile1 = aPlayerProfile();
        profile1.setPlayerId(BigDecimal.valueOf(-1001));
        profile1.setEmailAddress(registeredEmail);
        underTest.save(profile1);

        Map<String, BigDecimal> expected = new HashMap<>();
        expected.put(registeredEmail, BigDecimals.strip(profile1.getPlayerId()));

        Map<String, BigDecimal> actual = underTest.findRegisteredEmailAddresses(registeredEmail, "no such email 1", "no such email 2");

        assertEquals(expected, actual);
    }

    @Test
    @Transactional
    public void findRegisteredEmailAddresses_shouldReturnMatchingAddresses_multipleMatch() {
        String registeredEmail1 = "email1@example.com";
        PlayerProfile profile1 = aPlayerProfile();
        profile1.setPlayerId(BigDecimal.valueOf(-1001));
        profile1.setEmailAddress(registeredEmail1);
        underTest.save(profile1);

        String registeredEmail2 = "email2@example.com";
        PlayerProfile profile2 = aPlayerProfile();
        profile2.setPlayerId(BigDecimal.valueOf(-1002));
        profile2.setEmailAddress(registeredEmail2);
        underTest.save(profile2);

        String unregisteredEmail = "email3@example.com";

        Map<String, BigDecimal> expected = new HashMap<>();
        expected.put(registeredEmail1, BigDecimals.strip(profile1.getPlayerId()));
        expected.put(registeredEmail2, BigDecimals.strip(profile2.getPlayerId()));

        Map<String, BigDecimal> actual = underTest.findRegisteredEmailAddresses(registeredEmail1, registeredEmail2, unregisteredEmail);

        assertEquals(expected, actual);
    }

    @Test
    @Transactional
    public void findRegisteredProfilesByProviderNameAndExternalIds_one_match() {
        final PlayerProfile profile1 = aPlayerProfile();
        profile1.setExternalId("ext1");
        profile1.setProviderName("facebook");
        profile1.setPlayerId(BigDecimal.valueOf(-1001));
        underTest.save(profile1);
        final PlayerProfile profile2 = aPlayerProfile();
        profile2.setExternalId("ext2");
        profile2.setPlayerId(BigDecimal.valueOf(-1002));
        profile2.setProviderName("facebook");
        underTest.save(profile2);

        Map<String, BigDecimal> expected = new HashMap<>();
        expected.put("ext1", BigDecimals.strip(profile1.getPlayerId()));

        Map<String, BigDecimal> actual = underTest.findRegisteredExternalIds("facebook", "ext1");
        assertEquals(expected, actual);
    }

    @Test
    @Transactional
    public void findRegisteredProfilesByProviderNameAndExternalIds_multiple_matches() {
        final PlayerProfile profile1 = aPlayerProfileWith(-1001, "ext1", "facebook");
        underTest.save(profile1);
        final PlayerProfile profile2 = aPlayerProfileWith(-1002, "ext2", "facebook");
        underTest.save(profile2);
        underTest.save(aPlayerProfileWith(-1003, "ext3", "facebook"));
        underTest.save(aPlayerProfileWith(-1004, "ext4", "email"));

        Map<String, BigDecimal> expected = new HashMap<>();
        expected.put("ext1", BigDecimals.strip(profile1.getPlayerId()));
        expected.put("ext2", BigDecimals.strip(profile2.getPlayerId()));

        Map<String, BigDecimal> actual = underTest.findRegisteredExternalIds("facebook", "ext1", "ext2", "ext5");
        assertEquals(expected, actual);
    }

    @Test
    @Transactional
    public void searchingPlayersByEmailAddressFindsAllPlayersWithAMatchingEmail() throws InterruptedException {
        save(aProfileWithEmail(-1001, "fpbyea@yazino.com"));
        save(aProfileWithEmail(-1002, "fpbyeas@example.com"));
        save(aProfileWithEmail(-1003, "fpbyea@yazi.no"));

        final PagedData<PlayerSearchResult> results = underTest.searchByEmailAddress("fpbyea@%", 0, 20);

        assertThat(results.getSize(), is(equalTo(2)));
        assertThat(results.getStartPosition(), is(equalTo(0)));
        assertThat(results.getTotalSize(), is(equalTo(2)));
        assertThat(results, contains(searchResultFor(aProfileWithEmail(-1001, "fpbyea@yazino.com")),
                searchResultFor(aProfileWithEmail(-1003, "fpbyea@yazi.no"))));
    }

    @Test
    @Transactional
    public void searchingPlayersByEmailAddressReturnsAnEmptyResultWhenNoneMatch() throws InterruptedException {
        save(aProfileWithEmail(-1001, "fpbyea@yazino.com"));
        save(aProfileWithEmail(-1002, "fpbyeas@example.com"));
        save(aProfileWithEmail(-1003, "fpbyea@yazi.no"));

        final PagedData<PlayerSearchResult> results = underTest.searchByEmailAddress("noMatch", 0, 20);

        assertThat(results, is(equalTo(PagedData.<PlayerSearchResult>empty())));
    }

    @Test
    @Transactional
    public void searchingPlayersByEmailAddressFindsTheMatchingMiddlePage() throws InterruptedException {
        save(aProfileWithEmail(-1001, "xtest@1"), aProfileWithEmail(-1002, "xtest@2"), aProfileWithEmail(-1003, "xtest@3"),
                aProfileWithEmail(-1004, "xtest@4"), aProfileWithEmail(-1005, "xtest@5"));

        final PagedData<PlayerSearchResult> results = underTest.searchByEmailAddress("xtest@%", 1, 2);

        assertThat(results.getSize(), is(equalTo(2)));
        assertThat(results.getStartPosition(), is(equalTo(2)));
        assertThat(results.getTotalSize(), is(equalTo(5)));
        assertThat(results, contains(searchResultFor(aProfileWithEmail(-1003, "xtest@3")),
                searchResultFor(aProfileWithEmail(-1004, "xtest@4"))));
    }

    @Test
    @Transactional
    public void searchingPlayersByEmailAddressFindsTheMatchingEndPage() throws InterruptedException {
        save(aProfileWithEmail(-1001, "xtest@1"), aProfileWithEmail(-1002, "xtest@2"), aProfileWithEmail(-1003, "xtest@3"),
                aProfileWithEmail(-1004, "xtest@4"), aProfileWithEmail(-1005, "xtest@5"));

        final PagedData<PlayerSearchResult> results = underTest.searchByEmailAddress("xtest@%", 2, 2);

        assertThat(results.getSize(), is(equalTo(1)));
        assertThat(results.getStartPosition(), is(equalTo(4)));
        assertThat(results.getTotalSize(), is(equalTo(5)));
        assertThat(results, contains(searchResultFor(aProfileWithEmail(-1005, "xtest@5"))));
    }

    @Test
    @Transactional
    public void searchingPlayersByNamesReturnsAnEmptyResultWhenNoneMatch() throws InterruptedException {
        save(aPlayerProfileWithName(-1001, "aNameOfSorts", "anotherName"),
                aPlayerProfileWithName(-1002, "yetAnotherName", "aNameOfSorts"),
                aPlayerProfileWithName(-1003, "againAnotherName", "andAnother"));

        final PagedData<PlayerSearchResult> results = underTest.searchByRealOrDisplayName("noMatch", 0, 20);

        assertThat(results, is(equalTo(PagedData.<PlayerSearchResult>empty())));
    }

    @Test
    @Transactional
    public void searchingPlayersByNamesFindsAllPlayersWithAMatchingRealOrDisplayName() throws InterruptedException {
        save(aPlayerProfileWithName(-1001, "aNameOfSorts", "anotherName"),
                aPlayerProfileWithName(-1002, "yetAnotherName", "aNameOfSorts"),
                aPlayerProfileWithName(-1003, "againAnotherName", "andAnother"));

        final PagedData<PlayerSearchResult> results = underTest.searchByRealOrDisplayName("aNameOfSorts", 0, 20);

        assertThat(results.getSize(), is(equalTo(2)));
        assertThat(results.getStartPosition(), is(equalTo(0)));
        assertThat(results.getTotalSize(), is(equalTo(2)));
        assertThat(results, contains(searchResultFor(aPlayerProfileWithName(-1001, "aNameOfSorts", "anotherName")),
                searchResultFor(aPlayerProfileWithName(-1002, "yetAnotherName", "aNameOfSorts"))));
    }

    @Test
    @Transactional
    public void searchingPlayersByNamesFindsTheMatchingMiddlePage() throws InterruptedException {
        save(aPlayerProfileWithName(-1001, "aName1", "aNameD1"),
                aPlayerProfileWithName(-1002, "aName2", "aNameD2"),
                aPlayerProfileWithName(-1003, "aName3", "aNameD3"),
                aPlayerProfileWithName(-1004, "aName4", "aNameD4"),
                aPlayerProfileWithName(-1005, "aName5", "aNameD5"));

        final PagedData<PlayerSearchResult> results = underTest.searchByRealOrDisplayName("aName%", 1, 2);

        assertThat(results.getSize(), is(equalTo(2)));
        assertThat(results.getStartPosition(), is(equalTo(2)));
        assertThat(results.getTotalSize(), is(equalTo(5)));
        assertThat(results, contains(searchResultFor(aPlayerProfileWithName(-1003, "aName3", "aNameD3")),
                searchResultFor(aPlayerProfileWithName(-1004, "aName4", "aNameD4"))));
    }

    @Test
    @Transactional
    public void searchingPlayersByNamesFindsTheMatchingEndPage() throws InterruptedException {
        save(aPlayerProfileWithName(-1001, "aName1", "aNameD1"),
                aPlayerProfileWithName(-1002, "aName2", "aNameD2"),
                aPlayerProfileWithName(-1003, "aName3", "aNameD3"),
                aPlayerProfileWithName(-1004, "aName4", "aNameD4"),
                aPlayerProfileWithName(-1005, "aName5", "aNameD5"));

        final PagedData<PlayerSearchResult> results = underTest.searchByRealOrDisplayName("aName%", 2, 2);

        assertThat(results.getSize(), is(equalTo(1)));
        assertThat(results.getStartPosition(), is(equalTo(4)));
        assertThat(results.getTotalSize(), is(equalTo(5)));
        assertThat(results, contains(searchResultFor(aPlayerProfileWithName(-1005, "aName5", "aNameD5"))));
    }

    @Test(expected = NullPointerException.class)
    @Transactional
    public void findingAPlayerSummaryForANullPlayerIdThrowsANullPointerException() {
        underTest.findSummaryById(null);
    }

    @Test
    @Transactional
    public void findingAPlayerSummaryForAnInvalidPlayerIdReturnsAbsent() {
        assertThat(underTest.findSummaryById(BigDecimal.valueOf(-10001)), is(equalTo(Optional.<PlayerSummary>absent())));
    }

    @Test
    @Transactional
    public void findingAPlayerSummaryForAPlayerIdReturnsTheSummary() {
        final PlayerProfile playerProfile = aPlayerProfile();
        save(playerProfile);

        final Optional<PlayerSummary> summary = underTest.findSummaryById(playerProfile.getPlayerId());

        assertThat(summary.get(), is(equalTo(summaryOf(playerProfile))));
    }

    @Test
    @Transactional
    public void findingAPlayerSummaryWithAZeroLastPlayedTimeReturnsTheSummary() {
        // MySQL Connector dislikes 00-00-00 00:00:00 in timestamp fields

        final PlayerProfile playerProfile = aPlayerProfile();
        save(playerProfile);
        jdbc.update("UPDATE PLAYER SET TS_LAST_PLAYED = '00-00-00 00:00:00' WHERE PLAYER_ID = ?", playerProfile.getPlayerId());

        final Optional<PlayerSummary> summary = underTest.findSummaryById(playerProfile.getPlayerId());

        assertThat(summary.get(), is(equalTo(summaryOf(playerProfile))));
    }

    @Test
    @Transactional
    public void findingDisplayNamesForAnEmptySetOfPlayerIdsReturnsAnEmptyMap() {

        final Map<BigDecimal, String> displayNamesByIds = underTest.findDisplayNamesByIds(new HashSet<BigDecimal>());

        assertThat(displayNamesByIds.size(), is(equalTo(0)));
    }

    @Test
    @Transactional
    public void findingDisplayNamesForANullSetOfPlayerIdsReturnsAnEmptyMap() {

        final Map<BigDecimal, String> displayNamesByIds = underTest.findDisplayNamesByIds(null);

        assertThat(displayNamesByIds.size(), is(equalTo(0)));
    }

    @Test
    @Transactional
    public void findingDisplayNamesByIdsReturnsDisplayNamesForAllValidPlayers() {
        saveAPlayerWithName(-1, "display1");
        saveAPlayerWithName(-2, "display2");
        saveAPlayerWithName(-3, "display3");

        final Map<BigDecimal, String> displayNamesByIds = underTest.findDisplayNamesByIds(newHashSet(
                BigDecimal.valueOf(-1), BigDecimal.valueOf(-2), BigDecimal.valueOf(-3), BigDecimal.valueOf(-4)));

        assertThat(displayNamesByIds.size(), is(equalTo(3)));
        assertThat(displayNamesByIds.get(BigDecimal.valueOf(-1)), is(equalTo("display1")));
        assertThat(displayNamesByIds.get(BigDecimal.valueOf(-2)), is(equalTo("display2")));
        assertThat(displayNamesByIds.get(BigDecimal.valueOf(-3)), is(equalTo("display3")));
    }

    private PlayerSummary summaryOf(final PlayerProfile playerProfile) {
        DateTime lastUpdated;
        try {
            Timestamp timestamp = jdbc.queryForObject("SELECT ts_last_played FROM PLAYER WHERE PLAYER_ID=?", Timestamp.class, playerProfile.getPlayerId());

            if (timestamp != null) {
                lastUpdated = new DateTime(timestamp);
            } else {
                lastUpdated = null;
            }
        } catch (DataAccessException e) {
            lastUpdated = null; // MySQL Connector-J won't handle 0 timestamps without driver options
        }
        return new PlayerSummary(playerProfile.getPlayerId(), playerProfile.getPlayerId(),
                "http://a.picture/for/" + PLAYER_ID, lastUpdated, playerProfile.getRegistrationTime(),
                BigDecimal.TEN.setScale(4), playerProfile.getRealName(), playerProfile.getDisplayName(), playerProfile.getEmailAddress(),
                playerProfile.getProviderName(), playerProfile.getExternalId(), playerProfile.getCountry(), playerProfile.getGender(),
                playerProfile.getStatus(), PlayerProfileRole.CUSTOMER, BigDecimal.valueOf(15).setScale(4), purchasesByCurrency(),
                levelsByGame(), Collections.<String>emptySet());
    }

    private Map<String, Integer> levelsByGame() {
        final Map<String, Integer> levelsByGame = new HashMap<>();
        levelsByGame.put("BLACKJACK", 10);
        levelsByGame.put("ROULETTE", 12);
        return levelsByGame;
    }

    private Map<String, BigDecimal> purchasesByCurrency() {
        final Map<String, BigDecimal> purchasesByCurrency = new HashMap<>();
        purchasesByCurrency.put("USD", BigDecimal.valueOf(10).setScale(4));
        purchasesByCurrency.put("GBP", BigDecimal.valueOf(25).setScale(4));
        return purchasesByCurrency;
    }

    private void checkPlayerProfile(final PlayerProfile profile, final PlayerProfile updatedProfile, final Map<String, Object> map) {
        assertEquals(updatedProfile.getEmailAddress(), map.get("EMAIL_ADDRESS").toString());
        assertEquals(updatedProfile.getRealName(), map.get("REAL_NAME").toString());
        assertEquals(updatedProfile.getDisplayName(), map.get("DISPLAY_NAME").toString());
        assertEquals(updatedProfile.getCountry(), map.get("COUNTRY").toString());
        assertEquals(updatedProfile.getFirstName(), map.get("FIRST_NAME").toString());
        assertEquals(updatedProfile.getLastName(), map.get("LAST_NAME").toString());
        final Object date = map.get("DATE_OF_BIRTH");
        final DateTime dateOfBirth = date != null ? new DateTime(((java.sql.Date) date).getTime()) : null;
        assertEquals(updatedProfile.getDateOfBirth(), dateOfBirth);
        assertEquals(updatedProfile.getGender(), Gender.getById(map.get("GENDER").toString()));
        assertEquals(updatedProfile.getProviderName(), map.get("PROVIDER_NAME").toString());
        assertEquals(updatedProfile.getPartnerId().name(), map.get("PARTNER_ID").toString());
        assertEquals(updatedProfile.getRpxProvider(), map.get("RPX_PROVIDER").toString());
        assertEquals(profile.getExternalId(), map.get("EXTERNAL_ID").toString());
        assertEquals(profile.getVerificationIdentifier(), map.get("VERIFICATION_IDENTIFIER").toString());
        assertEquals(updatedProfile.isSyncProfile(), map.get("SYNC_PROFILE"));
    }

    private void assertPlayerProfile(final PlayerProfile expectedPlayerProfile, final Map<String, Object> map) {
        assertEquals(expectedPlayerProfile.getEmailAddress(), map.get("EMAIL_ADDRESS").toString());
        assertEquals(expectedPlayerProfile.getRealName(), map.get("REAL_NAME").toString());
        assertEquals(expectedPlayerProfile.getDisplayName(), map.get("DISPLAY_NAME").toString());
        assertEquals(expectedPlayerProfile.getCountry(), map.get("COUNTRY").toString());
        assertEquals(expectedPlayerProfile.getFirstName(), map.get("FIRST_NAME").toString());
        assertEquals(expectedPlayerProfile.getLastName(), map.get("LAST_NAME").toString());
        final Object date = map.get("DATE_OF_BIRTH");
        final DateTime dateOfBirth = date != null ? new DateTime(((java.sql.Date) date).getTime()) : null;
        assertEquals(expectedPlayerProfile.getDateOfBirth(), dateOfBirth);
        assertEquals(expectedPlayerProfile.getGender(), Gender.getById(map.get("GENDER").toString()));
        assertEquals(expectedPlayerProfile.getProviderName(), map.get("PROVIDER_NAME").toString());
        assertEquals(expectedPlayerProfile.getRpxProvider(), map.get("RPX_PROVIDER").toString());
        assertEquals(expectedPlayerProfile.getExternalId(), map.get("EXTERNAL_ID").toString());
        assertEquals(expectedPlayerProfile.getVerificationIdentifier(), map.get("VERIFICATION_IDENTIFIER").toString());
        assertEquals(expectedPlayerProfile.isSyncProfile(), map.get("SYNC_PROFILE"));
        assertEquals(expectedPlayerProfile.getGuestStatus(), GuestStatus.getById(map.get("GUEST_STATUS").toString()));
        assertEquals(expectedPlayerProfile.getPartnerId().name(), map.get("PARTNER_ID").toString());
        final Object tsReg = map.get("TSREG");
        final DateTime regDate = tsReg != null ? new DateTime(((Timestamp) tsReg).getTime()) : null;
        assertEquals(expectedPlayerProfile.getRegistrationTime(), regDate);
    }

    private PlayerSearchResult searchResultFor(final PlayerProfile playerProfile) {
        return new PlayerSearchResult(playerProfile.getPlayerId(), playerProfile.getEmailAddress(),
                playerProfile.getRealName(), playerProfile.getDisplayName(), playerProfile.getProviderName(),
                "http://a.picture/for/" + playerProfile.getPlayerId(), playerProfile.getStatus(), PlayerProfileRole.CUSTOMER);
    }

    private void save(final PlayerProfile... playerProfiles) {
        for (PlayerProfile playerProfile : playerProfiles) {
            jdbc.update("INSERT INTO ACCOUNT (ACCOUNT_ID,NAME,BALANCE) VALUES (?,?,?)", playerProfile.getPlayerId(), getClass().getName(), 10);
            jdbc.update("INSERT INTO ACCOUNT_STATEMENT (INTERNAL_TRANSACTION_ID,ACCOUNT_ID,CASHIER_NAME,PURCHASE_CURRENCY,"
                            + "PURCHASE_AMOUNT,CHIPS_AMOUNT,TRANSACTION_STATUS) VALUES (?,?,?,?,?,?,?),(?,?,?,?,?,?,?),(?,?,?,?,?,?,?),(?,?,?,?,?,?,?),(?,?,?,?,?,?,?)",
                    "testTx1" + playerProfile.getPlayerId(), playerProfile.getPlayerId(), "testCashier", "USD", 10, 5, "SUCCESS",
                    "testTx2" + playerProfile.getPlayerId(), playerProfile.getPlayerId(), "testCashier", "GBP", 12, 5, "AUTHORISED",
                    "testTx3" + playerProfile.getPlayerId(), playerProfile.getPlayerId(), "testCashier", "USD", 14, 7, "ERROR",
                    "testTx4" + playerProfile.getPlayerId(), playerProfile.getPlayerId(), "testCashier", "USD", 16, 6, "FAILURE",
                    "testTx5" + playerProfile.getPlayerId(), playerProfile.getPlayerId(), "testCashier", "GBP", 13, 5, "SETTLED"
            );
            jdbc.update("INSERT INTO PLAYER (PLAYER_ID,ACCOUNT_ID,NAME,PICTURE_LOCATION,LEVEL) VALUES (?,?,?,?,?)",
                    playerProfile.getPlayerId(), playerProfile.getPlayerId(), playerProfile.getDisplayName(),
                    "http://a.picture/for/" + playerProfile.getPlayerId(), "BLACKJACK\t10\t123\nROULETTE\t12\t321\n");
            underTest.save(playerProfile);
        }
    }

    private PlayerProfile aPlayerProfileWith(final int playerId, final String externalId, final String provider) {
        final PlayerProfile profile = aPlayerProfile();
        profile.setExternalId(externalId);
        profile.setProviderName(provider);
        profile.setPlayerId(BigDecimal.valueOf(playerId));
        profile.setGuestStatus(GuestStatus.NON_GUEST);
        return profile;
    }

    private PlayerProfile aPlayerProfileWithName(final int playerId, final String realName, final String displayName) {
        final PlayerProfile profile = aPlayerProfile();
        profile.setPlayerId(BigDecimal.valueOf(playerId));
        profile.setRealName(realName);
        profile.setDisplayName(displayName);
        return profile;
    }

    private void saveAPlayerWithName(final int playerId, final String displayName) {
        jdbc.update("INSERT INTO ACCOUNT (ACCOUNT_ID,NAME) VALUES (?,?)", playerId, displayName);
        jdbc.update("INSERT INTO PLAYER (PLAYER_ID,ACCOUNT_ID,NAME) VALUES (?,?,?)", playerId, playerId, displayName);
    }

    private PlayerProfile aProfileWithEmail(final int playerId, final String emailAddress) {
        final PlayerProfile profile = aPlayerProfile();
        profile.setPlayerId(BigDecimal.valueOf(playerId));
        profile.setEmailAddress(emailAddress);
        profile.setRealName(Integer.toString(playerId));
        return profile;
    }

    private PlayerProfile aPlayerProfile() {
        final PlayerProfile playerProfile = new PlayerProfile(PLAYER_ID, "email", "displayName", "realName",
                Gender.MALE, "UK", "firstName", "lastName", new DateTime(1970, 3, 20, 0, 0, 0, 0), "referralId", "providerName",
                "rpxProvider", "externalId", false);
        playerProfile.setVerificationIdentifier(VERIFICATION_IDENTIFIER);
        playerProfile.setRegistrationTime(REGISTRATION_TIME);
        playerProfile.setOptIn(false);
        playerProfile.setGuestStatus(GuestStatus.NON_GUEST);
        playerProfile.setPartnerId(YAZINO);
        return playerProfile;
    }

    private PlayerProfile aSimplePlayerProfile() {
        final PlayerProfile playerProfile = new PlayerProfile("email", "displayName", "realName", Gender.MALE, "UK",
                null, null, null, "referralId", "providerName", "rpxProvider", "externalId", false);
        playerProfile.setPlayerId(PLAYER_ID);
        playerProfile.setPartnerId(YAZINO);
        playerProfile.setVerificationIdentifier(VERIFICATION_IDENTIFIER);
        playerProfile.setOptIn(false);
        playerProfile.setRegistrationTime(new DateTime(234524343L).withMillisOfSecond(0));
        playerProfile.setGuestStatus(GuestStatus.GUEST);
        return playerProfile;
    }
}

