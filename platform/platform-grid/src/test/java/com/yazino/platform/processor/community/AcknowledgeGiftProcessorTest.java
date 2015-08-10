package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.AcknowledgeGiftRequest;
import com.yazino.platform.model.community.Gift;
import com.yazino.platform.repository.community.GiftRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AcknowledgeGiftProcessorTest {
    private static final BigDecimal GIFT_ID = BigDecimal.valueOf(456);
    private static final BigDecimal RECIPIENT = BigDecimal.valueOf(123);

    @Mock
    private GiftRepository giftRepository;

    private AcknowledgeGiftProcessor underTest;

    @Before
    public void setUp() {
        when(giftRepository.findByRecipientAndId(RECIPIENT, GIFT_ID)).thenReturn(aGift());
        when(giftRepository.lockByRecipientAndId(RECIPIENT, GIFT_ID)).thenReturn(aGift());

        underTest = new AcknowledgeGiftProcessor(giftRepository);
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullGiftRepository() {
        new AcknowledgeGiftProcessor(null);
    }

    @Test
    public void theTemplateIsAnEmptyRequest() {
        assertThat(underTest.template(), is(equalTo(new AcknowledgeGiftRequest())));
    }

    @Test
    public void aNullRequestIsIgnored() {
        underTest.process(null);

        verifyZeroInteractions(giftRepository);
    }

    @Test
    public void aNonExistentGiftIsIgnored() {
        reset(giftRepository);
        underTest.process(aRequest());

        verify(giftRepository).findByRecipientAndId(RECIPIENT, GIFT_ID);
        verifyNoMoreInteractions(giftRepository);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void aLockedGiftPropagatesAConcurrentModifiedException() {
        when(giftRepository.lockByRecipientAndId(RECIPIENT, GIFT_ID)).thenThrow(new ConcurrentModificationException("aTestException"));

        underTest.process(aRequest());
    }

    @Test
    public void aGiftForTheIntendedRecipientIsAcknowledgedAndSaved() {
        underTest.process(aRequest());

        final Gift expectedGift = aGift();
        expectedGift.setAcknowledged(true);
        verify(giftRepository).save(expectedGift);
    }

    @Test
    public void anExceptionDuringGiftPersistenceIsNotPropagated() {
        doThrow(new RuntimeException("aTestException")).when(giftRepository).save(aGift());

        underTest.process(aRequest());
    }

    private AcknowledgeGiftRequest aRequest() {
        return new AcknowledgeGiftRequest(RECIPIENT, GIFT_ID);
    }

    private Gift aGift() {
        final DateTime now = new DateTime(400000000000L);
        return new Gift(GIFT_ID, BigDecimal.valueOf(9999), RECIPIENT, now, now.plusHours(16), null, false);
    }

}
