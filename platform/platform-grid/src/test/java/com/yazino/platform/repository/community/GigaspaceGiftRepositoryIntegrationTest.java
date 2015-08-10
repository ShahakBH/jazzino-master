package com.yazino.platform.repository.community;

import com.yazino.platform.gifting.CollectChoice;
import com.yazino.platform.gifting.PlayerCollectionStatus;
import com.yazino.platform.model.community.*;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaspaceGiftRepositoryIntegrationTest {
    private static final BigDecimal SENDER_ID = BigDecimal.valueOf(32000);
    private static final BigDecimal RECIPIENT_ID = BigDecimal.valueOf(64000);
    private static final BigDecimal ANOTHER_RECIPIENT_ID = BigDecimal.valueOf(64001);
    private static final int EXPIRY_HOURS = 2;
    private static final int GIFT_RETENTION = 168;

    @Autowired
    private GigaspaceGiftRepository underTest;
    @Autowired
    private GigaSpace gigaSpace;

    private DateTime created;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(341587562700L);

        gigaSpace.clear(new Gift());
        gigaSpace.clear(new SendGiftRequest());

        created = new DateTime().withMillisOfSecond(0);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void findingLocalGiftsBySenderFindsAllGiftsFromTheSenderSinceTheGivenTime() {
        final Gift aGiftFromAnotherSender = aGift(-2, created);
        aGiftFromAnotherSender.setSendingPlayer(BigDecimal.ONE);
        gigaSpace.writeMultiple(new Gift[]{aGift(-1, created), aGiftFromAnotherSender, aGift(-3, created)});

        final Set<BigDecimal> recipients = underTest.findLocalRecipientsBySender(SENDER_ID, new DateTime().minusHours(1));

        assertThat(recipients, containsInAnyOrder(RECIPIENT_ID));
    }

    @Test
    public void findingLocalGiftsBySenderReturnsAnEmptySetIfThereAreNoGiftsLocally() {
        final Gift aGiftFromAnotherSender = aGift(-2, created);
        aGiftFromAnotherSender.setSendingPlayer(BigDecimal.ONE);
        gigaSpace.writeMultiple(new Gift[]{aGiftFromAnotherSender});

        final Set<BigDecimal> recipients = underTest.findLocalRecipientsBySender(SENDER_ID, new DateTime().minusHours(1));

        assertThat(recipients, is(empty()));
    }

    @Test
    public void aGiftCanBeSavedToTheSpace() {
        underTest.save(aGift(-1, created));

        final Gift gift = gigaSpace.readById(Gift.class, BigDecimal.valueOf(-1));
        assertThat(gift, is(equalTo(aGift(-1, created))));
    }

    @Test
    public void aGiftCanBeFoundById() {
        gigaSpace.write(aGift(-1, created));

        final Gift gift = underTest.findByRecipientAndId(RECIPIENT_ID, BigDecimal.valueOf(-1));

        assertThat(gift, is(equalTo(aGift(-1, created))));
    }

    @Test
    @Transactional
    public void aGiftCanBeLockedById() {
        gigaSpace.write(aGift(-1, created));

        final Gift gift = underTest.lockByRecipientAndId(RECIPIENT_ID, BigDecimal.valueOf(-1));

        assertThat(gift, is(equalTo(aGift(-1, created))));
    }

    @Test
    public void findingANonExistentGiftReturnsNull() {
        final Gift gift = underTest.findByRecipientAndId(RECIPIENT_ID, BigDecimal.valueOf(-1));

        assertThat(gift, is(nullValue()));
    }

    @Test
    public void availableGiftsCanBeFoundByTheRecipient() {
        final Gift collectedGift = aGift(-4, created);
        collectedGift.setCollected(new DateTime());
        final Gift expiredGift = aGift(-5, created);
        expiredGift.setExpiry(new DateTime().minusDays(1));
        expiredGift.setAcknowledged(true);
        gigaSpace.writeMultiple(new Gift[]{aGift(-1, created), aGiftToSomeoneElse(-2, created), aGift(-3, created), collectedGift, expiredGift});

        final Set<Gift> gifts = underTest.findAvailableByRecipient(RECIPIENT_ID);

        assertThat(gifts, containsInAnyOrder(aGift(-1, created), aGift(-3, created)));
    }

    @Test
    public void giftsCanBeFoundByTheRecipient() {
        final Gift collectedGift = aGift(-4, created);
        collectedGift.setCollected(new DateTime());
        gigaSpace.writeMultiple(new Gift[]{aGift(-1, created), aGiftToSomeoneElse(-2, created), aGift(-3, created), collectedGift});

        final Set<Gift> gifts = underTest.findByRecipient(RECIPIENT_ID);

        assertThat(gifts, containsInAnyOrder(aGift(-1, created), aGift(-3, created), collectedGift));
    }

    @Test
    public void giftsAvailableForCollectionIsZeroWhenNoGiftsAreAvailable() {
        final Gift anExpiredGift = aGift(-1, created);
        anExpiredGift.setExpiry(new DateTime().minusDays(7));
        gigaSpace.writeMultiple(new Gift[]{anExpiredGift, aGiftToSomeoneElse(-2, created)});

        final int count = underTest.countAvailableForCollection(RECIPIENT_ID);

        assertThat(count, is(equalTo(0)));
    }

    @Test
    public void giftsAvailableForCollectionMatchesTheCountOfNonExpiredAndNonCollectedGifts() {
        final Gift anExpiredGift = aGift(-1, created);
        anExpiredGift.setExpiry(new DateTime().minusDays(7));
        final Gift aCollectedGift = aGift(-2, created);
        aCollectedGift.setCollected(new DateTime());
        gigaSpace.writeMultiple(new Gift[]{anExpiredGift, aCollectedGift, aGift(-3, created), aGift(-4, created)});

        final int count = underTest.countAvailableForCollection(RECIPIENT_ID);

        assertThat(count, is(equalTo(2)));
    }

    @Test
    public void giftsCollectedReturnsZeroWhenNoGiftsHaveBeenCollectedToday() {
        final Gift anOldCollectedGift = aGift(-1, created);
        anOldCollectedGift.setCollected(new DateTime().minusDays(2));
        gigaSpace.writeMultiple(new Gift[]{anOldCollectedGift});

        final int count = underTest.countCollectedOn(RECIPIENT_ID, new DateTime().withMillisOfDay(0));

        assertThat(count, is(equalTo(0)));
    }

    @Test
    public void giftsCollectedReturnsTheGiftsCollectedSinceTheGivenTime() {
        final Gift anOldCollectedGift = aGift(-1, created);
        anOldCollectedGift.setCollected(new DateTime().minusDays(2));
        final Gift aGiftCollectedToday = aGift(-2, created);
        aGiftCollectedToday.setCollected(new DateTime().withMillisOfDay(0).plusMinutes(2));
        final Gift aGiftCollectedInTheFuture = aGift(-3, created);
        aGiftCollectedInTheFuture.setCollected(new DateTime().withMillisOfDay(0).plusDays(2));
        gigaSpace.writeMultiple(new Gift[]{anOldCollectedGift, aGiftCollectedToday, aGiftCollectedInTheFuture});

        final int count = underTest.countCollectedOn(RECIPIENT_ID, new DateTime().withMillisOfDay(0));

        assertThat(count, is(equalTo(2)));
    }

    @Test
    public void cleaningUpOldGiftsRemovesGiftsOlderThanTheRetentionPeriod() {
        gigaSpace.writeMultiple(new Gift[]{aGift(-1, created), aGift(-2, new DateTime().minusHours(GIFT_RETENTION + 1))});

        underTest.cleanUpOldGifts(GIFT_RETENTION);

        final Gift[] gifts = gigaSpace.readMultiple(new Gift());
        assertThat(gifts, arrayContaining(aGift(-1, created)));
    }

    @Test
    public void publishReceivedWritesAPublishStatusRequestToTheSpace() {
        underTest.publishReceived(RECIPIENT_ID);

        final PublishStatusRequest requestFromSpace = gigaSpace.read(new PublishStatusRequest());
        assertThat(requestFromSpace.getRequestType(), is(equalTo(PublishStatusRequestType.GIFT_RECEIVED)));
        assertThat(requestFromSpace.getPlayerId(), is(equalTo(RECIPIENT_ID)));
        assertThat(requestFromSpace.getSpaceId(), is(not(nullValue())));
    }

    @Test
    public void publishCollectionStatusWritesAPublishStatusRequestWithArgumentsToTheSpace() {
        underTest.publishCollectionStatus(RECIPIENT_ID, aPlayerCollectionStatus());

        final PublishStatusRequestWithArguments expectedRequest = new PublishStatusRequestWithArguments(
                RECIPIENT_ID, PublishStatusRequestType.GIFTING_PLAYER_COLLECTION_STATUS,
                singletonMap(PublishStatusRequestArgument.PLAYER_COLLECTION_STATUS.name(), (Object) aPlayerCollectionStatus()));
        final PublishStatusRequestWithArguments requestFromSpace = gigaSpace.read(new PublishStatusRequestWithArguments());
        assertThat(requestFromSpace.getArguments(), is(equalTo(expectedRequest.getArguments())));
        assertThat(requestFromSpace.getRequestType(), is(equalTo(expectedRequest.getRequestType())));
        assertThat(requestFromSpace.getPlayerId(), is(equalTo(expectedRequest.getPlayerId())));
        assertThat(requestFromSpace.getSpaceId(), is(not(nullValue())));
    }

    @Test
    public void requestingASendOfAGiftWritesASendGiftRequestToTheSpace() {
        final BigDecimal sessionId = BigDecimal.valueOf(28090);
        final BigDecimal giftId = BigDecimal.valueOf(8888);

        underTest.requestSendGifts(SENDER_ID, sessionId, singletonMap(RECIPIENT_ID, giftId));

        final SendGiftRequest sendGiftRequest = gigaSpace.read(new SendGiftRequest());
        assertThat(sendGiftRequest, is(not(nullValue())));
        assertThat(sendGiftRequest.getSpaceId(), is(not(nullValue())));
        assertThat(sendGiftRequest.getSendingPlayerId(), is(equalTo(SENDER_ID)));
        assertThat(sendGiftRequest.getRecipientPlayerId(), is(equalTo(RECIPIENT_ID)));
        assertThat(sendGiftRequest.getGiftId(), is(equalTo(giftId)));
        assertThat(sendGiftRequest.getSessionId(), is(equalTo(sessionId)));
    }

    @Test
    public void requestingASendOfMultipleGiftsWritesSendGiftRequestsToTheSpace() {
        final BigDecimal sessionId = BigDecimal.valueOf(28090);
        final BigDecimal gift1Id = BigDecimal.valueOf(8888);
        final BigDecimal gift2Id = BigDecimal.valueOf(8889);

        final Map<BigDecimal, BigDecimal> recipientsToGiftIds = new HashMap<>();
        recipientsToGiftIds.put(RECIPIENT_ID, gift1Id);
        recipientsToGiftIds.put(ANOTHER_RECIPIENT_ID, gift2Id);
        underTest.requestSendGifts(SENDER_ID, sessionId, recipientsToGiftIds);

        final Set<SendGiftRequest> sendGiftRequests = newHashSet(gigaSpace.readMultiple(new SendGiftRequest()));
        assertThat(sendGiftRequests.size(), is(equalTo(2)));
        for (SendGiftRequest sendGiftRequest : sendGiftRequests) {
            assertThat(sendGiftRequest.getSpaceId(), is(not(nullValue())));
            sendGiftRequest.setSpaceId(null);
        }
        assertThat(sendGiftRequests, containsInAnyOrder(
                new SendGiftRequest(SENDER_ID, RECIPIENT_ID, gift1Id, sessionId),
                new SendGiftRequest(SENDER_ID, ANOTHER_RECIPIENT_ID, gift2Id, sessionId)));
    }

    @Test
    public void requestingAcknowledgementOfAGiftWritesAnAcknowledgeGiftRequestToTheSpace() {
        final BigDecimal giftId = BigDecimal.valueOf(8888);

        underTest.requestAcknowledgement(RECIPIENT_ID, newHashSet(giftId));

        final AcknowledgeGiftRequest acknowledgeGiftRequest = gigaSpace.read(new AcknowledgeGiftRequest());
        assertThat(acknowledgeGiftRequest, is(not(nullValue())));
        assertThat(acknowledgeGiftRequest.getRecipientPlayerId(), is(equalTo(RECIPIENT_ID)));
        assertThat(acknowledgeGiftRequest.getGiftId(), is(equalTo(giftId)));
    }

    @Test
    public void requestingAcknowledgementOfMultipleGiftsWritesAllAcknowledgeGiftRequestsToTheSpace() {
        final BigDecimal gift1Id = BigDecimal.valueOf(8888);
        final BigDecimal gift2Id = BigDecimal.valueOf(8889);
        final BigDecimal gift3Id = BigDecimal.valueOf(8810);

        underTest.requestAcknowledgement(RECIPIENT_ID, newHashSet(gift1Id, gift2Id, gift3Id));

        final AcknowledgeGiftRequest[] requests = gigaSpace.readMultiple(new AcknowledgeGiftRequest());
        assertThat(requests, is(not(nullValue())));
        assertThat(newHashSet(requests), containsInAnyOrder(new AcknowledgeGiftRequest(RECIPIENT_ID, gift1Id),
                new AcknowledgeGiftRequest(RECIPIENT_ID, gift2Id),
                new AcknowledgeGiftRequest(RECIPIENT_ID, gift3Id)));
    }

    @Test
    public void requestingCollectionOfAGiftWritesACollectionGiftRequestToTheSpace() {
        final BigDecimal giftId = BigDecimal.valueOf(8888);
        final BigDecimal sessionId = BigDecimal.valueOf(999);
        final BigDecimal winnings = BigDecimal.valueOf(7777);
        final CollectChoice collectChoice = CollectChoice.GAMBLE;

        underTest.requestCollection(RECIPIENT_ID, giftId, sessionId, winnings, collectChoice);

        final CollectGiftRequest collectGiftRequest = gigaSpace.read(new CollectGiftRequest());
        assertThat(collectGiftRequest, is(not(nullValue())));
        assertThat(collectGiftRequest.getRecipientPlayerId(), is(equalTo(RECIPIENT_ID)));
        assertThat(collectGiftRequest.getGiftId(), is(equalTo(giftId)));
        assertThat(collectGiftRequest.getSessionId(), is(equalTo(sessionId)));
        assertThat(collectGiftRequest.getWinnings(), is(equalTo(winnings)));
        assertThat(collectGiftRequest.getChoice(), is(equalTo(collectChoice)));
    }

    private PlayerCollectionStatus aPlayerCollectionStatus() {
        return new PlayerCollectionStatus(10, 20);
    }

    private Gift aGift(final long id,
                       final DateTime created) {
        return new Gift(BigDecimal.valueOf(id), SENDER_ID, RECIPIENT_ID, created, created.plusHours(EXPIRY_HOURS), null, false);
    }

    private Gift aGiftToSomeoneElse(final long id,
                                    final DateTime created) {
        return new Gift(BigDecimal.valueOf(id), SENDER_ID, ANOTHER_RECIPIENT_ID, created, created.plusHours(EXPIRY_HOURS), null, false);
    }
}
