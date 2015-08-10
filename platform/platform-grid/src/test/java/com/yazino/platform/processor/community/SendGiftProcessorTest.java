package com.yazino.platform.processor.community;

import com.yazino.platform.audit.GiftAuditingService;
import com.yazino.platform.gifting.PlayerCollectionStatus;
import com.yazino.platform.model.community.Gift;
import com.yazino.platform.model.community.SendGiftRequest;
import com.yazino.platform.repository.community.GiftRepository;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SendGiftProcessorTest {
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(400);
    private static final BigDecimal GIFT_ID = BigDecimal.valueOf(300);
    private static final BigDecimal RECIPIENT = BigDecimal.valueOf(200);
    private static final BigDecimal SENDER = BigDecimal.valueOf(100);
    private static final int EXPIRY_HOURS = 13;
    private static final int COLLECTED_TODAY = 4;
    private static final int REMAINING_COLLECTIONS = 11;
    private static final int GIFTS_WAITING = 4;

    @Mock
    private GiftRepository giftRepository;
    @Mock
    private GiftAuditingService giftAuditingService;
    @Mock
    private GiftProperties giftProperties;

    private SendGiftProcessor underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(4524593872834L);

        final DateTime startOfDay = new DateTime().withMillisOfDay(0);

        when(giftProperties.expiryTimeInHours()).thenReturn(EXPIRY_HOURS);
        when(giftProperties.remainingGiftCollections(COLLECTED_TODAY)).thenReturn(REMAINING_COLLECTIONS);
        when(giftProperties.startOfGiftPeriod()).thenReturn(startOfDay);

        when(giftRepository.countCollectedOn(RECIPIENT, startOfDay)).thenReturn(COLLECTED_TODAY);
        when(giftRepository.countAvailableForCollection(RECIPIENT)).thenReturn(GIFTS_WAITING);

        underTest = new SendGiftProcessor(giftAuditingService, giftRepository, giftProperties);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullAuditingService() {
        new SendGiftProcessor(null, giftRepository, giftProperties);
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullGiftRepository() {
        new SendGiftProcessor(giftAuditingService, null, giftProperties);
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullGiftingProperties() {
        new SendGiftProcessor(giftAuditingService, giftRepository, null);
    }

    @Test
    public void templateIsAnEmptySendGiftRequest() {
        assertThat(underTest.template(), is(equalTo(new SendGiftRequest())));
    }

    @Test
    public void aNullRequestIsIgnored() {
        underTest.process(null);

        verifyZeroInteractions(giftAuditingService, giftRepository, giftProperties);
    }

    @Test
    public void aNewGiftIsSavedToTheRepository() {
        underTest.process(aRequest());

        verify(giftRepository).save(aGift());
    }

    @Test
    public void aGiftReceivedMessageIsPublished() {
        underTest.process(aRequest());

        verify(giftRepository).publishReceived(RECIPIENT);
    }

    @Test
    public void theGiftCollectionStatusMessageIsPublished() {
        underTest.process(aRequest());

        verify(giftRepository).publishCollectionStatus(RECIPIENT, new PlayerCollectionStatus(REMAINING_COLLECTIONS, GIFTS_WAITING));
    }

    @Test
    public void theGiftSendIsAudited() {
        underTest.process(aRequest());

        verify(giftAuditingService).auditGiftSent(GIFT_ID, SENDER, RECIPIENT, new DateTime().plusHours(EXPIRY_HOURS), new DateTime(), SESSION_ID);
    }

    @Test
    public void anExceptionDuringGiftCreationIsNotPropagated() {
        doThrow(new RuntimeException("aTestException")).when(giftRepository).save(aGift());

        underTest.process(aRequest());
    }

    private Gift aGift() {
        return new Gift(GIFT_ID, SENDER, RECIPIENT, new DateTime(), new DateTime().plusHours(EXPIRY_HOURS), null, false);
    }

    private SendGiftRequest aRequest() {
        return new SendGiftRequest(SENDER, RECIPIENT, GIFT_ID, SESSION_ID);
    }
}
