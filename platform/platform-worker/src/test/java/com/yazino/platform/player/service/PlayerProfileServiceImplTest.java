package com.yazino.platform.player.service;

import com.google.common.base.Optional;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.Partner;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerCreditConfiguration;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.event.message.PlayerProfileEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.player.*;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.platform.player.persistence.YazinoLoginDao;
import com.yazino.platform.player.updater.PlayerProfileUpdater;
import com.yazino.platform.player.util.Hasher;
import com.yazino.platform.player.util.HasherFactory;
import com.yazino.platform.player.util.PasswordGenerator;
import com.yazino.platform.player.validation.YazinoPlayerValidator;
import com.yazino.platform.session.SessionService;
import com.yazino.platform.worker.message.PlayerVerifiedMessage;
import com.yazino.platform.worker.message.VerificationType;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.Partner.TANGO;
import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.player.GuestStatus.CONVERTED;
import static com.yazino.platform.player.GuestStatus.NON_GUEST;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PlayerProfileServiceImplTest {

    public static final PaymentPreferences PAYMENT_PREFERENCES = new PaymentPreferences();
    public static final String DISPLAY_NAME = "My Display Name";
    public static final String NEW_DISPLAY_NAME = "My New Display Name";
    public static final BigDecimal PLAYER_ID = BigDecimal.TEN.min(BigDecimal.ONE);
    public static final String EXPECTED_AVATAR_URL = "http://lol/pic";
    public static final String EXPECTED_AVATAR_PICTURE_LOCATION_URL = "lol/pic/location";
    public static final String EXPECTED_COUNTRY = "Yazino World";
    public static final DateTime EXPECTED_DATE_OF_BIRTH = new DateTime(101);
    public static final Gender EXPECTED_GENDER = Gender.FEMALE;
    public static final String EXPECTED_GENDER_ID = EXPECTED_GENDER.getId();
    public static final String EXPECTED_EMAIL = "super@thanks.for.asking";
    public static final Avatar EXPECTED_AVATAR = new Avatar(EXPECTED_AVATAR_PICTURE_LOCATION_URL, EXPECTED_AVATAR_URL);
    private static final BigDecimal MAXIMILES_ID = BigDecimal.valueOf(234);
    private static final String EMAIL_ADDRESS = "emailAddress@emailAddress.com";
    private static final String NEW_EMAIL_ADDRESS = "newEmailAddress@emailAddress.com";
    private static final String EMAIL_ADDRESS_2 = "emailAddress2@emailAddress.com";
    private static final String EMAIL_ADDRESS_3 = "emailAddress3@emailAddress.com";
    private static final String HASHED_PASSWORD = "aHashedPassword";
    private static final String PROVIDER_NAME = "providerName";
    private static final String VERIFICATION_IDENTIFIER = "a-verification-identifier";
    private static final String ORIGINAL_AVATAR = "http://avatarURL.com/lovely/picture.jpg";
    private static final String GUEST_EMAIL_ADDRESS = "guest@yazino.com";
    private static final String GUEST_DISPLAY_NAME = "GuestName";
    private static final String NEW_PASSWORD = "new-password";
    private static final String NEW_PASSWORD_HASH = "aNewHashedPassword";
    private static final String PASSWORD = "aGeneratedPassword";
    private static final String PASSWORD_HASH = "aHashedPassword";
    private static final String FACEBOOK_ID = "facebookId";
    private static final String TANGO_ID = "tangoId";
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(6778L);
    private static final BigDecimal GUEST_ACCOUNT_CONVERSION_AMOUNT = BigDecimal.valueOf(3000L);
    private static final BigDecimal REFERRAL_AMOUNT = BigDecimal.valueOf(5000L);
    private static final BigDecimal INITIAL_AMOUNT = BigDecimal.valueOf(2500L);
    private static final String TANGO_PARTNER_ID = "TANGO";

    private PlayerProfileServiceImpl underTest;

    @Mock
    private PlayerProfileDao playerProfileDao;
    @Mock
    private CommunityService communityService;
    @Mock
    private PlayerService playerService;
    @Mock
    private SessionService sessionService;
    @Mock
    private YazinoLoginDao yazinoLoginDao;
    @Mock
    private Hasher hasher;
    @Mock
    private HasherFactory hasherFactory;
    @Mock
    private PasswordGenerator passwordGenerator;
    @Mock
    private QueuePublishingService<PlayerProfileEvent> eventService;
    @Mock
    private QueuePublishingService<PlayerVerifiedMessage> playerVerifiedService;
    @Mock
    private PlayerProfileUpdater playerProfileUpdater1;
    @Mock
    private PlayerProfileUpdater playerProfileUpdater2;
    @Mock
    private YazinoPlayerValidator yazinoPlayerValidator;
    @Mock
    private WalletService walletService;

    private PlayerCreditConfiguration playerCreditConfig = new PlayerCreditConfiguration(INITIAL_AMOUNT, REFERRAL_AMOUNT, GUEST_ACCOUNT_CONVERSION_AMOUNT);

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100);

        when(playerService.getPictureUrl(PLAYER_ID)).thenReturn("http://avatarURL.com/lovely/picture.jpg");
        when(passwordGenerator.generatePassword()).thenReturn(PASSWORD);
        when(hasherFactory.getPreferred()).thenReturn(hasher);
        when(hasher.hash(PASSWORD, null)).thenReturn(PASSWORD_HASH);
        when(hasher.hash(NEW_PASSWORD, null)).thenReturn(NEW_PASSWORD_HASH);
        when(hasher.getType()).thenReturn(PasswordType.MD5);

        when(yazinoLoginDao.findByEmailAddress(EMAIL_ADDRESS)).thenReturn(
                new YazinoLogin(PLAYER_ID, EMAIL_ADDRESS, "theOriginalPasswordHash", PasswordType.MD5, null, 0));

        when(playerService.getPaymentPreferences(PLAYER_ID)).thenReturn(paymentPreferences());

        underTest = new PlayerProfileServiceImpl(playerProfileDao,
                communityService,
                playerService,
                sessionService,
                yazinoLoginDao,
                hasherFactory,
                eventService,
                playerVerifiedService,
                passwordGenerator,
                yazinoPlayerValidator,
                walletService,
                playerCreditConfig);
        underTest.setPlayerProfileUpdaters(asList(playerProfileUpdater1, playerProfileUpdater2));
    }

    @After
    public void resetTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldReturnUserProfileForId() {
        PlayerProfile expectedUserProfile = aPlayerProfileBuilder().asProfile();
        when(playerProfileDao.findByPlayerId(expectedUserProfile.getPlayerId())).thenReturn(expectedUserProfile);
        assertEquals(expectedUserProfile, underTest.findByPlayerId(expectedUserProfile.getPlayerId()));
    }

    // TODO tests around error handling
    // TODO integration test testing transactional... etc
    // TODO really a helper should be used by ProfilePLayerService wrapping updateActive

    @Test
    public void convertGuestToYazinoAccountShouldUpdateCurrentGuestProfile() {
        PlayerProfile guestProfile = givenAFacebookGuestProfile();
        YazinoLogin existingLogin = new YazinoLogin(PLAYER_ID, EMAIL_ADDRESS /* already updated by the time this is relevant */, HASHED_PASSWORD, PasswordType.MD5, null, 0);
        YazinoLogin updatedLogin = new YazinoLogin(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD_HASH, PasswordType.MD5, null, 0);
        when(yazinoLoginDao.findByPlayerId(PLAYER_ID)).thenReturn(existingLogin);

        PlayerProfileServiceResponse response = underTest.convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME);

        PlayerProfile convertedPlayerProfile = PlayerProfile.copy(guestProfile)
                .withGuestStatus(CONVERTED)
                .withDisplayName(NEW_DISPLAY_NAME)
                .withEmailAddress(NEW_EMAIL_ADDRESS)
                .asProfile();

        verify(communityService).asyncUpdatePlayer(PLAYER_ID, NEW_DISPLAY_NAME, ORIGINAL_AVATAR, paymentPreferences());
        verify(playerProfileDao).save(convertedPlayerProfile);
        verify(sessionService).updatePlayerInformation(PLAYER_ID, NEW_DISPLAY_NAME, ORIGINAL_AVATAR);
        verify(eventService).send(asEvent(convertedPlayerProfile, ORIGINAL_AVATAR));
        verify(yazinoLoginDao).save(updatedLogin);
        assertThat(response.isSuccessful(), is(true));
        assertThat(response.getErrors(), is(empty()));
    }

    @Test
    public void convertGuestToYazinoAccountShouldCreditAccountWithConversionAmount() throws WalletServiceException {
        givenAFacebookGuestProfile();
        YazinoLogin existingLogin = new YazinoLogin(PLAYER_ID, EMAIL_ADDRESS, HASHED_PASSWORD, PasswordType.MD5, null, 0);
        when(yazinoLoginDao.findByPlayerId(PLAYER_ID)).thenReturn(existingLogin);
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);

        underTest.convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME);

        verify(walletService).postTransaction(ACCOUNT_ID, GUEST_ACCOUNT_CONVERSION_AMOUNT, "Convert Account", "account conversion", TransactionContext.EMPTY);
    }

    @Test
    public void convertGuestToYazinoAccountShouldPublishBalanceAfterCreditingAccountWithConversionAmount() throws WalletServiceException {
        givenAFacebookGuestProfile();
        YazinoLogin existingLogin = new YazinoLogin(PLAYER_ID, EMAIL_ADDRESS, HASHED_PASSWORD, PasswordType.MD5, null, 0);
        when(yazinoLoginDao.findByPlayerId(PLAYER_ID)).thenReturn(existingLogin);
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        InOrder inorder = inOrder(communityService, walletService);

        underTest.convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME);

        inorder.verify(walletService).postTransaction(ACCOUNT_ID, GUEST_ACCOUNT_CONVERSION_AMOUNT, "Convert Account", "account conversion", TransactionContext.EMPTY);
        inorder.verify(communityService).publishBalance(PLAYER_ID);
    }

    @Test
    public void convertGuestToYazinoAccountShouldNotFailWhenCreditingAccountWithConversionAmountFails() throws WalletServiceException {
        givenAFacebookGuestProfile();
        YazinoLogin existingLogin = new YazinoLogin(PLAYER_ID, EMAIL_ADDRESS, HASHED_PASSWORD, PasswordType.MD5, null, 0);
        when(yazinoLoginDao.findByPlayerId(PLAYER_ID)).thenReturn(existingLogin);
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        doThrow(new WalletServiceException("sample wallet exception"))
                .when(walletService)
                .postTransaction(any(BigDecimal.class), any(BigDecimal.class), anyString(), anyString(), any(TransactionContext.class));

        PlayerProfileServiceResponse playerProfileServiceResponse = underTest.convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME);

        assertTrue(playerProfileServiceResponse.isSuccessful());
    }

    @Test
    public void convertGuestToYazinoAccountShouldRejectPreviouslyConvertedAccount() {
        PlayerProfile guestProfile = givenAFacebookGuestProfile();
        guestProfile.setGuestStatus(CONVERTED);

        PlayerProfileServiceResponse response = underTest.convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME);

        assertThat(response.getErrors(), hasItem(equalTo(new ParameterisedMessage("cannot convert account with non-guest account (status=%s)", "C"))));
        verifyZeroInteractions(communityService, sessionService, eventService, yazinoLoginDao);
        verify(playerProfileDao, never()).save(any(PlayerProfile.class));
    }

    @Test
    public void convertGuestToYazinoAccountShouldRejectNonGuestAccount() {
        PlayerProfile guestProfile = givenAFacebookGuestProfile();
        guestProfile.setGuestStatus(NON_GUEST);

        PlayerProfileServiceResponse response = underTest.convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME);

        assertThat(response.getErrors(), hasItem(equalTo(new ParameterisedMessage("cannot convert account with non-guest account (status=%s)", "N"))));
        verifyZeroInteractions(communityService, sessionService, eventService, yazinoLoginDao);
        verify(playerProfileDao, never()).save(any(PlayerProfile.class));
    }

    @Test
    public void convertGuestToYazinoAccountShouldReportAllValidationErrors() {
        Set<ParameterisedMessage> expectedErrors = new HashSet<>(asList(new ParameterisedMessage("error1"), new ParameterisedMessage("error2")));
        when(yazinoPlayerValidator.validateForYazinoConversion(NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, NEW_PASSWORD)).thenReturn(expectedErrors);

        PlayerProfileServiceResponse response = underTest.convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME);

        verifyZeroInteractions(communityService, sessionService, eventService, yazinoLoginDao);
        verify(playerProfileDao, never()).save(any(PlayerProfile.class));
        assertThat(response.isSuccessful(), is(false));
        assertThat(response.getErrors(), equalTo(expectedErrors));
    }

    @Test
    public void convertGuestToYazinoAccountShouldThrowNonBusinessExceptions() {
        Exception expectedException = new RuntimeException("sample exception");
        when(yazinoPlayerValidator.validateForYazinoConversion(NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, NEW_PASSWORD)).thenThrow(expectedException);

        try {
            underTest.convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME);
            fail("Expected exception");
        } catch (Exception e) {
            assertThat(e, sameInstance(expectedException));
        }
    }

    @Test
    public void convertGuestToYazinoAccountShouldThrowIfPlayerNotFound() {
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(null);

        try {
            underTest.convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME);
            fail("Expected exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), equalTo("Player not found (playerId=" + PLAYER_ID + ")"));
        }
    }

    @Test
    public void convertGuestToYazinoAccountShouldRejectIfYazinoUserAlreadyHasAnAccount() {
        Set<ParameterisedMessage> expectedErrors =
                new HashSet<>(asList(new ParameterisedMessage("error1"), new ParameterisedMessage("Cannot convert to Yazino account. E-mail already registered")));
        when(yazinoPlayerValidator.validateForYazinoConversion(NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, NEW_PASSWORD)).thenReturn(expectedErrors);

        PlayerProfileServiceResponse response = underTest.convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME);

        assertThat(response.isSuccessful(), is(false));
        assertThat(response.getErrors(), hasItem(equalTo(new ParameterisedMessage("Cannot convert to Yazino account. E-mail already registered"))));
        verifyZeroInteractions(communityService, sessionService, eventService, yazinoLoginDao);
        verify(playerProfileDao, never()).save(any(PlayerProfile.class));
    }

    @Test
    public void convertGuestToExternalAccountShouldThrowIfPlayerNotFound() {
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(null);

        try {
            underTest.convertGuestToExternalAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, "FACEBOOK");
            fail("Expected exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), equalTo("Player not found (playerId=" + PLAYER_ID + ")"));
        }
    }

    @Test
    public void convertGuestToExternalAccountShouldRejectIfFacebookUserAlreadyHasAnAccount() {
        PlayerProfile playerProfile = new PlayerProfile();
        when(playerProfileDao.findByProviderNameAndExternalId("FACEBOOK", FACEBOOK_ID)).thenReturn(playerProfile);

        PlayerProfileServiceResponse response = underTest.convertGuestToExternalAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, "FACEBOOK");

        assertThat(response.isSuccessful(), is(false));
        assertThat(response.getErrors(), hasItem(equalTo(new ParameterisedMessage("Facebook user already has an account"))));
        verifyZeroInteractions(communityService, sessionService, eventService, yazinoLoginDao);
        verify(playerProfileDao, never()).save(any(PlayerProfile.class));
    }

    @Test
    public void convertGuestToExternalAccountShouldRejectPreviouslyConvertedAccount() {
        PlayerProfile guestProfile = givenAFacebookGuestProfile();
        guestProfile.setGuestStatus(CONVERTED);

        PlayerProfileServiceResponse response = underTest.convertGuestToExternalAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, "FACEBOOK");

        assertThat(response.getErrors(), hasItem(equalTo(new ParameterisedMessage("cannot convert account with non-guest account (status=%s)", "C"))));
        verifyZeroInteractions(communityService, sessionService, eventService, yazinoLoginDao);
        verify(playerProfileDao, never()).save(any(PlayerProfile.class));
    }

    @Test
    public void convertGuestToExternalAccountShouldRejectNonGuestAccount() {
        PlayerProfile guestProfile = givenAFacebookGuestProfile();
        guestProfile.setGuestStatus(NON_GUEST);

        PlayerProfileServiceResponse response = underTest.convertGuestToExternalAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, "FACEBOOK");

        assertThat(response.getErrors(), hasItem(equalTo(new ParameterisedMessage("cannot convert account with non-guest account (status=%s)", "N"))));
        verifyZeroInteractions(communityService, sessionService, eventService, yazinoLoginDao);
        verify(playerProfileDao, never()).save(any(PlayerProfile.class));
    }

    @Test
    public void convertGuestToExternalAccountShouldThrowNonBusinessExceptions() {
        Exception expectedException = new RuntimeException("sample exception");
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenThrow(expectedException);

        try {
            underTest.convertGuestToExternalAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, "FACEBOOK");
            fail("Expected exception");
        } catch (Exception e) {
            assertThat(e, sameInstance(expectedException));
        }
    }

    @Test
    public void convertGuestToFacebookAccountShouldUpdateCurrentGuestProfile() {
        PlayerProfile guestProfile = givenAFacebookGuestProfile();

        PlayerProfileServiceResponse response = underTest.convertGuestToFacebookAccount(PLAYER_ID,
                FACEBOOK_ID,
                NEW_DISPLAY_NAME,
                NEW_EMAIL_ADDRESS);

        PlayerProfile convertedPlayerProfile = PlayerProfile.copy(guestProfile)
                .withGuestStatus(CONVERTED)
                .withProviderName("FACEBOOK")
                .withRpxProvider("FACEBOOK")
                .withDisplayName(NEW_DISPLAY_NAME)
                .withEmailAddress(NEW_EMAIL_ADDRESS)
                .withExternalId(FACEBOOK_ID)
                .asProfile();

        verify(communityService).asyncUpdatePlayer(PLAYER_ID, NEW_DISPLAY_NAME, ORIGINAL_AVATAR, paymentPreferences());
        verify(playerProfileDao).save(convertedPlayerProfile);
        verify(sessionService).updatePlayerInformation(PLAYER_ID, NEW_DISPLAY_NAME, ORIGINAL_AVATAR);
        verify(eventService).send(asEvent(convertedPlayerProfile, ORIGINAL_AVATAR));
        assertThat(response.isSuccessful(), is(true));
        assertThat(response.getErrors(), is(empty()));
    }

    @Test
    public void convertGuestToTangoAccountShouldUpdateCurrentGuestProfile() {
        PlayerProfile guestProfile = givenAGuestProfile(TANGO_PARTNER_ID);

        PlayerProfileServiceResponse response = underTest.convertGuestToExternalAccount(PLAYER_ID,
                TANGO_ID,
                NEW_DISPLAY_NAME,
                NEW_EMAIL_ADDRESS,
                TANGO_PARTNER_ID);

        PlayerProfile convertedPlayerProfile = PlayerProfile.copy(guestProfile)
                .withGuestStatus(CONVERTED)
                .withProviderName(TANGO_PARTNER_ID)
                .withRpxProvider(TANGO_PARTNER_ID)
                .withDisplayName(NEW_DISPLAY_NAME)
                .withEmailAddress(NEW_EMAIL_ADDRESS)
                .withExternalId(TANGO_ID)
                .withPartnerId(Partner.parse(TANGO_PARTNER_ID))
                .asProfile();

        verify(communityService).asyncUpdatePlayer(PLAYER_ID, NEW_DISPLAY_NAME, ORIGINAL_AVATAR, paymentPreferences());
        verify(playerProfileDao).save(convertedPlayerProfile);
        verify(sessionService).updatePlayerInformation(PLAYER_ID, NEW_DISPLAY_NAME, ORIGINAL_AVATAR);
        verify(eventService).send(asEvent(convertedPlayerProfile, ORIGINAL_AVATAR));
        assertThat(response.isSuccessful(), is(true));
        assertThat(response.getErrors(), is(empty()));
    }

    @Test
    public void convertGuestToExternalAccountShouldDeleteYazinoLogin() {
        givenAFacebookGuestProfile();

        underTest.convertGuestToExternalAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, "FACEBOOK");

        verify(yazinoLoginDao).deleteByPlayerId(PLAYER_ID);
    }

    @Test
    public void convertGuestToExternalAccountShouldCreditAccountWithConversionAmount() throws WalletServiceException {
        givenAFacebookGuestProfile();
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);

        underTest.convertGuestToExternalAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, "FACEBOOK");

        verify(walletService).postTransaction(ACCOUNT_ID, GUEST_ACCOUNT_CONVERSION_AMOUNT, "Convert Account", "account conversion", TransactionContext.EMPTY);
    }

    @Test
    public void convertGuestToExternalAccountShouldPublishBalanceAfterCreditingAccountWithConversionAmount() throws WalletServiceException {
        givenAFacebookGuestProfile();
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        InOrder inOrder = inOrder(walletService, communityService);

        underTest.convertGuestToExternalAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, "FACEBOOK");

        inOrder.verify(walletService).postTransaction(ACCOUNT_ID, GUEST_ACCOUNT_CONVERSION_AMOUNT, "Convert Account", "account conversion", TransactionContext.EMPTY);
        inOrder.verify(communityService).publishBalance(PLAYER_ID);
    }

    @Test
    public void convertGuestToExternalAccountShouldNotFailWhenCreditingAccountWithConversionAmountFails() throws WalletServiceException {
        givenAFacebookGuestProfile();
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        doThrow(new WalletServiceException("sample wallet exception"))
                .when(walletService)
                .postTransaction(any(BigDecimal.class), any(BigDecimal.class), anyString(), anyString(), any(TransactionContext.class));

        PlayerProfileServiceResponse playerProfileServiceResponse = underTest.convertGuestToExternalAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, "FACEBOOK");

        assertTrue(playerProfileServiceResponse.isSuccessful());
    }

    private PlayerProfile givenAFacebookGuestProfile() {
        return givenAGuestProfile("FACEBOOK");
    }

    private PlayerProfile givenAGuestProfile(String partnerId) {
        PlayerProfile guestProfile = PlayerProfile.withPlayerId(PLAYER_ID)
                .withDisplayName(GUEST_DISPLAY_NAME)
                .withEmailAddress(GUEST_EMAIL_ADDRESS)
                .withStatus(PlayerProfileStatus.ACTIVE)
                .withGuestStatus(GuestStatus.GUEST)
                .withPartnerId(partnerId.equals("TANGO") ? TANGO : YAZINO)
                .withProviderName(partnerId)
                .withRpxProvider(partnerId)
                .asProfile();
        given(playerProfileDao.findByPlayerId(PLAYER_ID)).willReturn(guestProfile);
        return guestProfile;
    }

    @Test
    public void shouldUpdateUserProfileAndReturnFalseIfFails() {
        assertFalse(underTest.updatePlayerInfo(PLAYER_ID, getUserProfileInfo()));
    }

    @Test
    public void shouldUpdateUserProfileAndReturnTrueIfAllOK() {
        setUpSuccessfulUpdate();
        PlayerProfileSummary userProfileInfo = getUserProfileInfo();

        underTest.updatePlayerInfo(PLAYER_ID, userProfileInfo);
        assertTrue(underTest.updateDisplayName(PLAYER_ID, DISPLAY_NAME));
    }

    @Test
    public void shouldUpdateUserProfileInCommunityService() {
        PlayerProfile oldUserProfile = setUpSuccessfulUpdate();
        PlayerProfileSummary userProfileInfo = getUserProfileInfo();

        underTest.updatePlayerInfo(PLAYER_ID, userProfileInfo);
        verifyCommunityService(oldUserProfile.getDisplayName(), ORIGINAL_AVATAR);
    }

    @Test
    public void shouldUpdateUserProfileInUserProfileDao() {
        PlayerProfile oldUserProfile = setUpSuccessfulUpdate();
        PlayerProfileSummary userProfileInfo = getUserProfileInfo();

        underTest.updatePlayerInfo(PLAYER_ID, userProfileInfo);

        PlayerProfile expectedUserProfile = PlayerProfile.copy(oldUserProfile)
                .withDateOfBirth(EXPECTED_DATE_OF_BIRTH)
                .withGender(EXPECTED_GENDER)
                .withCountry(EXPECTED_COUNTRY)
                .asProfile();
        verify(playerProfileDao).save(expectedUserProfile);
    }

    @Test
    public void shouldUpdateUserProfileInSessionService() {
        PlayerProfile oldUserProfile = setUpSuccessfulUpdate();
        PlayerProfileSummary userProfileInfo = getUserProfileInfo();

        underTest.updatePlayerInfo(PLAYER_ID, userProfileInfo);

        verifySessionService(oldUserProfile.getDisplayName(), ORIGINAL_AVATAR);
    }

    @Test
    public void shouldUpdateDisplayNameAndReturnTrueIfAllOK() {
        setUpSuccessfulUpdate();
        assertTrue(underTest.updateDisplayName(PLAYER_ID, DISPLAY_NAME));
    }

    @Test
    public void shouldUpdateDisplayNameAndReturnFalseIfFails() {
        assertFalse(underTest.updateDisplayName(PLAYER_ID, DISPLAY_NAME));
    }

    @Test
    public void shouldUpdateDisplayNameOnCommunityService() {
        setUpSuccessfulUpdate();
        underTest.updateDisplayName(PLAYER_ID, DISPLAY_NAME);
        verifyCommunityService(DISPLAY_NAME, ORIGINAL_AVATAR);
    }

    @Test
    public void shouldUpdateDisplayNameOnUserProfileDao() {
        PlayerProfile oldUserProfile = setUpSuccessfulUpdate();

        underTest.updateDisplayName(PLAYER_ID, DISPLAY_NAME);

        PlayerProfile expectedUserProfile = PlayerProfile.copy(oldUserProfile).withDisplayName(DISPLAY_NAME)
                .asProfile();
        verify(playerProfileDao).save(expectedUserProfile);
    }

    @Test
    public void shouldUpdateDisplayNameOnSessionService() {
        setUpSuccessfulUpdate();

        underTest.updateDisplayName(PLAYER_ID, DISPLAY_NAME);

        verifySessionService(DISPLAY_NAME, ORIGINAL_AVATAR);
    }

    @Test
    public void shouldUpdateEmailAddressAndReturnTrueIfAllOK() {
        setUpSuccessfulUpdate();
        assertTrue(underTest.updateEmailAddress(PLAYER_ID, EXPECTED_EMAIL));
    }

    @Test
    public void shouldUpdateEmailAddressAndReturnFalseIFails() {
        assertFalse(underTest.updateEmailAddress(PLAYER_ID, EXPECTED_EMAIL));
    }

    @Test
    public void shouldUpdateEmailAddressOnCommunityService() {
        PlayerProfile oldUserProfile = setUpSuccessfulUpdate();
        underTest.updateEmailAddress(PLAYER_ID, EXPECTED_EMAIL);
        verifyCommunityService(oldUserProfile.getDisplayName(), ORIGINAL_AVATAR);
    }

    @Test
    public void shouldUpdateEmailAddressOnUserProfileDao() {
        PlayerProfile oldUserProfile = setUpSuccessfulUpdate();

        underTest.updateEmailAddress(PLAYER_ID, EXPECTED_EMAIL);

        PlayerProfile expectedUserProfile = PlayerProfile.copy(oldUserProfile).withEmailAddress(EXPECTED_EMAIL)
                .asProfile();
        verify(playerProfileDao).save(expectedUserProfile);
    }

    @Test
    public void shouldUpdateEmailAddressOnSessionService() {
        PlayerProfile oldUserProfile = setUpSuccessfulUpdate();

        underTest.updateEmailAddress(PLAYER_ID, EXPECTED_EMAIL);

        verifySessionService(oldUserProfile.getDisplayName(), ORIGINAL_AVATAR);
    }

    @Test
    public void shouldUpdateAvatarAndReturnTrueIfAllOK() {
        setUpSuccessfulUpdate();
        assertTrue(underTest.updateAvatar(PLAYER_ID, EXPECTED_AVATAR));
    }

    @Test
    public void shouldUpdateAvatarAndReturnFalseIFails() {
        assertFalse(underTest.updateAvatar(PLAYER_ID, EXPECTED_AVATAR));
    }

    @Test
    public void shouldUpdateAvatarOnCommunityService() {
        PlayerProfile oldUserProfile = setUpSuccessfulUpdate();
        underTest.updateAvatar(PLAYER_ID, EXPECTED_AVATAR);
        verifyCommunityService(oldUserProfile.getDisplayName(), EXPECTED_AVATAR_URL);
    }

    @Test
    public void shouldUpdateAvatarOnUserProfileDao() {
        PlayerProfile oldUserProfile = setUpSuccessfulUpdate();

        underTest.updateAvatar(PLAYER_ID, EXPECTED_AVATAR);

        PlayerProfile expectedUserProfile = PlayerProfile.copy(oldUserProfile).asProfile();
        verify(playerProfileDao).save(expectedUserProfile);
    }

    @Test
    public void shouldUpdateAvatarOnSessionService() {
        PlayerProfile oldUserProfile = setUpSuccessfulUpdate();

        underTest.updateAvatar(PLAYER_ID, EXPECTED_AVATAR);

        verifySessionService(oldUserProfile.getDisplayName(), EXPECTED_AVATAR_URL);
    }

    @Test
    public void shouldSaveCorrectYazinoLogin() {
        final YazinoLogin yazinoLogin = aYazinoLogin();
        when(yazinoLoginDao.findByPlayerId(PLAYER_ID)).thenReturn(yazinoLogin);
        PasswordChangeRequest passwordChangeForm = aPasswordChangeRequest();

        String passwordHash = "NEWpasswordHash";
        when(hasher.hash(passwordChangeForm.getNewPassword(), null)).thenReturn(passwordHash);

        underTest.updatePassword(PLAYER_ID, passwordChangeForm);

        YazinoLogin expectedYazinoLogin = YazinoLogin.copy(yazinoLogin).withPasswordHash(passwordHash).asLogin();
        verify(yazinoLoginDao, times(1)).save(expectedYazinoLogin);
    }

    @Test
    public void shouldUpdateFbSyncOnUserProfileDao() {
        PlayerProfile oldUserProfile = setUpSuccessfulUpdate();
        boolean expectedFbSync = true;
        underTest.updateSyncFor(oldUserProfile.getPlayerId(), expectedFbSync);

        PlayerProfile expectedUserProfile = PlayerProfile.copy(oldUserProfile).withSyncProfile(expectedFbSync)
                .asProfile();
        verify(playerProfileDao, times(1)).save(expectedUserProfile);
    }

    @Test
    public void updatingTheRoleDelegatesToTheDAO() {
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(new PlayerProfile());

        underTest.updateRole(PLAYER_ID, PlayerProfileRole.INSIDER);

        verify(playerProfileDao).updateRole(PLAYER_ID, PlayerProfileRole.INSIDER);
    }

    @Test
    public void updatingTheRoleSendsAnEvent() {
        PlayerProfile aPlayerProfile = new PlayerProfile();
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile);

        underTest.updateRole(PLAYER_ID, PlayerProfileRole.INSIDER);

        verify(playerProfileDao).findByPlayerId(PLAYER_ID);
        verify(eventService).send(any(PlayerProfileEvent.class));
    }

    @Test
    public void updatingTheBlockStatusDelegatesToTheDAO() {

        PlayerProfile aPlayerProfile = new PlayerProfile();
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile);

        underTest.updateStatus(PLAYER_ID, PlayerProfileStatus.BLOCKED, "test", "aReason");

        verify(playerProfileDao).updateStatus(PLAYER_ID, PlayerProfileStatus.BLOCKED, "test", "aReason");

        verify(playerProfileDao).findByPlayerId(PLAYER_ID);
        verify(eventService).send(any(PlayerProfileEvent.class));
    }

    @Test(expected = NullPointerException.class)
    public void updatingTheBlockStatusForANullPlayerIdThrowsANullPointerException() {
        underTest.updateStatus(null, PlayerProfileStatus.BLOCKED, "test", "aReason");
    }

    @Test(expected = NullPointerException.class)
    public void updatingTheBlockStatusForANullStatusThrowsANullPointerException() {
        underTest.updateStatus(PLAYER_ID, null, "test", "aReason");
    }

    @Test(expected = NullPointerException.class)
    public void updatingTheBlockStatusForANullChangedByThrowsANullPointerException() {
        underTest.updateStatus(PLAYER_ID, PlayerProfileStatus.BLOCKED, null, "aReason");
    }

    @Test(expected = NullPointerException.class)
    public void updatingTheBlockStatusForANullReasonThrowsANullPointerException() {
        underTest.updateStatus(PLAYER_ID, PlayerProfileStatus.BLOCKED, "test", null);
    }

    @Test
    public void findingAuditRecordsForAPlayerDelegatesToTheDAO() {
        final List<PlayerProfileAudit> expectedAuditRecords = asList(
                anAuditRecord(PlayerProfileStatus.BLOCKED), anAuditRecord(PlayerProfileStatus.ACTIVE));
        when(playerProfileDao.findAuditRecordsFor(PLAYER_ID)).thenReturn(expectedAuditRecords);

        final List<PlayerProfileAudit> actualAuditRecords = underTest.findAuditRecordsFor(PLAYER_ID);

        assertThat(actualAuditRecords, is(equalTo(expectedAuditRecords)));
    }

    private PlayerProfileAudit anAuditRecord(final PlayerProfileStatus status) {
        return new PlayerProfileAudit(PLAYER_ID, PlayerProfileStatus.ACTIVE, status, "test", "aReason", new DateTime(1200000));
    }

    @Test
    public void findingAnExistingPlayerProfileByProviderAndExternalIdShouldReturnThePlayerProfile() {
        final PlayerProfile expectedProfile = aPlayerProfileBuilder().asProfile();
        when(playerProfileDao.findByProviderNameAndExternalId("aProvider", "anExternalId")).thenReturn(expectedProfile);

        final PlayerProfile profile = underTest.findByProviderNameAndExternalId("aProvider", "anExternalId");

        assertThat(profile, is(equalTo(expectedProfile)));
    }

    @Test
    public void findingAnNonExistentPlayerProfileByProviderAndExternalIdShouldReturnNull() {
        final PlayerProfile profile = underTest.findByProviderNameAndExternalId("aProvider", "anExternalId");

        assertThat(profile, is(nullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void findingAPlayerProfileByProviderAndExternalIdShouldThrowANullPointerExceptionWhenTheProviderIsNull() {
        underTest.findByProviderNameAndExternalId(null, "anExternalId");
    }

    @Test(expected = NullPointerException.class)
    public void findingAPlayerProfileByProviderAndExternalIdShouldThrowANullPointerExceptionWhenTheExternalIdIsNull() {
        underTest.findByProviderNameAndExternalId("aProvider", null);
    }

    @Test
    public void resetPasswordShouldSucceedIfProvidedEmailExists() {
        final PlayerProfile expectedProfile = aPlayerProfileBuilder().asProfile();
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(expectedProfile);

        final ResetPasswordResponse result = underTest.resetPassword(EMAIL_ADDRESS);

        assertThat(result.isSuccessful(), is(true));
        verify(yazinoLoginDao).save(new YazinoLogin(
                PLAYER_ID, EMAIL_ADDRESS, HASHED_PASSWORD, PasswordType.MD5, null, 0));
    }

    @Test
    public void resetPasswordShouldIncludeProfileDetailsResetPasswordSucceeds() {
        final PlayerProfile expectedProfile = aPlayerProfileBuilder().asProfile();
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(expectedProfile);

        final ResetPasswordResponse result = underTest.resetPassword(EMAIL_ADDRESS);

        assertThat(result.getPlayerId(), is(equalTo(PLAYER_ID)));
        assertThat(result.getPlayerName(), is(equalTo(expectedProfile.getDisplayName())));
        assertThat(result.getNewPassword(), is(equalTo("aGeneratedPassword")));
    }

    @Test
    public void passwordIsNotResetIfProvidedEmailDoesNotExist() {
        final ResetPasswordResponse result = underTest.resetPassword("bob@bob.com");

        assertThat(result.isSuccessful(), is(false));
        verify(yazinoLoginDao).findByEmailAddress("bob@bob.com");
        verifyNoMoreInteractions(yazinoLoginDao);
    }

    @Test(expected = NullPointerException.class)
    public void passwordResetThrowsANullPointerExceptionIfTheEmailIsNull() {
        underTest.resetPassword(null);
    }

    @Test
    public void countShouldReturnTheResultFromTheDAO() {
        when(playerProfileDao.count()).thenReturn(10);

        assertThat(underTest.count(), is(equalTo(10)));
    }

    @Test(expected = NullPointerException.class)
    public void findingLoginEmailThrowsANullPointerExceptionWhenThePlayerIdIsNull() {
        underTest.findLoginEmailByPlayerId(null);
    }

    @Test
    public void findingLoginEmailReturnsTheEmailAddressForTheAssociatedLogin() {
        when(yazinoLoginDao.findByPlayerId(PLAYER_ID)).thenReturn(
                new YazinoLogin(MAXIMILES_ID, "aLoginEmail", "aPassword", PasswordType.MD5, null, 0));

        final String loginEmail = underTest.findLoginEmailByPlayerId(PLAYER_ID);

        assertThat(loginEmail, is(equalTo("aLoginEmail")));
    }

    @Test
    public void findingLoginEmailReturnsNullWhereNoMatchingLoginIsPresent() {
        when(yazinoLoginDao.findByPlayerId(PLAYER_ID)).thenReturn(null);

        final String loginEmail = underTest.findLoginEmailByPlayerId(PLAYER_ID);

        assertThat(loginEmail, is(nullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void updatingAPlayerProfileAndPasswordThrowsANullPointerExceptionIfThePlayerProfileIsNull() {
        underTest.update(null, "aPassword", ORIGINAL_AVATAR);
    }

    @Test
    public void updatingAPlayerProfileAndPasswordCallsTheFirstMatchingUpdater() {
        when(playerProfileUpdater1.accepts(PROVIDER_NAME)).thenReturn(true);
        final PlayerProfile playerProfile = aPlayerProfileBuilder().asProfile();

        underTest.update(playerProfile, "aPassword", ORIGINAL_AVATAR);

        verify(playerProfileUpdater1).update(playerProfile, "aPassword", ORIGINAL_AVATAR);
        verifyZeroInteractions(playerProfileUpdater2);
    }

    @Test
    public void updatingAPlayerProfileAndPasswordCallsChecksAllUpdaters() {
        when(playerProfileUpdater2.accepts(PROVIDER_NAME)).thenReturn(true);
        final PlayerProfile playerProfile = aPlayerProfileBuilder().asProfile();

        underTest.update(playerProfile, "aPassword", ORIGINAL_AVATAR);

        verify(playerProfileUpdater2).update(playerProfile, "aPassword", ORIGINAL_AVATAR);
        verify(playerProfileUpdater1).accepts(PROVIDER_NAME);
        verifyNoMoreInteractions(playerProfileUpdater1);
    }

    @Test(expected = IllegalStateException.class)
    public void updatingAPlayerProfileThrowsAnIllegalStateExceptionIfNoMatchingUpdatersAreFound() {
        underTest.update(aPlayerProfileBuilder().asProfile(), "aPassword", ORIGINAL_AVATAR);
    }

    @Test
    public void verificationShouldFailWhenEmailAddressIsNull() {
        final boolean success = underTest.verify(null, VERIFICATION_IDENTIFIER, VerificationType.NOT_PLAYED);

        assertThat(success, is(equalTo(false)));
    }

    @Test
    public void verificationShouldFailWhenVerificationIdentifierIsNull() {
        final boolean success = underTest.verify(EMAIL_ADDRESS, null, VerificationType.NOT_PLAYED);

        assertThat(success, is(equalTo(false)));
    }

    @Test
    public void verificationShouldUpdateAnUnverifiedPlayerProfileWithANullIdentifier() {
        when(playerProfileDao.findByEmailAddress(EMAIL_ADDRESS)).thenReturn(
                aPlayerProfileBuilder()
                        .withVerificationIdentifier(VERIFICATION_IDENTIFIER)
                        .asProfile()
        );

        final boolean success = underTest.verify(EMAIL_ADDRESS, VERIFICATION_IDENTIFIER, VerificationType.NOT_PLAYED);

        assertThat(success, is(equalTo(true)));
        verify(playerProfileDao).save(aPlayerProfileBuilder().asProfile());
    }

    @Test
    public void successfulVerificationShouldGenerateANotPlayedVerificationMessage() {
        when(playerProfileDao.findByEmailAddress(EMAIL_ADDRESS)).thenReturn(
                aPlayerProfileBuilder()
                        .withVerificationIdentifier(VERIFICATION_IDENTIFIER)
                        .asProfile()
        );

        underTest.verify(EMAIL_ADDRESS, VERIFICATION_IDENTIFIER, VerificationType.NOT_PLAYED);

        verify(playerVerifiedService).send(new PlayerVerifiedMessage(PLAYER_ID, VerificationType.NOT_PLAYED));
    }

    @Test
    public void successfulVerificationShouldGenerateAPlayedVerificationMessage() {
        when(playerProfileDao.findByEmailAddress(EMAIL_ADDRESS)).thenReturn(
                aPlayerProfileBuilder()
                        .withVerificationIdentifier(VERIFICATION_IDENTIFIER)
                        .asProfile()
        );

        underTest.verify(EMAIL_ADDRESS, VERIFICATION_IDENTIFIER, VerificationType.PLAYED);

        verify(playerVerifiedService).send(new PlayerVerifiedMessage(PLAYER_ID, VerificationType.PLAYED));
    }

    @Test
    public void verificationShouldNotUpdateAnUnverifiedPlayerProfileWithAnIncorrectIdentifier() {
        when(playerProfileDao.findByEmailAddress(EMAIL_ADDRESS)).thenReturn(
                aPlayerProfileBuilder()
                        .withVerificationIdentifier(VERIFICATION_IDENTIFIER)
                        .asProfile()
        );

        final boolean success = underTest.verify(EMAIL_ADDRESS, "an-incorrect-verification-identifier",
                VerificationType.NOT_PLAYED);

        assertThat(success, is(equalTo(false)));
        verify(playerProfileDao).findByEmailAddress(EMAIL_ADDRESS);
        verifyNoMoreInteractions(playerProfileDao);
        verifyZeroInteractions(eventService);
    }

    @Test
    public void verificationShouldFailIfEmailIsNotFound() {
        when(playerProfileDao.findByEmailAddress(EMAIL_ADDRESS)).thenReturn(null);

        final boolean success = underTest.verify(EMAIL_ADDRESS, VERIFICATION_IDENTIFIER, VerificationType.NOT_PLAYED);

        assertThat(success, is(equalTo(false)));
        verify(playerProfileDao).findByEmailAddress(EMAIL_ADDRESS);
        verifyNoMoreInteractions(playerProfileDao);
        verifyZeroInteractions(eventService);
    }

    @Test
    public void verificationShouldFailIfPlayerProfileIsAlreadyVerified() {
        when(playerProfileDao.findByEmailAddress(EMAIL_ADDRESS)).thenReturn(
                aPlayerProfileBuilder().asProfile());

        final boolean success = underTest.verify(EMAIL_ADDRESS, VERIFICATION_IDENTIFIER, VerificationType.NOT_PLAYED);

        assertThat(success, is(equalTo(false)));
        verify(playerProfileDao).findByEmailAddress(EMAIL_ADDRESS);
        verifyNoMoreInteractions(playerProfileDao);
        verifyZeroInteractions(eventService);
    }

    @Test
    public void verificationShouldSendPlayerProfileEventOnSuccess() {
        when(playerProfileDao.findByEmailAddress(EMAIL_ADDRESS)).thenReturn(
                aPlayerProfileBuilder()
                        .withVerificationIdentifier(VERIFICATION_IDENTIFIER)
                        .asProfile()
        );

        final boolean success = underTest.verify(EMAIL_ADDRESS, VERIFICATION_IDENTIFIER, VerificationType.NOT_PLAYED);

        assertThat(success, is(equalTo(true)));
        verify(eventService)
                .send(asEvent(aPlayerProfileBuilder().asProfile(), ORIGINAL_AVATAR));
    }

    @Test
    public void findRegisteredEmailAddresses_shouldReturnAnEmptySetWhenArgIsNull() {
        assertEquals(Collections.<String, BigDecimal>emptyMap(), underTest.findByEmailAddresses((String) null));
    }

    @Test
    public void findRegisteredEmailAddresses_shouldIncludeAddressesAssociatedWithLobbyUsers() {
        String[] candidateEmailAddresses = new String[]{EMAIL_ADDRESS, EMAIL_ADDRESS_2, EMAIL_ADDRESS_3};
        Map<String, BigDecimal> expectedEmailAddresses = new HashMap<>();
        expectedEmailAddresses.put(EMAIL_ADDRESS, BigDecimal.valueOf(1));
        expectedEmailAddresses.put(EMAIL_ADDRESS_2, BigDecimal.valueOf(2));
        when(playerProfileDao.findRegisteredEmailAddresses(candidateEmailAddresses))
                .thenReturn(expectedEmailAddresses);

        Map<String, BigDecimal> actual = underTest.findByEmailAddresses(candidateEmailAddresses);

        assertEquals(expectedEmailAddresses, actual);
    }

    @Test
    public void findRegisteredEmailAddresses_shouldIncludeAddressesAssociatedWithYazionLogins() {
        String[] candidateEmailAddresses = new String[]{EMAIL_ADDRESS, EMAIL_ADDRESS_2, EMAIL_ADDRESS_3};
        Map<String, BigDecimal> expectedEmailAddresses = new HashMap<>();
        expectedEmailAddresses.put(EMAIL_ADDRESS, BigDecimal.valueOf(1));
        expectedEmailAddresses.put(EMAIL_ADDRESS_2, BigDecimal.valueOf(2));

        when(yazinoLoginDao.findRegisteredEmailAddresses(candidateEmailAddresses))
                .thenReturn(expectedEmailAddresses);

        Map<String, BigDecimal> actual = underTest.findByEmailAddresses(candidateEmailAddresses);

        assertEquals(expectedEmailAddresses, actual);
    }

    @Test
    public void findRegisteredExternalIds_shouldIgnoreNullIds() {
        assertEquals(Collections.<String, BigDecimal>emptyMap(), underTest.findByProviderNameAndExternalIds(PROVIDER_NAME, null));
        verifyZeroInteractions(playerProfileDao);
    }

    @Test
    public void findRegisteredExternalIds_shouldIgnoreEmptyIds() {
        assertEquals(Collections.<String, BigDecimal>emptyMap(), underTest.findByProviderNameAndExternalIds(PROVIDER_NAME));
        verifyZeroInteractions(playerProfileDao);
    }

    @Test
    public void findRegisteredExternalIds_shouldReturnDaoResults() {
        final Map<String, BigDecimal> expected = new HashMap<>();
        expected.put("ext1", BigDecimal.valueOf(1));
        when(playerProfileDao.findRegisteredExternalIds(PROVIDER_NAME, "ext1", "ext2")).thenReturn(expected);
        assertEquals(expected, underTest.findByProviderNameAndExternalIds(PROVIDER_NAME, "ext1", "ext2"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void searchingByEmailAddressDelegatesToTheDAO() {
        final PagedData<PlayerSearchResult> expectedResult = mock(PagedData.class);
        when(playerProfileDao.searchByEmailAddress("anEmail", 2, 20)).thenReturn(expectedResult);

        final PagedData<PlayerSearchResult> result = underTest.searchByEmailAddress("anEmail", 2, 20);

        assertThat(result, is(equalTo(expectedResult)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void searchingByEmailAddressWithATrailingWildcardDelegatesToTheDao() {
        final PagedData<PlayerSearchResult> expectedResult = mock(PagedData.class);
        when(playerProfileDao.searchByEmailAddress("anEmail%", 2, 20)).thenReturn(expectedResult);

        final PagedData<PlayerSearchResult> result = underTest.searchByEmailAddress("anEmail%", 2, 20);

        assertThat(result, is(equalTo(expectedResult)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void searchingByEmailAddressThrowsAnIllegalArgumentExceptionForAWildcardQuery() {
        underTest.searchByEmailAddress("%", 2, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void searchingByEmailAddressThrowsAnIllegalArgumentExceptionForAQueryStartingWithAWildcard() {
        underTest.searchByEmailAddress("%test", 2, 20);
    }

    @Test(expected = NullPointerException.class)
    public void searchingByEmailAddressThrowsANullPointerExceptionForNullEmailAddress() {
        underTest.searchByEmailAddress(null, 2, 20);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void searchingByNameDelegatesToTheDAO() {
        final PagedData<PlayerSearchResult> expectedResult = mock(PagedData.class);
        when(playerProfileDao.searchByRealOrDisplayName("aName", 2, 20)).thenReturn(expectedResult);

        final PagedData<PlayerSearchResult> result = underTest.searchByRealOrDisplayName("aName", 2, 20);

        assertThat(result, is(equalTo(expectedResult)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void searchingByNameWithATrailingWildcardDelegatesToTheDao() {
        final PagedData<PlayerSearchResult> expectedResult = mock(PagedData.class);
        when(playerProfileDao.searchByRealOrDisplayName("aName%", 2, 20)).thenReturn(expectedResult);

        final PagedData<PlayerSearchResult> result = underTest.searchByRealOrDisplayName("aName%", 2, 20);

        assertThat(result, is(equalTo(expectedResult)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void searchingByNameThrowsAnIllegalArgumentExceptionForAWildcardQuery() {
        underTest.searchByRealOrDisplayName("%", 2, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void searchingByNameThrowsAnIllegalArgumentExceptionForAQueryStartingWithAWildcard() {
        underTest.searchByRealOrDisplayName("%test", 2, 20);
    }

    @Test(expected = NullPointerException.class)
    public void searchingByNameThrowsANullPointerExceptionForANullName() {
        underTest.searchByRealOrDisplayName(null, 2, 20);
    }

    @Test(expected = NullPointerException.class)
    public void findingASummaryByIdThrowsANullPointerExceptionForANullId() {
        underTest.findSummaryById(null);
    }

    @Test
    public void findingASummaryByIdDelegatesToTheDAO() {
        final PlayerSummary summary = mock(PlayerSummary.class);
        when(playerProfileDao.findSummaryById(PLAYER_ID)).thenReturn(Optional.fromNullable(summary));

        assertThat(underTest.findSummaryById(PLAYER_ID).get(), is(equalTo(summary)));
    }

    @Test
    public void findingDisplayNamesDelegatesToTheDAO() {
        final Map<BigDecimal, String> expectedResult = new HashMap<>();
        expectedResult.put(BigDecimal.valueOf(1), "display1");
        expectedResult.put(BigDecimal.valueOf(2), "display2");
        when(playerProfileDao.findDisplayNamesByIds(newHashSet(BigDecimal.valueOf(1), BigDecimal.valueOf(2))))
                .thenReturn(expectedResult);

        final Map<BigDecimal, String> actualResult = underTest.findDisplayNamesById(newHashSet(BigDecimal.valueOf(1), BigDecimal.valueOf(2)));

        assertThat(actualResult, is(equalTo(expectedResult)));
    }

    @Test
    public void findingDisplayNamesWithANullSetOfIDsReturnsAnEmptyMap() {
        final Map<BigDecimal, String> actualResult = underTest.findDisplayNamesById(null);

        assertThat(actualResult.size(), is(equalTo(0)));
    }

    @Test
    public void findingDisplayNamesWithAnEmptySetOfIDsReturnsAnEmptyMap() {
        final Map<BigDecimal, String> actualResult = underTest.findDisplayNamesById(new HashSet<BigDecimal>());

        assertThat(actualResult.size(), is(equalTo(0)));
    }

    private PlayerProfileEvent asEvent(final PlayerProfile profile, final String avatarUrl) {
        final String gender;
        if (profile.getGender() == null) {
            gender = null;
        } else {
            gender = profile.getGender().getId();
        }

        return new PlayerProfileEvent(profile.getPlayerId(),
                new DateTime(),
                profile.getDisplayName(),
                profile.getRealName(),
                profile.getFirstName(),
                avatarUrl,
                profile.getEmailAddress(),
                profile.getCountry(),
                profile.getExternalId(),
                profile.getVerificationIdentifier(),
                profile.getProviderName(),
                profile.getStatus(),
                profile.getPartnerId(),
                profile.getDateOfBirth(),
                gender,
                profile.getReferralIdentifier(), null,
                false,
                profile.getLastName(),
                guestStatusOf(profile).getId());
    }

    private GuestStatus guestStatusOf(PlayerProfile profile) {
        GuestStatus guestStatus = profile.getGuestStatus();
        if (guestStatus == null) {
            guestStatus = GuestStatus.NON_GUEST;
        }
        return guestStatus;
    }

    private PaymentPreferences paymentPreferences() {
        return new PaymentPreferences(PaymentPreferences.PaymentMethod.CREDITCARD);
    }

    private PlayerProfileSummary getUserProfileInfo() {
        return new PlayerProfileSummary(EXPECTED_GENDER_ID, EXPECTED_COUNTRY, EXPECTED_DATE_OF_BIRTH);
    }

    private PlayerProfile setUpSuccessfulUpdate() {
        PlayerProfile oldUserProfile = aPlayerProfileBuilder().withPlayerId(PLAYER_ID).asProfile();

        when(playerProfileDao.findByPlayerId(oldUserProfile.getPlayerId())).thenReturn(oldUserProfile);
        when(playerService.getPaymentPreferences(oldUserProfile.getPlayerId())).thenReturn(PAYMENT_PREFERENCES);
        return oldUserProfile;
    }

    private void verifySessionService(String displayName, String avatarUrl) {
        verify(sessionService).updatePlayerInformation(PLAYER_ID, displayName, avatarUrl);
    }

    private void verifyCommunityService(String displayName, String avatarUrl) {
        verify(communityService).asyncUpdatePlayer(PLAYER_ID, displayName, avatarUrl, PAYMENT_PREFERENCES);
    }

    private YazinoLogin aYazinoLogin() {
        return new YazinoLogin(BigDecimal.ZERO, "email@email.com", "passwordHash", PasswordType.MD5, null, 0);
    }

    private PasswordChangeRequest aPasswordChangeRequest() {
        return new PasswordChangeRequest("currentPassword", "newPassword", "confirmPassword", BigDecimal.ONE);
    }

    private PlayerProfile.PlayerProfileBuilder aPlayerProfileBuilder() {
        return PlayerProfile.withPlayerId(PLAYER_ID)
                .withEmailAddress(EMAIL_ADDRESS)
                .withDisplayName("displayName")
                .withRealName("realName")
                .withGender(Gender.OTHER)
                .withCountry("country")
                .withFirstName("firstName")
                .withLastName("lastName")
                .withDateOfBirth(new DateTime(1337))
                .withReferralIdentifier("referralIdentifier")
                .withPartnerId(YAZINO)
                .withProviderName(PROVIDER_NAME)
                .withRpxProvider("rpxProvider")
                .withExternalId("externalId")
                .withStatus(PlayerProfileStatus.ACTIVE)
                .withSyncProfile(false)
                .withGuestStatus(GuestStatus.NON_GUEST);
    }
}
