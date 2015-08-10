package com.yazino.platform.player.service;

import com.yazino.platform.Platform;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.player.*;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.platform.player.persistence.YazinoLoginDao;
import com.yazino.platform.player.util.Hasher;
import com.yazino.platform.player.util.HasherFactory;
import com.yazino.platform.player.validation.YazinoPlayerValidator;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class YazinoAuthenticationServiceTest {
    private static final BigDecimal MAXIMILES_ID = BigDecimal.ONE;
    public static final String VERIFICATION_IDENTIFIER = "dummy";
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final String REMOTE_ADDRESS = "aRemoteAddress";
    private static final String REF = "ref";
    public static final String SLOTS = "SLOTS";

    @Mock
    private YazinoLoginDao yazinoLoginDao;
    @Mock
    private Hasher hasher;
    @Mock
    private HasherFactory hasherFactory;
    @Mock
    private PlayerProfileDao playerProfileDao;
    @Mock
    private YazinoPlayerValidator yazinoPlayerValidator;
    @Mock
    private PlayerProfileFactory playerProfileFactory;

    private YazinoAuthenticationService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(hasherFactory.getPreferred()).thenReturn(hasher);
        when(hasherFactory.forType(PasswordType.MD5)).thenReturn(hasher);
        when(hasher.hash("aPassword", null)).thenReturn("aPasswordHash");
        when(hasher.hash("anIncorrectPassword", null)).thenReturn("anIncorrectPasswordHash");
        when(hasher.getType()).thenReturn(PasswordType.MD5);

        when(yazinoPlayerValidator.validate("anEmail", "aPassword", aMinimalPlayerProfile(), "anAvatarURL", true))
                .thenReturn(new HashSet<ParameterisedMessage>());
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile().asProfile());
        when(playerProfileFactory.createAndPersistPlayerAndSendPlayerRegistrationEvent(aMinimalPlayerProfile(), REMOTE_ADDRESS, REF, Platform.FACEBOOK_CANVAS, "anAvatarURL", SLOTS))
                .thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        final PlayerProfile playerProfile = (PlayerProfile) invocation.getArguments()[0];
                        playerProfile.setPlayerId(PLAYER_ID);
                        return new BasicProfileInformation(PLAYER_ID, "aPlayerName", "aPictureUrl", BigDecimal.valueOf(123));
                    }
                });

        underTest = new YazinoAuthenticationService(yazinoLoginDao, hasherFactory, playerProfileDao,
                yazinoPlayerValidator, playerProfileFactory) {
            @Override
            String generateUniqueId() {
                return VERIFICATION_IDENTIFIER;
            }
        };
    }

    @Test
    public void authenticationFailsWhenTheEmailAddressIsNull() throws Exception {
        final PlayerProfileAuthenticationResponse result = underTest.authenticate(null, "aPassword");

        assertThat(result.isSuccessful(), is(equalTo(false)));
    }

    @Test
    public void authenticationFailsWhenTheEmailAddressIsEmpty() throws Exception {
        final PlayerProfileAuthenticationResponse result = underTest.authenticate("  ", "aPassword");

        assertThat(result.isSuccessful(), is(equalTo(false)));
    }

    @Test
    public void authenticationFailsWhenThePasswordIsNull() throws Exception {
        final PlayerProfileAuthenticationResponse result = underTest.authenticate("anEmail", null);

        assertThat(result.isSuccessful(), is(equalTo(false)));
    }

    @Test
    public void authenticationFailsWhenThePasswordIsEmpty() throws Exception {
        final PlayerProfileAuthenticationResponse result = underTest.authenticate("anEmail", "   ");

        assertThat(result.isSuccessful(), is(equalTo(false)));
    }

    @Test
    public void authenticationFailsWhenTheEmailAddressDoesNotExist() throws Exception {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(null);

        final PlayerProfileAuthenticationResponse result = underTest.authenticate("anEmail", "aPassword");

        assertThat(result.isSuccessful(), is(equalTo(false)));
    }

    @Test
    public void authenticationFailsWhenThePasswordDoesNotMatchThatRecorded() throws Exception {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(
                aYazinoLogin());

        final PlayerProfileAuthenticationResponse result = underTest.authenticate("anEmail", "anIncorrectPassword");

        assertThat(result.isSuccessful(), is(equalTo(false)));
    }

    private YazinoLogin aYazinoLogin() {
        return new YazinoLogin(PLAYER_ID, "anEmail", "aPasswordHash", PasswordType.MD5, null, 0);
    }

    @Test
    public void authenticationUpdatesThePasswordHashIfItDiffersFromThePreferredPasswordType() throws Exception {
        reset(hasherFactory);
        final Hasher preferredHasher = mock(Hasher.class);
        when(hasherFactory.forType(PasswordType.MD5)).thenReturn(hasher);
        when(hasherFactory.getPreferred()).thenReturn(preferredHasher);
        when(preferredHasher.generateSalt()).thenReturn("salt".getBytes());
        when(preferredHasher.hash("aPassword", "salt".getBytes())).thenReturn("aNewPasswordHash");
        when(preferredHasher.getType()).thenReturn(PasswordType.PBKDF2);
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(aYazinoLogin());

        underTest.authenticate("anEmail", "aPassword");

        verify(yazinoLoginDao).save(new YazinoLogin(PLAYER_ID, "anEmail",
                "aNewPasswordHash", PasswordType.PBKDF2, "salt".getBytes(), 0));
    }

    @Test
    public void authenticationEatsExceptionsCausedIfUpdatingThePasswordHashFails() throws Exception {
        reset(hasherFactory);
        final Hasher preferredHasher = mock(Hasher.class);
        when(hasherFactory.forType(PasswordType.MD5)).thenReturn(hasher);
        when(hasherFactory.getPreferred()).thenReturn(preferredHasher);
        when(preferredHasher.generateSalt()).thenReturn("salt".getBytes());
        when(preferredHasher.hash("aPassword", "salt".getBytes())).thenReturn("aNewPasswordHash");
        when(preferredHasher.getType()).thenReturn(PasswordType.PBKDF2);
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(aYazinoLogin());
        doThrow(new RuntimeException("aTestException")).when(yazinoLoginDao).save(
                new YazinoLogin(PLAYER_ID, "anEmail",
                        "aNewPasswordHash", PasswordType.PBKDF2, "salt".getBytes(), 0));

        underTest.authenticate("anEmail", "aPassword");

        verify(yazinoLoginDao).save(new YazinoLogin(PLAYER_ID, "anEmail",
                "aNewPasswordHash", PasswordType.PBKDF2, "salt".getBytes(), 0));
    }

    @Test
    public void authenticationDoesNotUpdateThePasswordHashIfMatchesThePreferredPasswordType() throws Exception {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(aYazinoLogin());

        underTest.authenticate("anEmail", "aPassword");

        verify(yazinoLoginDao).findByEmailAddress("anEmail");
        verifyNoMoreInteractions(yazinoLoginDao);
    }

    @Test
    public void authenticationSucceedsWhenThePasswordMatchesThatRecorded() throws Exception {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(aYazinoLogin());

        final PlayerProfileAuthenticationResponse result = underTest.authenticate("anEmail", "aPassword");

        assertThat(result.isSuccessful(), is(equalTo(true)));
    }

    @Test
    public void authenticationFailsWhenTheUserIsBlocked() throws Exception {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(aYazinoLogin());
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile()
                .withStatus(PlayerProfileStatus.BLOCKED).asProfile());

        final PlayerProfileAuthenticationResponse result = underTest.authenticate("anEmail", "aPassword");

        assertThat(result.isSuccessful(), is(equalTo(false)));
        assertThat(result.isBlocked(), is(equalTo(true)));
    }

    @Test
    public void authenticationFailsWhenTheUserIsClosed() throws Exception {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(aYazinoLogin());
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile()
                .withStatus(PlayerProfileStatus.CLOSED).asProfile());

        final PlayerProfileAuthenticationResponse result = underTest.authenticate("anEmail", "aPassword");

        assertThat(result.isSuccessful(), is(equalTo(false)));
        assertThat(result.isBlocked(), is(equalTo(true)));
    }

    @Test
    public void registrationFailsWhenTheValidatorFails() throws Exception {
        returnValidationErrors("bad email");

        final PlayerProfileRegistrationResponse result = underTest.register(
                "anEmail", "aPassword", aMinimalPlayerProfile(), REMOTE_ADDRESS, REF, Platform.FACEBOOK_CANVAS, "anAvatarURL", SLOTS);

        assertThat(result.isSuccessful(), is(equalTo(false)));
        assertThat(result.getErrors(), contains(messageWith("bad email")));
    }

    @Test
    public void registrationFailsWhenTheEmailAlreadyExists() throws Exception {
        when(yazinoLoginDao.existsWithEmailAddress("anemail")).thenReturn(true);

        final PlayerProfileRegistrationResponse result = underTest.register(
                "anEmail", "aPassword", aMinimalPlayerProfile(), REMOTE_ADDRESS, REF, Platform.FACEBOOK_CANVAS, "anAvatarURL", SLOTS);

        assertThat(result.isSuccessful(), is(equalTo(false)));
        assertThat(result.getErrors(), contains(messageWith("E-mail already registered")));
    }

    @Test(expected = NullPointerException.class)
    public void registrationThrowsAnExceptionWhenThePlayerProfileIsNull() throws Exception {
        underTest.register("anEmail", "aPassword", null, REMOTE_ADDRESS, REF, Platform.FACEBOOK_CANVAS, "anAvatarURL", SLOTS);
    }

    @Test
    public void registrationSucceedsWhenDataValidatesCorrectly() throws Exception {
        final PlayerProfileRegistrationResponse result = underTest.register(
                "anEmail", "aPassword", aMinimalPlayerProfile(), REMOTE_ADDRESS, REF, Platform.FACEBOOK_CANVAS, "anAvatarURL", SLOTS);

        assertThat(result.isSuccessful(), is(equalTo(true)));
        assertThat(result.getErrors().isEmpty(), is(equalTo(true)));
        assertThat(result.getPlayerId(), is(equalTo(PLAYER_ID)));
    }

    @Test
    public void aSuccessfulRegistrationCreatesANewPlayerProfile() throws Exception {
        underTest.register("anEmail", "aPassword", aMinimalPlayerProfile(), REMOTE_ADDRESS, REF, Platform.FACEBOOK_CANVAS, "anAvatarURL", SLOTS);

        final PlayerProfile createProfile = aMinimalPlayerProfile();
        createProfile.setPlayerId(PLAYER_ID);
        verify(playerProfileFactory).createAndPersistPlayerAndSendPlayerRegistrationEvent(createProfile, REMOTE_ADDRESS, REF, Platform.FACEBOOK_CANVAS, "anAvatarURL", SLOTS);
    }

    @Test
    public void aSuccessfulRegistrationSavesTheYazinoLogin() throws Exception {
        underTest.register("anEmail", "aPassword", aMinimalPlayerProfile(), REMOTE_ADDRESS, REF, Platform.FACEBOOK_CANVAS, "anAvatarURL", SLOTS);

        verify(yazinoLoginDao).save(aYazinoLogin());
    }

    @Test
    public void loginReturnsTheBlockedResultWhenThePlayerProfileIsBlocked() {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(
                new YazinoLogin(PLAYER_ID, "anEmail", "aPasswordHash", PasswordType.MD5, null, 0));
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile()
                .withStatus(PlayerProfileStatus.BLOCKED).asProfile());

        final PlayerProfileLoginResponse result = underTest.login("anEmail", "aPassword");

        assertThat(result.getLoginResult(), is(equalTo(LoginResult.BLOCKED)));
    }

    @Test
    public void loginReturnsTheBlockedResultWhenThePlayerProfileIsClosed() {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(
                new YazinoLogin(PLAYER_ID, "anEmail", "aPasswordHash", PasswordType.MD5, null, 0));
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile()
                .withStatus(PlayerProfileStatus.CLOSED).asProfile());

        final PlayerProfileLoginResponse result = underTest.login("anEmail", "aPassword");

        assertThat(result.getLoginResult(), is(equalTo(LoginResult.BLOCKED)));
    }

    @Test
    public void loginIncrementsTheLoginAttemptsWhenThePlayerProfileIsBlocked() {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(
                new YazinoLogin(BigDecimal.ONE, "anEmail", "aPasswordHash", PasswordType.MD5, null, 0));
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile()
                .withStatus(PlayerProfileStatus.BLOCKED).asProfile());

        underTest.login("anEmail", "aPassword");

        verify(yazinoLoginDao).incrementLoginAttempts("anEmail");
    }

    @Test
    public void loginIncrementsTheLoginAttemptsWhenThePlayerProfileIsClosed() {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(
                new YazinoLogin(BigDecimal.ONE, "anEmail", "aPasswordHash", PasswordType.MD5, null, 0));
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile()
                .withStatus(PlayerProfileStatus.CLOSED).asProfile());

        underTest.login("anEmail", "aPassword");

        verify(yazinoLoginDao).incrementLoginAttempts("anEmail");
    }

    @Test
    public void loginReturnsTheFailureResultForIncorrectUsernames() {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(null);

        final PlayerProfileLoginResponse result = underTest.login("anEmail", "aPassword");

        assertThat(result.getLoginResult(), is(equalTo(LoginResult.FAILURE)));
    }

    @Test
    public void loginReturnsTheFailureResultForIncorrectPasswords() {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(
                new YazinoLogin(PLAYER_ID, "anEmail", "anotherPasswordHash", PasswordType.MD5, null, 0));

        final PlayerProfileLoginResponse result = underTest.login("anEmail", "aPassword");

        assertThat(result.getLoginResult(), is(equalTo(LoginResult.FAILURE)));
    }

    @Test
    public void loginIncrementsTheLoginAttemptsOnFailure() {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(null);

        underTest.login("anEmail", "aPassword");

        verify(yazinoLoginDao).incrementLoginAttempts("anEmail");
    }

    @Test
    public void loginDoesNotIncrementTheLoginAttemptsOnFailureDueToANullEmailAddress() {
        underTest.login(null, "aPassword");

        verify(yazinoLoginDao, times(0)).incrementLoginAttempts(null);
    }

    @Test
    public void loginFailureDoesNotPropagateErrorsFromIncrementingTheLoginAttempts() {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(null);
        doThrow(new RuntimeException("aTestException")).when(yazinoLoginDao).incrementLoginAttempts("anEmail");

        underTest.login("anEmail", "aPassword");
    }

    @Test
    public void aSuccessfulLoginReturnsTheUserUpdatedResult() {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(aYazinoLogin());

        final PlayerProfileLoginResponse result = underTest.login("anEmail", "aPassword");

        assertThat(result.getLoginResult(), is(equalTo(LoginResult.EXISTING_USER)));
    }

    @Test
    public void aSuccessfulLoginResetLoginAttempts() {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(aYazinoLogin());

        underTest.login("anEmail", "aPassword");

        verify(yazinoLoginDao).resetLoginAttempts("anEmail");
    }

    @Test
    public void aSuccessfulLoginChecksThePasswordUsingTheAppropriateHasher() {
        final byte[] salt = {1, 2, 3, 4};
        final Hasher expectedHasher = mock(Hasher.class);
        when(hasherFactory.forType(PasswordType.PBKDF2)).thenReturn(expectedHasher);
        when(expectedHasher.hash("aPassword", salt)).thenReturn("aPasswordHash");
        final YazinoLogin login = new YazinoLogin(PLAYER_ID, "anEmail", "aPasswordHash",
                PasswordType.PBKDF2, salt, 0);
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(login);

        underTest.login("anEmail", "aPassword");

        verify(expectedHasher).hash("aPassword", salt);
    }

    @Test
    public void aSuccessfulLoginDoesNotPropagateErrorsFromResettingLoginAttempts() {
        when(yazinoLoginDao.findByEmailAddress("anEmail")).thenReturn(aYazinoLogin());
        doThrow(new RuntimeException("aTestException")).when(yazinoLoginDao).resetLoginAttempts("anEmail");

        underTest.login("anEmail", "aPassword");

        verify(yazinoLoginDao).resetLoginAttempts("anEmail");
    }

    private void returnValidationErrors(final String... errorStrings) {
        reset(yazinoPlayerValidator);
        Set<ParameterisedMessage> errors = new HashSet<ParameterisedMessage>();
        for (String errorString : errorStrings) {
            errors.add(new ParameterisedMessage(errorString));
        }
        when(yazinoPlayerValidator.validate("anEmail", "aPassword", aMinimalPlayerProfile(), "anAvatarURL", true)).thenReturn(errors);
    }

    private ParameterisedMessage messageWith(final String text) {
        return new ParameterisedMessage(text);
    }

    private PlayerProfile aMinimalPlayerProfile() {
        final PlayerProfile profile = new PlayerProfile();
        profile.setVerificationIdentifier(VERIFICATION_IDENTIFIER);
        profile.setStatus(PlayerProfileStatus.ACTIVE);
        return profile;
    }

    private PlayerProfile.PlayerProfileBuilder aPlayerProfile() {
        return PlayerProfile.withPlayerId(PLAYER_ID)
                .withEmailAddress("anEmail")
                .withDisplayName("aDisplayName")
                .withRealName("aRealName")
                .withGender(Gender.MALE)
                .withCountry("aCountry")
                .withFirstName("aFirstName")
                .withLastName("aLastName")
                .withDateOfBirth(new DateTime(1975, 10, 3, 0, 0, 0, 0))
                .withReferralIdentifier("aReferralId")
                .withProviderName("YAZINO")
                .withRpxProvider("YAZINO")
                .withExternalId("anExternalId")
                .withStatus(PlayerProfileStatus.ACTIVE)
                .withSyncProfile(false);
    }


}
