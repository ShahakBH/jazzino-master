package com.yazino.platform.processor.community;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.audit.GiftAuditingService;
import com.yazino.platform.gifting.CollectChoice;
import com.yazino.platform.gifting.PlayerCollectionStatus;
import com.yazino.platform.model.community.CollectGiftRequest;
import com.yazino.platform.model.community.Gift;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.repository.community.GiftRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.community.GiftProperties;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CollectGiftProcessorTest {
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(600);
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(400);
    private static final BigDecimal GIFT_ID = BigDecimal.valueOf(300);
    private static final BigDecimal RECIPIENT = BigDecimal.valueOf(200);
    private static final BigDecimal WINNINGS = BigDecimal.valueOf(1200);
    private static final CollectChoice CHOICE = CollectChoice.GAMBLE;
    private static final int EXPIRY_HOURS = 13;
    private static final int REMAINING_COLLECTIONS = 11;
    private static final int COLLECTED_TODAY = 4;
    private static final int GIFTS_WAITING = 4;

    @Mock
    private GiftRepository giftRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private GiftAuditingService giftAuditingService;
    @Mock
    private InternalWalletService internalWalletService;
    @Mock
    private GiftProperties giftProperties;

    private CollectGiftProcessor underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(458345894389547L);

        final DateTime startOfDay = new DateTime().withMillisOfDay(0);

        when(giftProperties.expiryTimeInHours()).thenReturn(EXPIRY_HOURS);
        when(giftProperties.remainingGiftCollections(COLLECTED_TODAY)).thenReturn(REMAINING_COLLECTIONS);
        when(giftProperties.startOfGiftPeriod()).thenReturn(startOfDay);

        when(giftRepository.countCollectedOn(RECIPIENT, startOfDay)).thenReturn(COLLECTED_TODAY);
        when(giftRepository.countAvailableForCollection(RECIPIENT)).thenReturn(GIFTS_WAITING);
        when(giftRepository.findByRecipientAndId(RECIPIENT, GIFT_ID)).thenReturn(aGift());
        when(giftRepository.lockByRecipientAndId(RECIPIENT, GIFT_ID)).thenReturn(aGift());

        final Player player = new Player(RECIPIENT);
        player.setAccountId(ACCOUNT_ID);
        when(playerRepository.findById(RECIPIENT)).thenReturn(player);

        underTest = new CollectGiftProcessor(giftRepository, playerRepository, giftAuditingService, internalWalletService, giftProperties);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullGiftRepository() {
        new CollectGiftProcessor(null, playerRepository, giftAuditingService, internalWalletService, giftProperties);
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullPlayerRepository() {
        new CollectGiftProcessor(giftRepository, null, giftAuditingService, internalWalletService, giftProperties);
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullGiftAuditingService() {
        new CollectGiftProcessor(giftRepository, playerRepository, null, internalWalletService, giftProperties);
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullInternalWalletService() {
        new CollectGiftProcessor(giftRepository, playerRepository, giftAuditingService, null, giftProperties);
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullGiftProperties() {
        new CollectGiftProcessor(giftRepository, playerRepository, giftAuditingService, internalWalletService, null);
    }

    @Test
    public void templateIsAnEmptyRequest() {
        assertThat(underTest.template(), is(equalTo(new CollectGiftRequest())));
    }

    @Test
    public void aNullRequestIsIgnored() {
        underTest.process(null);

        verifyZeroInteractions(giftRepository, playerRepository, giftAuditingService, internalWalletService, giftProperties);
    }

    @Test
    public void aNonExistentGiftIsIgnored() {
        reset(giftRepository);

        underTest.process(aRequest());

        verify(giftRepository).findByRecipientAndId(RECIPIENT, GIFT_ID);
        verifyNoMoreInteractions(giftRepository);
        verifyZeroInteractions(playerRepository, giftAuditingService, internalWalletService, giftProperties);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void aLockedGiftPropagatesAConcurrentModificationException() {
        when(giftRepository.lockByRecipientAndId(RECIPIENT, GIFT_ID)).thenThrow(new ConcurrentModificationException("aTestException"));

        underTest.process(aRequest());
    }

    @Test
    public void theGiftIsMarkedAsCollected() {
        underTest.process(aRequest());

        final Gift expectedGift = aGift();
        expectedGift.setCollected(new DateTime());
        verify(giftRepository).save(expectedGift);
    }

    @Test
    public void theGiftCollectionIsAudited() {
        underTest.process(aRequest());

        verify(giftAuditingService).auditGiftCollected(GIFT_ID, CHOICE, WINNINGS, SESSION_ID, new DateTime());
    }

    @Test
    public void theGiftCollectionStatusMessageIsPublished() {
        underTest.process(aRequest());

        verify(giftRepository).publishCollectionStatus(RECIPIENT, new PlayerCollectionStatus(REMAINING_COLLECTIONS, GIFTS_WAITING));
    }

    @Test
    public void theRecipientIsCreditedWithTheirWinnings() throws WalletServiceException {
        underTest.process(aRequest());

        verify(internalWalletService).postTransaction(ACCOUNT_ID, WINNINGS, "Collected Gift", CHOICE.name(),
                transactionContext().withSessionId(SESSION_ID).build());
    }

    @Test
    public void theRecipientIsNotCreditedWithTheirWinningsWhenZero() throws WalletServiceException {
        final CollectGiftRequest request = aRequest();
        request.setWinnings(BigDecimal.ZERO);

        underTest.process(request);

        verify(internalWalletService, never()).postTransaction(ACCOUNT_ID, BigDecimal.ZERO, "Collected Gift", CHOICE.name(),
                transactionContext().withSessionId(SESSION_ID).build());
    }

    @Test
    public void theGiftIsNotMarkedAsCollectedIfThePlayerDoesNotExist() throws WalletServiceException {
        reset(playerRepository);

        underTest.process(aRequest());

        verify(giftRepository, never()).save(any(Gift.class));
    }

    @Test
    public void theGiftIsNotMarkedAsCollectedIfThePlayerCannotBeCredited() throws WalletServiceException {
        doThrow(new WalletServiceException("aTestException")).when(internalWalletService)
                .postTransaction(ACCOUNT_ID, WINNINGS, "Collected Gift", CHOICE.name(), transactionContext().withSessionId(SESSION_ID).build());

        underTest.process(aRequest());

        verify(giftRepository, never()).save(any(Gift.class));
    }

    private Gift aGift() {
        return new Gift(GIFT_ID, BigDecimal.valueOf(4444), RECIPIENT, new DateTime(), new DateTime().plusHours(EXPIRY_HOURS), null, false);
    }

    private CollectGiftRequest aRequest() {
        return new CollectGiftRequest(RECIPIENT, GIFT_ID, SESSION_ID, WINNINGS, CHOICE);
    }
}
