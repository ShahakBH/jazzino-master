package com.yazino.platform.player.service;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerCreditConfiguration;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.email.AsyncEmailService;
import com.yazino.platform.event.message.PlayerProfileEvent;
import com.yazino.platform.event.message.PlayerReferrerEvent;
import com.yazino.platform.invitation.InvitationService;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileStatus;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.reference.ReferenceService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.player.GuestStatus.NON_GUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class PlayerProfileFactoryTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(9000);
    private static final String DISPLAY_NAME = "aDisplayName";
    private static final String PICTURE_URL = "aPictureUrl";
    private static final String COUNTRY = "GB";
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(7000);
    private static final String REMOTE_ADDRESS = "aRemoteAddress";
    public static final String PROVIDER_NAME = "FACEBOOK";
    private static final GuestStatus GUEST_STATUS = NON_GUEST;

    @Mock
    private PlayerProfileDao playerProfileDao;
    @Mock
    private QueuePublishingService<PlayerProfileEvent> playerProfileEventService;
    @Mock
    private QueuePublishingService<PlayerReferrerEvent> playerReferrerEventService;
    @Mock
    private PlayerService playerService;
    @Mock
    private ReferenceService referenceService;
    @Mock
    private AsyncEmailService asyncEmailService;

    private PlayerCreditConfiguration playerCreditConfiguration;

    private PlayerProfileFactory underTest;

    private DateTime registrationTime;

    @Mock
    private InvitationService invitationService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(10000);
        registrationTime = new DateTime();

        playerCreditConfiguration = new PlayerCreditConfiguration(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(200), BigDecimal.valueOf(3000));

        when(referenceService.getPreferredCurrency(COUNTRY)).thenReturn(Currency.GBP);
        when(playerService.createNewPlayer(DISPLAY_NAME,
                PICTURE_URL, GUEST_STATUS, thePaymentPreferences(), playerCreditConfiguration))
                .thenReturn(theBasicProfile());

        underTest = new PlayerProfileFactory(playerProfileDao, playerProfileEventService, playerService, referenceService,
                playerCreditConfiguration, invitationService, playerReferrerEventService, asyncEmailService);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void factoryCannotBeCreatedWithANullPlayerProfileDao() {
        new PlayerProfileFactory(null, playerProfileEventService, playerService,
                referenceService, playerCreditConfiguration, invitationService, playerReferrerEventService, asyncEmailService);
    }

    @Test(expected = NullPointerException.class)
    public void factoryCannotBeCreatedWithANullPlayerProfileEventService() {
        new PlayerProfileFactory(playerProfileDao, null, playerService,
                referenceService, playerCreditConfiguration, invitationService, playerReferrerEventService, asyncEmailService);
    }

    @Test(expected = NullPointerException.class)
    public void factoryCannotBeCreatedWithANullReferenceDataSource() {
        new PlayerProfileFactory(playerProfileDao, playerProfileEventService,
                playerService, null, playerCreditConfiguration, invitationService, playerReferrerEventService, asyncEmailService);
    }

    @Test(expected = NullPointerException.class)
    public void factoryCannotBeCreatedWithANullPlayerService() {
        new PlayerProfileFactory(playerProfileDao, playerProfileEventService,
                null, referenceService, playerCreditConfiguration, invitationService, playerReferrerEventService, asyncEmailService);
    }

    @Test(expected = NullPointerException.class)
    public void factoryCannotBeCreatedWithANullPlayerCreditConfiguration() {
        new PlayerProfileFactory(playerProfileDao, playerProfileEventService,
                playerService, referenceService, null, invitationService, playerReferrerEventService, asyncEmailService);
    }

    @Test(expected = NullPointerException.class)
    public void factoryCannotBeCreatedWithANullAsyncEmailService() {
        new PlayerProfileFactory(playerProfileDao, playerProfileEventService,
                playerService, referenceService, playerCreditConfiguration, invitationService, playerReferrerEventService, null);
    }

    @Test(expected = NullPointerException.class)
    public void creationThrowsAnExceptionIfThePlayerProfileIdIsNull() {
        underTest.createAndPersistPlayerAndSendPlayerRegistrationEvent(null, REMOTE_ADDRESS, null, Platform.FACEBOOK_CANVAS, PICTURE_URL);
    }

    @Test
    public void creationReturnsTheBasicProfileInformation() {
        final BasicProfileInformation newProfile = underTest.createAndPersistPlayerAndSendPlayerRegistrationEvent(
                aPlayerProfile().asProfile(), REMOTE_ADDRESS, null, Platform.IOS, PICTURE_URL);

        assertThat(newProfile, is(equalTo(theBasicProfile())));
    }

    @Test
    public void creationUpdatesThePassedProfileWithThePlayerId() {
        final PlayerProfile playerProfile = aPlayerProfile().asProfile();
        underTest.createAndPersistPlayerAndSendPlayerRegistrationEvent(playerProfile, REMOTE_ADDRESS, null, Platform.FACEBOOK_CANVAS, PICTURE_URL);

        assertThat(playerProfile.getPlayerId(), is(equalTo(PLAYER_ID)));
    }

    @Test
    public void creationCreatesANewPlayer() {
        underTest.createAndPersistPlayerAndSendPlayerRegistrationEvent(aPlayerProfile().asProfile(), REMOTE_ADDRESS, null, Platform.IOS, PICTURE_URL);

        verify(playerService).createNewPlayer(DISPLAY_NAME, PICTURE_URL, GUEST_STATUS, thePaymentPreferences(), playerCreditConfiguration);
    }

    @Test
    public void creationCreatesANewPlayerProfile() {
        PlayerProfile playerProfileToCreate = aPlayerProfile().asProfile();
        underTest.createAndPersistPlayerAndSendPlayerRegistrationEvent(playerProfileToCreate, REMOTE_ADDRESS, null, Platform.ANDROID, PICTURE_URL);

        verify(playerProfileDao).save(aPlayerProfile()
                .withRegistrationTime(registrationTime)
                .withPlayerId(PLAYER_ID)
                .asProfile());
    }

    @Test
    public void creationAsynchronouslyVerifiesTheEmailAddress() {
        final PlayerProfile playerProfile = aPlayerProfile().withEmailAddress("anEmailAddress").asProfile();

        underTest.createAndPersistPlayerAndSendPlayerRegistrationEvent(playerProfile, REMOTE_ADDRESS, null, Platform.ANDROID, PICTURE_URL);

        verify(asyncEmailService).verifyAddress(playerProfile.getEmailAddress());
    }

    @Test
    public void creationSendsAPlayerProfileEvent() {
        underTest.createAndPersistPlayerAndSendPlayerRegistrationEvent(thePlayerProfile(), REMOTE_ADDRESS, "referrer", Platform.ANDROID, PICTURE_URL);

        verify(playerProfileEventService).send(theEvent());
        verify(playerReferrerEventService).send(theReferrer());
    }

    private PlayerProfile thePlayerProfile() {
        return aPlayerProfile().withReferralIdentifier("inviterID").withPartnerId(YAZINO).asProfile();
    }

    private PlayerReferrerEvent theReferrer() {
        return new PlayerReferrerEvent(PLAYER_ID, "referrer", "ANDROID", null);
    }

    private PlayerProfileEvent theEvent() {
        return new PlayerProfileEvent(PLAYER_ID, new DateTime(), DISPLAY_NAME, null, null,
                PICTURE_URL, null, COUNTRY, null, null, PROVIDER_NAME, PlayerProfileStatus.ACTIVE, YAZINO, null, null, "inviterID", REMOTE_ADDRESS, true, null, NON_GUEST.getId());
    }

    @Test
    public void shouldSendInvitationAcceptedEvent_FACEBOOK() {
        PlayerProfile profile = aPlayerProfile().asProfile();
        profile.setExternalId("externalId");
        profile.setProviderName(PROVIDER_NAME);

        underTest.createAndPersistPlayerAndSendPlayerRegistrationEvent(profile, REMOTE_ADDRESS, null, Platform.ANDROID, PICTURE_URL);

        verify(invitationService).invitationAccepted(profile.getExternalId(), InvitationSource.FACEBOOK, registrationTime, PLAYER_ID);
    }

    @Test
    public void shouldSendInvitationAcceptedEvent_Yazino() {
        PlayerProfile profile = aPlayerProfile().asProfile();
        profile.setEmailAddress("emailAddress");
        profile.setProviderName("YAZINO");

        underTest.createAndPersistPlayerAndSendPlayerRegistrationEvent(profile, REMOTE_ADDRESS, null, Platform.ANDROID, PICTURE_URL);

        verify(invitationService).invitationAccepted(profile.getEmailAddress(), InvitationSource.EMAIL, registrationTime, PLAYER_ID);
    }

    private PaymentPreferences thePaymentPreferences() {
        return new PaymentPreferences(Currency.GBP);
    }

    private BasicProfileInformation theBasicProfile() {
        return new BasicProfileInformation(PLAYER_ID, DISPLAY_NAME, PICTURE_URL, ACCOUNT_ID);
    }

    private PlayerProfile.PlayerProfileBuilder aPlayerProfile() {
        return PlayerProfile.withPlayerId(null)
                .withProviderName(PROVIDER_NAME)
                .withDisplayName(DISPLAY_NAME)
                .withCountry(COUNTRY)
                .withGuestStatus(GUEST_STATUS);
    }
}
