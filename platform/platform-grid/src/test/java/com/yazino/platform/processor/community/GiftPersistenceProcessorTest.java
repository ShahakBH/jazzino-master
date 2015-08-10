package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.Gift;
import com.yazino.platform.model.community.GiftPersistenceRequest;
import com.yazino.platform.persistence.community.GiftDAO;
import com.yazino.platform.repository.community.GiftRepository;
import org.joda.time.DateTime;
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
public class GiftPersistenceProcessorTest {
    private static final BigDecimal GIFT_ID = BigDecimal.valueOf(890);
    private static final BigDecimal RECIPIENT_ID = BigDecimal.valueOf(123);
    @Mock
    private GiftRepository giftRepository;
    @Mock
    private GiftDAO giftDao;

    private GiftPersistenceProcessor underTest;

    @Before
    public void setUp() {
        underTest = new GiftPersistenceProcessor(giftDao, giftRepository);
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullDao() {
        new GiftPersistenceProcessor(null, giftRepository);
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullRepository() {
        new GiftPersistenceProcessor(giftDao, null);
    }

    @Test
    public void templateIsAnEmptyPersistenceRequest() {
        assertThat(underTest.template(), is(equalTo(new GiftPersistenceRequest())));
    }

    @Test
    public void aNullRequestIsIgnored() {
        underTest.process(null);

        verifyZeroInteractions(giftDao, giftRepository);
    }

    @Test
    public void aNonExistentGiftIsIgnored() {
        underTest.process(new GiftPersistenceRequest(RECIPIENT_ID, GIFT_ID));

        verify(giftRepository).findByRecipientAndId(RECIPIENT_ID, GIFT_ID);
        verifyZeroInteractions(giftDao);
    }

    @Test
    public void anExistingGiftIsSaved() {
        when(giftRepository.findByRecipientAndId(RECIPIENT_ID, GIFT_ID)).thenReturn(aGift());

        underTest.process(new GiftPersistenceRequest(RECIPIENT_ID, GIFT_ID));

        verify(giftDao).save(aGift());
    }

    @Test
    public void exceptionsFromTheDAOAreNotPropagated() {
        when(giftRepository.findByRecipientAndId(RECIPIENT_ID, GIFT_ID)).thenReturn(aGift());
        doThrow(new RuntimeException("aTestException")).when(giftDao).save(aGift());

        underTest.process(new GiftPersistenceRequest(RECIPIENT_ID, GIFT_ID));
    }

    private Gift aGift() {
        final DateTime created = new DateTime(489743289743L);
        return new Gift(BigDecimal.valueOf(-1), BigDecimal.TEN, RECIPIENT_ID, created, created.plusHours(2), null, false);
    }
}
