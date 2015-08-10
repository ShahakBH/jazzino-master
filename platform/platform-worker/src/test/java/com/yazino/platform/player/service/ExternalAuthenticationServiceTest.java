package com.yazino.platform.player.service;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.player.*;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.platform.player.updater.PlayerProfileUpdater;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class ExternalAuthenticationServiceTest {
    private static final Partner PARTNER_ID = Partner.YAZINO;
    private static final String REMOTE_ADDRESS = "aRemoteAddress";
    private static final String EXTERNAL_ID = "externalId";
    private static final String PROVIDER_NAME = "provider";
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);
    public static final String SLOTS = "SLOTS";

    @Mock
    private PlayerProfileUpdater playerProfileUpdater;
    @Mock
    private PlayerProfileFactory playerProfileFactory;
    @Mock
    private PlayerProfileDao playerProfileDao;
    @Mock
    private PlayerService playerService;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpServletRequest request;
    @Mock
    private PlayerInformationHolder playerInformationHolder;

    private PlayerProfile newPlayerProfile;
    private PlayerProfile existingPlayerProfile;

    private ExternalAuthenticationService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new ExternalAuthenticationService(playerProfileUpdater,
                playerProfileFactory, playerProfileDao, playerService);

        newPlayerProfile = aPlayerProfile().asProfile();
        existingPlayerProfile = aPlayerProfile().asProfile();

        when(playerProfileDao.findByProviderNameAndExternalId(PROVIDER_NAME, EXTERNAL_ID)).thenReturn(existingPlayerProfile);
        when(playerInformationHolder.getPlayerProfile()).thenReturn(newPlayerProfile);
        when(playerInformationHolder.getSessionKey()).thenReturn("aSessionKey");
        when(playerInformationHolder.getAvatarUrl()).thenReturn("aPictureUrl");
    }

    @Test
    public void authenticationShouldFailIfTheExternalIdIsNull() {
        final PlayerProfileAuthenticationResponse result = underTest.authenticate(PROVIDER_NAME, null);

        assertThat(result.isSuccessful(), is(equalTo(false)));
    }

    @Test
    public void authenticationShouldFailIfTheExternalIdIsBlank() {
        final PlayerProfileAuthenticationResponse result = underTest.authenticate(PROVIDER_NAME, "   ");

        assertThat(result.isSuccessful(), is(equalTo(false)));
    }

    @Test
    public void authenticationShouldFailIfTheProviderNameIsNull() {
        final PlayerProfileAuthenticationResponse result = underTest.authenticate(null, EXTERNAL_ID);

        assertThat(result.isSuccessful(), is(equalTo(false)));
    }

    @Test
    public void authenticationShouldFailIfTheProviderNameIsBlank() {
        final PlayerProfileAuthenticationResponse result = underTest.authenticate("  ", EXTERNAL_ID);

        assertThat(result.isSuccessful(), is(equalTo(false)));
    }

    @Test
    public void authenticationShouldFailIfThePlayerDoesNotExist() {
        reset(playerProfileDao);

        final PlayerProfileAuthenticationResponse result = underTest.authenticate(PROVIDER_NAME, EXTERNAL_ID);

        assertThat(result.isSuccessful(), is(equalTo(false)));
    }

    @Test
    public void authenticationShouldFailIfThePlayerProfileIsBlocked() {
        reset(playerProfileDao);
        when(playerProfileDao.findByProviderNameAndExternalId(PROVIDER_NAME, EXTERNAL_ID)).thenReturn(aPlayerProfile()
                .withStatus(PlayerProfileStatus.BLOCKED).asProfile());

        final PlayerProfileAuthenticationResponse result = underTest.authenticate(PROVIDER_NAME, EXTERNAL_ID);

        assertThat(result.isSuccessful(), is(equalTo(false)));
        assertThat(result.isBlocked(), is(equalTo(true)));
    }

    @Test
    public void authenticationShouldFailIfThePlayerProfileIsClosed() {
        reset(playerProfileDao);
        when(playerProfileDao.findByProviderNameAndExternalId(PROVIDER_NAME, EXTERNAL_ID)).thenReturn(aPlayerProfile()
                .withStatus(PlayerProfileStatus.CLOSED).asProfile());

        final PlayerProfileAuthenticationResponse result = underTest.authenticate(PROVIDER_NAME, EXTERNAL_ID);

        assertThat(result.isSuccessful(), is(equalTo(false)));
        assertThat(result.isBlocked(), is(equalTo(true)));
    }

    @Test
    public void authenticationShouldSucceedIfThePlayerExistsAndIsNotBlocked() {
        final PlayerProfileAuthenticationResponse result = underTest.authenticate(PROVIDER_NAME, EXTERNAL_ID);

        assertThat(result.isSuccessful(), is(equalTo(true)));
    }

    @Test
    public void loginShouldFailIfTheUserInformationHolderDoesNotProviderAPlayerProfile() {
        reset(playerInformationHolder);

        final PlayerProfileLoginResponse result = underTest.login(REMOTE_ADDRESS, PARTNER_ID,
                playerInformationHolder, null, Platform.FACEBOOK_CANVAS, null);

        assertThat(result.getLoginResult(), is(equalTo(LoginResult.FAILURE)));
    }

    @Test
    public void loginShouldFailIfThePlayerProfileIsBlocked() {
        reset(playerProfileDao);
        when(playerProfileDao.findByProviderNameAndExternalId(PROVIDER_NAME, EXTERNAL_ID)).thenReturn(aPlayerProfile()
                .withStatus(PlayerProfileStatus.BLOCKED).asProfile());

        final PlayerProfileLoginResponse result = underTest.login(REMOTE_ADDRESS, PARTNER_ID,
                playerInformationHolder, null, Platform.FACEBOOK_CANVAS, null);

        assertThat(result.getLoginResult(), is(equalTo(LoginResult.BLOCKED)));
    }

    @Test
    public void loginShouldFailIfThePlayerProfileIsClosed() {
        reset(playerProfileDao);
        when(playerProfileDao.findByProviderNameAndExternalId(PROVIDER_NAME, EXTERNAL_ID)).thenReturn(aPlayerProfile()
                .withStatus(PlayerProfileStatus.CLOSED).asProfile());

        final PlayerProfileLoginResponse result = underTest.login(REMOTE_ADDRESS, PARTNER_ID,
                playerInformationHolder, null, Platform.FACEBOOK_CANVAS, null);

        assertThat(result.getLoginResult(), is(equalTo(LoginResult.BLOCKED)));
    }

    @Test
    public void aSuccessfulLoginForAnExistingPlayerShouldUpdateThePlayerProfile() {
        reset(playerInformationHolder);
        when(playerInformationHolder.getPlayerProfile()).thenReturn(
                aPlayerProfile().withDisplayName("aNewDisplayName").asProfile());
        when(playerInformationHolder.getAvatarUrl()).thenReturn("aPictureUrl");

        underTest.login(REMOTE_ADDRESS, PARTNER_ID, playerInformationHolder, null, Platform.FACEBOOK_CANVAS, null);

        verify(playerProfileUpdater).update(aPlayerProfile().withDisplayName("aNewDisplayName").asProfile(), null, "aPictureUrl");
    }

    @Test
    public void aSuccessfulLoginForAnExistingPlayerShouldNotUpdateThePlayerProfileIfSyncIsFalse() {
        reset(playerInformationHolder);
        when(playerInformationHolder.getAvatarUrl()).thenReturn("aNewAvatarUrl");
        when(playerInformationHolder.getPlayerProfile()).thenReturn(aPlayerProfile()
                .withSyncProfile(false)
                .withDisplayName("aNewDisplayName")
                .withCountry("aNewCountry")
                .withEmailAddress("aNewEmailAddress")
                .withDateOfBirth(new DateTime())
                .withGender(Gender.OTHER).asProfile());

        underTest.login(REMOTE_ADDRESS, PARTNER_ID, playerInformationHolder, null, Platform.FACEBOOK_CANVAS, null);

        verify(playerProfileUpdater).update(existingPlayerProfile, null, "aNewAvatarUrl");
    }

    @Test
    public void aSuccessLoginContinuesIfThePlayerUpdateFails() {
        reset(playerInformationHolder);
        when(playerInformationHolder.getPlayerProfile()).thenReturn(
                aPlayerProfile().withDisplayName("aNewDisplayName").asProfile());
        when(playerProfileUpdater.update(Matchers.any(PlayerProfile.class), Matchers.any(String.class), Matchers.any(String.class)))
                .thenThrow(new RuntimeException("aTestException"));

        final PlayerProfileLoginResponse result = underTest.login(REMOTE_ADDRESS, PARTNER_ID,
                playerInformationHolder, null, Platform.FACEBOOK_CANVAS, null);

        assertThat(result.getPlayerId(), is(equalTo(PLAYER_ID)));
        assertThat(result.getLoginResult(), is(equalTo(LoginResult.EXISTING_USER)));
    }

    @Test
    public void aSuccessfulLoginForAnExistingUserShouldReturnTheUpdatedResult() {
        final PlayerProfileLoginResponse result = underTest.login(
                REMOTE_ADDRESS, PARTNER_ID, playerInformationHolder, null, Platform.FACEBOOK_CANVAS, null);

        assertThat(result.getPlayerId(), is(equalTo(PLAYER_ID)));
        assertThat(result.getLoginResult(), is(equalTo(LoginResult.EXISTING_USER)));
    }

    @Test
    public void aSuccessfulLoginShouldRegisterTheUsersFriends() {
        when(playerInformationHolder.getFriends()).thenReturn(newHashSet("friend1", "friend2", "friend3"));
        when(playerProfileDao.findPlayerIdsByProviderNameAndExternalIds(newHashSet("friend1", "friend2", "friend3"), PROVIDER_NAME))
                .thenReturn(newHashSet(BigDecimal.valueOf(11), BigDecimal.valueOf(12)));

        underTest.login(REMOTE_ADDRESS, PARTNER_ID, playerInformationHolder, null, Platform.FACEBOOK_CANVAS, null);

        verify(playerService).asyncRegisterFriends(PLAYER_ID, newHashSet(BigDecimal.valueOf(11), BigDecimal.valueOf(12)));
    }

    @Test
    public void aSuccessfulLoginContinuesIfFriendRegistrationFails() {
        when(playerInformationHolder.getFriends()).thenReturn(newHashSet("friend1", "friend2", "friend3"));
        when(playerProfileDao.findPlayerIdsByProviderNameAndExternalIds(newHashSet("friend1", "friend2", "friend3"), PROVIDER_NAME))
                .thenThrow(new RuntimeException("anException"));

        underTest.login(REMOTE_ADDRESS, PARTNER_ID, playerInformationHolder, null, Platform.FACEBOOK_CANVAS, null);
    }

    @Test
    public void loginShouldRegisterAPlayerProfileIfDoesNotExist() {
        reset(playerProfileDao);
        when(playerProfileDao.findByProviderNameAndExternalId(PROVIDER_NAME, EXTERNAL_ID)).thenReturn(null);

        underTest.login(REMOTE_ADDRESS, PARTNER_ID, playerInformationHolder, null, Platform.FACEBOOK_CANVAS, SLOTS);

        verify(playerProfileFactory).createAndPersistPlayerAndSendPlayerRegistrationEvent(newPlayerProfile, REMOTE_ADDRESS, null, Platform.FACEBOOK_CANVAS, "aPictureUrl", SLOTS);
    }

    @Test
    public void ifANewRegistrationCannotBeSavedThenAFailureIsReturned() {
        reset(playerProfileDao);
        reset(playerProfileFactory);

        final PlayerProfileLoginResponse result = underTest.login(REMOTE_ADDRESS, PARTNER_ID, playerInformationHolder, null, Platform.FACEBOOK_CANVAS, null);

        assertThat(result.getLoginResult(), is(equalTo(LoginResult.FAILURE)));
    }

    @Test
    public void loginShouldReturnTheCreatedNewStatusWhenANewPlayerProfileIsCreated() {
        reset(playerProfileDao);
        when(playerProfileFactory.createAndPersistPlayerAndSendPlayerRegistrationEvent(newPlayerProfile, REMOTE_ADDRESS, null, Platform.FACEBOOK_CANVAS, "aPictureUrl", null))
                .thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        final PlayerProfile playerProfile = (PlayerProfile) invocation.getArguments()[0];
                        playerProfile.setPlayerId(PLAYER_ID);
                        return new BasicProfileInformation(PLAYER_ID, "aPlayerName", "aPictureUrl", BigDecimal.valueOf(123));
                    }
                });

        when(playerProfileDao.findByProviderNameAndExternalId(PROVIDER_NAME, EXTERNAL_ID)).thenReturn(null);

        final PlayerProfileLoginResponse result = underTest.login(REMOTE_ADDRESS,
                PARTNER_ID, playerInformationHolder, null, Platform.FACEBOOK_CANVAS, null);

        assertThat(result.getPlayerId(), is(equalTo(PLAYER_ID)));
        assertThat(result.getLoginResult(), is(equalTo(LoginResult.NEW_USER)));
    }

    @Test
    public void loginShouldPassGameTypeToCreateAndAuthenticatePlayer() {
        reset(playerProfileDao);
        when(playerProfileDao.findByProviderNameAndExternalId(PROVIDER_NAME, EXTERNAL_ID)).thenReturn(null);

        underTest.login(REMOTE_ADDRESS, PARTNER_ID, playerInformationHolder, null, Platform.ANDROID, SLOTS);

        verify(playerProfileFactory).createAndPersistPlayerAndSendPlayerRegistrationEvent(newPlayerProfile, REMOTE_ADDRESS, null, Platform.ANDROID, "aPictureUrl", SLOTS);
    }


    private PlayerProfile.PlayerProfileBuilder aPlayerProfile() {
        return PlayerProfile.withPlayerId(PLAYER_ID)
                .withEmailAddress("email")
                .withDisplayName("displayName")
                .withRealName("realName")
                .withGender(Gender.MALE)
                .withCountry("US")
                .withReferralIdentifier("ref")
                .withProviderName(PROVIDER_NAME)
                .withExternalId(EXTERNAL_ID)
                .withStatus(PlayerProfileStatus.ACTIVE)
                .withSyncProfile(true);
    }

}
