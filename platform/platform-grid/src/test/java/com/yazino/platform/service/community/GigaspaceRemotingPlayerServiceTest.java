package com.yazino.platform.service.community;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerCreditConfiguration;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.event.message.PlayerEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.ProfileInformationBuilder;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.statistic.PlayerLevel;
import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.repository.statistic.PlayerLevelsRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.account.TransactionContext.transactionContext;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.CREDITCARD;
import static com.yazino.platform.community.RelationshipType.*;
import static com.yazino.platform.player.GuestStatus.GUEST;
import static com.yazino.platform.player.GuestStatus.NON_GUEST;
import static com.yazino.platform.reference.Currency.GBP;
import static com.yazino.platform.reference.Currency.USD;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceRemotingPlayerServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(500);
    private static final String PLAYER_NAME = "player100";
    private static final String PICTURE_URL = "aPictureUrl";
    private static final BigDecimal REFERRAL_AMOUNT = BigDecimal.valueOf(3000);
    private static final BigDecimal GUEST_ACCOUNT_CONVERSION_AMOUNT = BigDecimal.valueOf(5500);
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerSessionRepository playerSessionRepository;
    @Mock
    private PlayerLevelsRepository playerLevelsRepository;
    @Mock
    private SequenceGenerator sequenceGenerator;
    @Mock
    private InternalWalletService internalWalletService;
    @Mock
    private ProfileInformationBuilder profileInformationBuilder;
    @Mock
    private QueuePublishingService<PlayerEvent> playerEventService;

    private GigaspaceRemotingPlayerService underTest;

    @Before
    public void setUp() {
        underTest = new GigaspaceRemotingPlayerService(playerRepository,
                playerLevelsRepository, sequenceGenerator, internalWalletService, profileInformationBuilder, playerEventService);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(10000000000L);
    }

    @After
    public void resetTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void updatingPaymentPreferencesWillWriteTheUpdatedDataToTheRepository() {
        when(playerRepository.findById(PLAYER_ID)).thenReturn(aPlayer(PLAYER_ID, ACCOUNT_ID));

        underTest.updatePaymentPreferences(PLAYER_ID, paymentPreferences(USD, CREDITCARD, "US"));

        verify(playerRepository).save(aPlayer(PLAYER_ID, ACCOUNT_ID, paymentPreferences(USD, CREDITCARD, "US")));
    }

    @Test
    public void updatingPaymentPreferencesIgnoresInvalidPlayerIDs() {
        when(playerRepository.findById(PLAYER_ID)).thenReturn(null);

        underTest.updatePaymentPreferences(PLAYER_ID, paymentPreferences(USD, CREDITCARD, "US"));

        verify(playerRepository).findById(PLAYER_ID);
        verifyNoMoreInteractions(playerRepository);
    }

    @Test(expected = NullPointerException.class)
    public void updatingPaymentPreferencesRejectsANullID() {
        underTest.updatePaymentPreferences(null, paymentPreferences(USD, CREDITCARD, "US"));
    }

    @Test(expected = NullPointerException.class)
    public void updatingPaymentPreferencesRejectsNullPreferences() {
        underTest.updatePaymentPreferences(PLAYER_ID, null);
    }

    @Test
    public void createNewPlayerSavesAPlayerToTheRepository() throws WalletServiceException {
        when(sequenceGenerator.next()).thenReturn(PLAYER_ID);
        when(internalWalletService.createAccount("player100")).thenReturn(ACCOUNT_ID);

        underTest.createNewPlayer(
                PLAYER_NAME, PICTURE_URL, NON_GUEST, paymentPreferences(GBP, CREDITCARD, "UK"),
                creditConfig(300));

        verify(playerRepository).save(aPlayer(PLAYER_ID, ACCOUNT_ID));
    }

    @Test
    public void createNewPlayerSavesANewPlayerLevelsToTheRepository() throws WalletServiceException {
        when(sequenceGenerator.next()).thenReturn(PLAYER_ID);
        when(internalWalletService.createAccount("player100")).thenReturn(ACCOUNT_ID);

        underTest.createNewPlayer(
                PLAYER_NAME, PICTURE_URL, NON_GUEST, paymentPreferences(GBP, CREDITCARD, "UK"),
                creditConfig(300));

        verify(playerLevelsRepository).save(new PlayerLevels(PLAYER_ID, Collections.<String, PlayerLevel>emptyMap()));
    }

    @Test(expected = NullPointerException.class)
    public void createNewPlayerRejectsANullDisplayName() {
        underTest.createNewPlayer(null, PICTURE_URL,
                null, paymentPreferences(GBP, CREDITCARD, "UK"), creditConfig(300));
    }

    @Test(expected = NullPointerException.class)
    public void createNewPlayerRejectsANullPictureURL() {
        underTest.createNewPlayer(PLAYER_NAME,
                null, null, paymentPreferences(GBP, CREDITCARD, "UK"), creditConfig(300));
    }

    @Test(expected = NullPointerException.class)
    public void createNewPlayerRejectsANullGuestStatus() {
        underTest.createNewPlayer(PLAYER_NAME,
                PICTURE_URL, null, paymentPreferences(GBP, CREDITCARD, "UK"), creditConfig(300));
    }

    @Test(expected = NullPointerException.class)
    public void createNewPlayerRejectsNullPaymentPreferences() {
        underTest.createNewPlayer(PLAYER_NAME,
                PICTURE_URL, null, null, creditConfig(300));
    }

    @Test(expected = NullPointerException.class)
    public void createNewPlayerRejectsNullCreditConfiguration() {
        underTest.createNewPlayer(PLAYER_NAME,
                PICTURE_URL, null, paymentPreferences(GBP, CREDITCARD, "UK"), null);
    }

    @Test
    public void createNewPlayerReturnsBasicProfileInformation() throws WalletServiceException {
        when(sequenceGenerator.next()).thenReturn(PLAYER_ID);
        when(internalWalletService.createAccount("player100")).thenReturn(ACCOUNT_ID);

        final BasicProfileInformation newPlayer = underTest.createNewPlayer(
                PLAYER_NAME, PICTURE_URL, NON_GUEST, paymentPreferences(GBP, CREDITCARD, "UK"),
                creditConfig(300));

        assertThat(newPlayer, is(equalTo(new BasicProfileInformation(PLAYER_ID, PLAYER_NAME, PICTURE_URL, ACCOUNT_ID))));
    }

    @Test
    public void createNewPlayerCreateANewAccount() throws WalletServiceException {
        when(sequenceGenerator.next()).thenReturn(PLAYER_ID);
        when(internalWalletService.createAccount("player100")).thenReturn(ACCOUNT_ID);

        underTest.createNewPlayer(
                PLAYER_NAME, PICTURE_URL, NON_GUEST, paymentPreferences(GBP, CREDITCARD, "UK"),
                creditConfig(300));

        verify(internalWalletService).createAccount("player100");
    }

    @Test(expected = RuntimeException.class)
    public void createNewPlayerThrowsARuntimeExceptionIfTheWalletThrowsAnException() throws WalletServiceException {
        when(sequenceGenerator.next()).thenReturn(PLAYER_ID);
        when(internalWalletService.createAccount("player100")).thenThrow(new WalletServiceException("aTestException"));

        underTest.createNewPlayer(
                PLAYER_NAME, PICTURE_URL, NON_GUEST, paymentPreferences(GBP, CREDITCARD, "UK"),
                creditConfig(300));
    }

    @Test
    public void createNewPlayerCreditsANonZeroInitialBalanceToTheNewAccountWhenAccountIsNotAGuestAccount() throws WalletServiceException {
        when(sequenceGenerator.next()).thenReturn(PLAYER_ID);
        when(internalWalletService.createAccount("player100")).thenReturn(ACCOUNT_ID);

        underTest.createNewPlayer(
                PLAYER_NAME, PICTURE_URL, NON_GUEST, paymentPreferences(GBP, CREDITCARD, "UK"),
                creditConfig(300));

        verify(internalWalletService).postTransaction(ACCOUNT_ID, BigDecimal.valueOf(300), "Create Account", "account setup", TransactionContext.EMPTY);
    }

    @Test
    public void createNewPlayerDoesNotCreditAZeroInitialBalance() throws WalletServiceException {
        when(sequenceGenerator.next()).thenReturn(PLAYER_ID);
        when(internalWalletService.createAccount("player100")).thenReturn(ACCOUNT_ID);

        underTest.createNewPlayer(
                PLAYER_NAME, PICTURE_URL, NON_GUEST, paymentPreferences(GBP, CREDITCARD, "UK"),
                creditConfig(0));

        verify(internalWalletService, never()).postTransaction(any(BigDecimal.class), any(BigDecimal.class), anyString(), anyString(), any(TransactionContext.class));
    }

    @Test
    public void createNewPlayerShouldNotCreditABalanceToTheNewAccountWhenAccountIsAGuestAccount() throws WalletServiceException {
        when(sequenceGenerator.next()).thenReturn(PLAYER_ID);
        when(internalWalletService.createAccount("player100")).thenReturn(ACCOUNT_ID);

        underTest.createNewPlayer(
                PLAYER_NAME, PICTURE_URL, GUEST, paymentPreferences(GBP, CREDITCARD, "UK"),
                creditConfig(300));

        verify(internalWalletService, never()).postTransaction(any(BigDecimal.class), any(BigDecimal.class), anyString(), anyString(), any(TransactionContext.class));
    }

    @Test(expected = NullPointerException.class)
    public void throwsExceptionWhenNullPlayerId() throws Exception {
        underTest.getBasicProfileInformation(null);
        verifyZeroInteractions(playerRepository);
    }

    @Test
    public void returnsNullWhenPlayerDoesntExists() throws Exception {
        final BasicProfileInformation profileInformation = underTest.getBasicProfileInformation(BigDecimal.ONE);

        Assert.assertThat(profileInformation, is(nullValue()));
        verify(playerRepository, atLeastOnce()).findById(BigDecimal.ONE);
    }

    @Test
    public void playerDetailsHasCorrectName() throws Exception {
        reset(playerRepository);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(aPlayerNamed("FooBar"));

        final BasicProfileInformation details = underTest.getBasicProfileInformation(PLAYER_ID);

        assertEquals("FooBar", details.getName());
    }

    @Test
    public void playerDetailsHasCorrectPlayerId() throws Exception {
        reset(playerRepository);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(aPlayerNamed("FooBar"));

        final BasicProfileInformation details = underTest.getBasicProfileInformation(PLAYER_ID);

        assertEquals(PLAYER_ID, details.getPlayerId());
    }

    @Test
    public void playerDetailsHasCorrectAccountId() throws Exception {
        reset(playerRepository);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(aPlayerNamed("FooBar"));

        final BasicProfileInformation details = underTest.getBasicProfileInformation(PLAYER_ID);

        assertEquals(ACCOUNT_ID, details.getAccountId());
    }

    @Test
    public void aTransactionWithNoPlayerSessionIsPostedToTheWalletWithAnEmptyContext() throws WalletServiceException {
        reset(playerRepository);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(aPlayer(PLAYER_ID, ACCOUNT_ID));
        final BigDecimal expectedBalance = BigDecimal.valueOf(20);
        when(internalWalletService.postTransaction(ACCOUNT_ID, BigDecimal.TEN, "tx type", "ref", TransactionContext.EMPTY)).thenReturn(expectedBalance);

        final BigDecimal newBalance = underTest.postTransaction(PLAYER_ID, null, BigDecimal.TEN, "tx type", "ref");

        assertEquals(expectedBalance, newBalance);
    }

    @Test
    public void aTransactionWithAPlayerSessionIsPostedToTheWalletWithTheSessionIdInTheContext() throws WalletServiceException {
        reset(playerRepository);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(aPlayer(PLAYER_ID, ACCOUNT_ID));
        final PlayerSession session = new PlayerSession();
        session.setSessionId(SESSION_ID);
        when(playerSessionRepository.findAllByPlayer(PLAYER_ID)).thenReturn(asList(session));
        final BigDecimal expectedBalance = BigDecimal.valueOf(20);
        when(internalWalletService.postTransaction(ACCOUNT_ID, BigDecimal.TEN, "tx type", "ref",
                transactionContext().withSessionId(SESSION_ID).build())).thenReturn(expectedBalance);

        final BigDecimal newBalance = underTest.postTransaction(PLAYER_ID, SESSION_ID, BigDecimal.TEN, "tx type", "ref");

        assertEquals(expectedBalance, newBalance);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotPostTransactionIfPlayerNotFound() throws WalletServiceException {
        reset(playerRepository);
        underTest.postTransaction(PLAYER_ID, SESSION_ID, BigDecimal.TEN, "tx type", "ref");
        verifyZeroInteractions(internalWalletService);
    }

    @Test
    public void aFriendsSummaryRequestWritesARequestToTheSpace() {
        underTest.publishFriendsSummary(PLAYER_ID);

        verify(playerRepository).publishFriendsSummary(PLAYER_ID);
    }

    @Test
    public void aFriendsSummaryRequestIgnoredANullPlayerID() {
        underTest.publishFriendsSummary(null);

        verifyZeroInteractions(playerRepository);
    }

    @Test
    public void shouldReturnOnlyFriendsOrderedByNickname() throws Exception {
        Player player = aPlayer(PLAYER_ID, ACCOUNT_ID);
        BigDecimal friendAId = BigDecimal.valueOf(30);
        BigDecimal friendBId = BigDecimal.valueOf(10);
        BigDecimal friendCId = BigDecimal.valueOf(20);

        Map<BigDecimal, Relationship> relationships = new HashMap<>();
        relationships.put(friendAId, new Relationship("My Best Friend - Alfred", FRIEND));
        relationships.put(BigDecimal.valueOf(100), new Relationship("My Wanna be Friend - Dave", NOT_FRIEND));
        relationships.put(BigDecimal.valueOf(110), new Relationship("My Ignorant Friend - George", IGNORED));
        relationships.put(friendBId, new Relationship("My Best Friend - Gareth", FRIEND));
        relationships.put(friendCId, new Relationship("My Best Friend - Zachara", FRIEND));

        player.setRelationships(relationships);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);

        Map<BigDecimal, String> friends = underTest.getFriendsOrderedByNickname(PLAYER_ID);
        List<BigDecimal> expected = asList(friendAId, friendBId, friendCId);
        assertEquals(expected, newArrayList(friends.keySet()));
    }

    @Test
    public void shouldReturnEmptyFriendsWhenPlayerIsALoner() throws Exception {
        when(playerRepository.findById(PLAYER_ID)).thenReturn(aPlayer(PLAYER_ID, ACCOUNT_ID));
        assertTrue(underTest.getFriendsOrderedByNickname(PLAYER_ID).isEmpty());
        assertTrue(underTest.getFriendRequestsOrderedByNickname(PLAYER_ID).isEmpty());
    }

    @Test
    public void shouldReturnOnlyFriendRequestsOrderedByNickname() throws Exception {
        Player player = aPlayer(PLAYER_ID, ACCOUNT_ID);
        BigDecimal desperateFriend = BigDecimal.valueOf(200);

        Map<BigDecimal, Relationship> relationships = new HashMap<>();
        relationships.put(BigDecimal.valueOf(30), new Relationship("My Best Friend - Alfred", FRIEND));
        relationships.put(BigDecimal.valueOf(100), new Relationship("My Wanna be Friend - Dave", NOT_FRIEND));
        relationships.put(desperateFriend, new Relationship("My Desperate Friend - George", INVITATION_RECEIVED));
        relationships.put(BigDecimal.valueOf(10), new Relationship("My Best Friend - Gareth", FRIEND));
        relationships.put(BigDecimal.valueOf(20), new Relationship("My Best Friend - Zachara", FRIEND));

        player.setRelationships(relationships);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);

        List<BigDecimal> requests = underTest.getFriendRequestsOrderedByNickname(PLAYER_ID);
        List<BigDecimal> expected = asList(desperateFriend);
        assertEquals(expected, requests);
    }

    @Test(expected = NullPointerException.class)
    public void friendRegistrationShouldThrowANullPointerExceptionForANullPlayerId() {
        underTest.registerFriends(null, newHashSet(BigDecimal.valueOf(1), BigDecimal.valueOf(2)));
    }

    @Test(expected = NullPointerException.class)
    public void friendRegistrationShouldThrowANullPointerExceptionForANullSetOfFriends() {
        underTest.registerFriends(PLAYER_ID, null);
    }

    @Test
    public void friendRegistrationShouldIgnoreAnEmptySetOfFriends() {
        underTest.registerFriends(PLAYER_ID, Collections.<BigDecimal>emptySet());

        verifyZeroInteractions(playerRepository);
    }

    @Test
    public void friendRegistrationShouldRequestFriendRegistrationFromTheRepository() {
        final HashSet<BigDecimal> friendPlayerIds = newHashSet(BigDecimal.valueOf(1), BigDecimal.valueOf(2));
        underTest.registerFriends(PLAYER_ID, friendPlayerIds);

        verify(playerRepository).requestFriendRegistration(PLAYER_ID, friendPlayerIds);
    }

    @Test
    public void addTagDelegatesToThePlayerRepository() {
        underTest.addTag(PLAYER_ID, "aTag");

        verify(playerRepository).addTag(PLAYER_ID, "aTag");
    }

    @Test
    public void removeTagDelegatesToThePlayerRepository() {
        underTest.removeTag(PLAYER_ID, "aTag");

        verify(playerRepository).removeTag(PLAYER_ID, "aTag");
    }

    private PaymentPreferences paymentPreferences(final Currency currency,
                                                  final PaymentPreferences.PaymentMethod paymentMethod,
                                                  final String country) {
        return new PaymentPreferences(currency, paymentMethod, country);
    }

    private PlayerCreditConfiguration creditConfig(final int initialBalance) {
        return new PlayerCreditConfiguration(BigDecimal.valueOf(initialBalance), REFERRAL_AMOUNT, GUEST_ACCOUNT_CONVERSION_AMOUNT);
    }

    private Player aPlayer(final BigDecimal newPlayerId,
                           final BigDecimal newAccountId) {
        return aPlayer(newPlayerId, newAccountId, paymentPreferences(GBP, CREDITCARD, "UK"));
    }

    private Player aPlayer(final BigDecimal newPlayerId,
                           final BigDecimal newAccountId,
                           final PaymentPreferences paymentPreferences) {
        final Player p = new Player();
        p.setPlayerId(newPlayerId);
        p.setAccountId(newAccountId);
        p.setPictureUrl(PICTURE_URL);
        p.setName("player" + newPlayerId);
        p.setPaymentPreferences(paymentPreferences);
        p.setCreationTime(new DateTime());
        return p;
    }

    private static Player aPlayerNamed(final String name) {
        final Player player = new Player();
        player.setName(name);
        player.setPlayerId(PLAYER_ID);
        player.setAccountId(ACCOUNT_ID);
        return player;
    }

}
