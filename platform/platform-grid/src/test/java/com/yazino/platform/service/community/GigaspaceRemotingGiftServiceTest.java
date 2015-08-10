package com.yazino.platform.service.community;

import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.gifting.CollectionResult;
import com.yazino.platform.gifting.GiftCollectionFailure;
import com.yazino.platform.gifting.PlayerCollectionStatus;
import com.yazino.platform.grid.Executor;
import com.yazino.platform.grid.ExecutorTestUtils;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.community.Gift;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.community.GiftRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.gifting.CollectChoice.GAMBLE;
import static com.yazino.platform.gifting.CollectChoice.TAKE_MONEY;
import static java.math.BigDecimal.valueOf;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceRemotingGiftServiceTest {

    private static final BigDecimal PLAYER_ID = valueOf(123);
    private static final BigDecimal FRIEND_ONE_ID = valueOf(2000);
    private static final BigDecimal FRIEND_TWO_ID = valueOf(1000);
    private static final BigDecimal ACCOUNT_ID = valueOf(689);
    private static final BigDecimal SENDER_ID = valueOf(345);
    private static final BigDecimal GIFT_ID = valueOf(666);
    private static final BigDecimal NEW_GIFT_ID_1 = valueOf(667);
    private static final BigDecimal NEW_GIFT_ID_2 = valueOf(668);
    private static final int MAX_GIFTS_PER_DAY = 15;
    private static final int COLLECTED_TODAY = 4;
    private static final int REMAINING_COLLECTIONS = 11;
    private static final int REGIFT_COOLDOWN_PERIOD_SECONDS = 20;
    private static final int DEFAULT_EXPIRY_OF_GIFT_IN_HOURS = 1;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(38362353453264l);

    @Mock
    private GiftRepository giftRepository;
    @Mock
    private GiftRepository injectedGiftRepository;
    @Mock
    private GiftGamblingService giftGamblingService;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private SequenceGenerator sequenceGenerator;
    @Mock
    private Routing injectedRouting;

    private DateTime endOfGiftPeriod;

    private GigaspaceRemotingGiftService underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());

        endOfGiftPeriod = new DateTime().plusHours(10);

        final Map<String, Object> injectedServices = newHashMap();
        injectedServices.put("giftRepository", injectedGiftRepository);
        injectedServices.put("routing", injectedRouting);
        final Executor executor = ExecutorTestUtils.mockExecutorWith(2, injectedServices);

        final GiftProperties properties = mock(GiftProperties.class);
        when(properties.remainingGiftCollections(COLLECTED_TODAY)).thenReturn(REMAINING_COLLECTIONS);
        when(properties.regiftCooldownPeriodInSeconds()).thenReturn(REGIFT_COOLDOWN_PERIOD_SECONDS);
        when(properties.expiryTimeInHours()).thenReturn(DEFAULT_EXPIRY_OF_GIFT_IN_HOURS);
        when(properties.endOfGiftPeriod()).thenReturn(endOfGiftPeriod);

        underTest = new GigaspaceRemotingGiftService(giftRepository, playerRepository, giftGamblingService,
                properties, sequenceGenerator, executor);

        when(injectedRouting.isRoutedToCurrentPartition(FRIEND_ONE_ID)).thenReturn(false).thenReturn(true);
        when(injectedRouting.isRoutedToCurrentPartition(FRIEND_TWO_ID)).thenReturn(true).thenReturn(false);
        when(injectedGiftRepository.findLocalRecipientsBySender(SENDER_ID, new DateTime().minusSeconds(REGIFT_COOLDOWN_PERIOD_SECONDS)))
                .thenReturn(newHashSet(FRIEND_TWO_ID));
        when(giftRepository.countAvailableForCollection(eq(PLAYER_ID))).thenReturn(1);
        when(giftRepository.countCollectedOn(eq(PLAYER_ID), any(DateTime.class))).thenReturn(COLLECTED_TODAY);
        final Player player = new Player(PLAYER_ID);
        player.setAccountId(ACCOUNT_ID);
        player.setRelationship(FRIEND_ONE_ID, new Relationship("friend1", RelationshipType.FRIEND));
        player.setRelationship(FRIEND_TWO_ID, new Relationship("friend2", RelationshipType.FRIEND));
        when(playerRepository.findById(SENDER_ID)).thenReturn(player);
        when(sequenceGenerator.next(2))
                .thenReturn(newHashSet(NEW_GIFT_ID_1, NEW_GIFT_ID_2))
                .thenThrow(new IllegalStateException("Unexpected sequence query"));
        when(sequenceGenerator.next(1))
                .thenReturn(newHashSet(NEW_GIFT_ID_1))
                .thenThrow(new IllegalStateException("Unexpected sequence query"));
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void giftRequestsAreCreatedForAllValidRecipients() {
        final Set<BigDecimal> sentGifts = underTest.giveGifts(SENDER_ID, newHashSet(FRIEND_ONE_ID, FRIEND_TWO_ID), SESSION_ID);

        verify(giftRepository).requestSendGifts(SENDER_ID, SESSION_ID, singletonMap(FRIEND_ONE_ID, NEW_GIFT_ID_1));
        assertThat(sentGifts, containsInAnyOrder(NEW_GIFT_ID_1));
    }

    @Test
    public void giftRequestsAreCreatedForAllValidFriends() {
        final Set<BigDecimal> sentGifts = underTest.giveGiftsToAllFriends(SENDER_ID, SESSION_ID);

        verify(giftRepository).requestSendGifts(SENDER_ID, SESSION_ID, singletonMap(FRIEND_ONE_ID, NEW_GIFT_ID_1));
        assertThat(sentGifts, containsInAnyOrder(NEW_GIFT_ID_1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void giveGiftsShouldUseRegiftCooldownPeriodFromConfiguration() {
        final Set<BigDecimal> giftIds = underTest.giveGifts(SENDER_ID, newHashSet(FRIEND_ONE_ID), SESSION_ID);

        assertThat(giftIds, containsInAnyOrder(NEW_GIFT_ID_1));
    }

    @Test
    public void acknowledgingGiftsShouldRequestAcknowledgement() {
        underTest.acknowledgeViewedGifts(PLAYER_ID, newHashSet(NEW_GIFT_ID_1, NEW_GIFT_ID_2));

        verify(giftRepository).requestAcknowledgement(PLAYER_ID, newHashSet(NEW_GIFT_ID_1, NEW_GIFT_ID_2));
    }

    @Test
    public void getEndOfGiftPeriodShouldReturnCorrectEndOfGift() {
        DateTime actual = underTest.getEndOfGiftPeriod();

        assertThat(actual, is(equalTo(endOfGiftPeriod)));
    }

    @Test
    public void collectGiftShouldReturnResultsOfGambling() throws GiftCollectionFailure {
        when(giftRepository.findByRecipientAndId(PLAYER_ID, GIFT_ID))
                .thenReturn(anUncollectedGift(GIFT_ID, PLAYER_ID));
        when(giftGamblingService.collectGift(TAKE_MONEY)).thenReturn(valueOf(1000));

        BigDecimal winnings = underTest.collectGift(PLAYER_ID, GIFT_ID, TAKE_MONEY, SESSION_ID);

        assertThat(winnings, is(valueOf(1000)));
    }

    @Test
    public void playerShouldNotBeAbleCollectMoreGiftsThanDefined() {
        when(giftRepository.countCollectedOn(eq(PLAYER_ID), any(DateTime.class))).thenReturn(MAX_GIFTS_PER_DAY);

        try {
            underTest.collectGift(PLAYER_ID, GIFT_ID, TAKE_MONEY, SESSION_ID);
            fail();
        } catch (GiftCollectionFailure giftCollectionFailure) {
            assertThat(giftCollectionFailure.getCollectionResult(), is(CollectionResult.MAX_COLLECTION_LIMIT_REACHED));
        }

        verify(giftRepository, times(1)).countCollectedOn(eq(PLAYER_ID), any(DateTime.class));
        verifyNoMoreInteractions(giftRepository, playerRepository);
    }

    @Test
    public void pushPlayerCollectionStatusShouldPush() {
        reset(giftRepository);
        when(giftRepository.countCollectedOn(eq(PLAYER_ID), any(DateTime.class))).thenReturn(COLLECTED_TODAY);
        final int availableForCollection = 2;
        when(giftRepository.countAvailableForCollection(PLAYER_ID)).thenReturn(availableForCollection);

        underTest.pushPlayerCollectionStatus(PLAYER_ID);

        ArgumentCaptor<PlayerCollectionStatus> status = ArgumentCaptor.forClass(PlayerCollectionStatus.class);
        verify(giftRepository).publishCollectionStatus(eq(PLAYER_ID), status.capture());
        assertThat(status.getValue().getCollectionsRemainingForCurrentPeriod(), equalTo(REMAINING_COLLECTIONS));
        assertThat(status.getValue().getGiftsWaitingToBeCollected(), equalTo(availableForCollection));
    }

    @Test
    public void collectGiftShouldRequestGiftCollection() throws GiftCollectionFailure {
        final Gift gift = anUncollectedGift(GIFT_ID, PLAYER_ID);
        final BigDecimal winnings = valueOf(1000);
        when(giftGamblingService.collectGift(GAMBLE)).thenReturn(winnings);
        when(giftRepository.findByRecipientAndId(PLAYER_ID, GIFT_ID)).thenReturn(gift);

        underTest.collectGift(PLAYER_ID, GIFT_ID, GAMBLE, SESSION_ID);

        verify(giftRepository).requestCollection(PLAYER_ID, GIFT_ID, SESSION_ID, winnings, GAMBLE);
    }

    private Gift anUncollectedGift(final BigDecimal giftId, final BigDecimal recipientId) {
        return new Gift(giftId, SENDER_ID, recipientId, new DateTime(), new DateTime().plusHours(10), null, false);
    }

}
