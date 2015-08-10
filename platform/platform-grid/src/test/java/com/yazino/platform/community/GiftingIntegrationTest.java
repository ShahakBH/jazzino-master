package com.yazino.platform.community;

import com.yazino.platform.gifting.CollectChoice;
import com.yazino.platform.gifting.GiftCollectionFailure;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Set;

import static com.yazino.platform.gifting.Giftable.GIFTABLE;
import static com.yazino.platform.gifting.Giftable.GIFTED;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(MockitoJUnitRunner.class)
public class GiftingIntegrationTest {

    public static final BigDecimal BOB = valueOf(0);
    public static final BigDecimal JIM = valueOf(1);
    public static final BigDecimal FREDDY = valueOf(2);
    private GiftingUnderTest sut;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis());
        sut = new GiftingUnderTest();
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void gifteeSelectionScreenShouldShowGiftingStatusOfPlayers() {
        sut.createPlayersWhoAreFriends(BOB, JIM, FREDDY);
        sut.playerGivesGiftTo(BOB, JIM);

        sut.playerGoesToHisGiftingStatusPage(BOB);

        assertThat(sut.playersGiftingStatusShows(JIM), is(GIFTED));
        assertThat(sut.playersGiftingStatusShows(FREDDY), is(GIFTABLE));

        sut.playerGoesToHisGiftingStatusPage(FREDDY);
        assertThat(sut.playersGiftingStatusShows(JIM), is(GIFTABLE));
        assertThat(sut.playersGiftingStatusShows(BOB), is(GIFTABLE));

        sut.playerGoesToHisGiftingStatusPage(JIM);
        assertThat(sut.playersGiftingStatusShows(FREDDY), is(GIFTABLE));
        assertThat(sut.playersGiftingStatusShows(BOB), is(GIFTABLE));
    }

    @Test
    public void giveGiftShouldPushMessageToClient() {
        sut.createPlayersWhoAreFriends(BOB, JIM);

        sut.playerGivesGiftTo(BOB, JIM);
        assertThat(sut.playerGiftReceivedMessageFrom(JIM), is(true));
    }

    @Test
    public void giveGiftShouldPushPlayerCollectionStatusMessagesToEachReceiver() {
        sut.createPlayersWhoAreFriends(BOB, JIM, FREDDY);

        sut.playerGivesGiftTo(BOB, JIM, FREDDY);

        assertThat(sut.playerReceivedCollectionStatusPush(JIM), is(true));
        assertThat(sut.playerReceivedCollectionStatusPush(FREDDY), is(true));
    }

    @Test
    public void sendGiftShouldRecordMessageInBusinessIntelligence() {
        sut.createPlayersWhoAreFriends(BOB, JIM);
        final Set<BigDecimal> giftId = sut.playerGivesGiftTo(BOB, JIM);

        assertThat(giftId, is(not(empty())));
        assertThat(sut.biRecordsGivenGiftisGiven(giftId.iterator().next()), is(true));
    }

    @Test
    public void whenAPlayerCollectsAGiftTheNumberOfGiftCollectionsShouldBePushedToThatPlayer() throws GiftCollectionFailure {
        sut.createPlayersWhoAreFriends(BOB, JIM);
        final Set<BigDecimal> giftId = sut.playerGivesGiftTo(BOB, JIM);
        assertThat(giftId, is(not(empty())));
        sut.playerCollectsGift(JIM, giftId.iterator().next(), CollectChoice.GAMBLE);
        assertThat(sut.playerReceivedCollectionStatusPush(JIM, 24, 0), is(true));
    }

    @Test
    public void whenPlayerCollectsGiftAGiftCollectedEventShouldBeSent() throws GiftCollectionFailure {
        sut.createPlayersWhoAreFriends(BOB, JIM);
        final Set<BigDecimal> giftIds = sut.playerGivesGiftTo(BOB, JIM);
        assertThat(giftIds, is(not(empty())));
        BigDecimal giftId = giftIds.iterator().next();
        sut.playerCollectsGift(JIM, giftId, CollectChoice.GAMBLE);
        assertThat(sut.giftCollectedEventSent(giftId), is(true));
    }
}
