package strata.server.worker.event.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.GiftSentEvent;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.event.persistence.PostgresGiftSentEventDao;

import java.math.BigDecimal;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GiftSentEventConsumerTest {
    private static final BigDecimal SESSION_ID = new BigDecimal(1231248234l);
    private static final BigDecimal GIFT_ID = BigDecimal.valueOf(23);

    @Mock
    private PostgresGiftSentEventDao postgresGiftSentEventDao;

    private GiftSentEventConsumer underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new GiftSentEventConsumer(yazinoConfiguration, postgresGiftSentEventDao);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(1000l);
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Mock
    private YazinoConfiguration yazinoConfiguration;

    @Test
    public void shouldSaveUsingRedshiftWhenEnabled() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        final GiftSentEvent message = new GiftSentEvent(GIFT_ID, BigDecimal.ONE, BigDecimal.TEN, new DateTime().plusHours(1), new DateTime(), SESSION_ID);
        underTest.handle(message);
        underTest.consumerCommitting();

        verify(postgresGiftSentEventDao).saveAll(newArrayList(message));
    }

    @Test
    public void shouldNotSaveWhenRedshiftIsNotEnabled() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(false);

        final GiftSentEvent message = new GiftSentEvent(GIFT_ID, BigDecimal.ONE, BigDecimal.TEN, new DateTime().plusHours(1), new DateTime(), SESSION_ID);
        underTest.handle(message);
        underTest.consumerCommitting();

        verifyZeroInteractions(postgresGiftSentEventDao);
    }

    @Test
    public void shouldSaveMultipleItems() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        final GiftSentEvent messageOne = new GiftSentEvent(GIFT_ID, BigDecimal.ONE, BigDecimal.TEN, new DateTime().plusHours(1), new DateTime(), SESSION_ID);
        final GiftSentEvent messageTwo = new GiftSentEvent(BigDecimal.valueOf(67l), BigDecimal.ONE, BigDecimal.TEN, new DateTime().plusHours(1), new DateTime(), SESSION_ID);
        underTest.handle(messageOne);
        underTest.handle(messageTwo);
        underTest.consumerCommitting();

        verify(postgresGiftSentEventDao).saveAll(newArrayList(messageOne, messageTwo));
    }

    @Test
    public void shouldClearBatchAfterCommit() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        final GiftSentEvent messageOne = new GiftSentEvent(GIFT_ID, BigDecimal.ONE, BigDecimal.TEN, new DateTime().plusHours(1), new DateTime(), SESSION_ID);
        final GiftSentEvent messageTwo = new GiftSentEvent(BigDecimal.valueOf(67l), BigDecimal.ONE, BigDecimal.TEN, new DateTime().plusHours(1), new DateTime(), SESSION_ID);

        final GiftSentEvent messageThree = new GiftSentEvent(BigDecimal.valueOf(231l), BigDecimal.ONE, BigDecimal.TEN, new DateTime().plusHours(3), new DateTime(), SESSION_ID);
        final GiftSentEvent messageFour = new GiftSentEvent(BigDecimal.valueOf(671l), BigDecimal.ONE, BigDecimal.TEN, new DateTime().plusHours(4), new DateTime(), SESSION_ID);

        underTest.handle(messageOne);
        underTest.handle(messageTwo);
        underTest.consumerCommitting();

        underTest.handle(messageThree);
        underTest.handle(messageFour);
        underTest.consumerCommitting();

        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);

        verify(postgresGiftSentEventDao, times(2)).saveAll(argument.capture());

        assertThat((List<GiftSentEvent>) argument.getAllValues().get(0), hasItems(messageOne, messageTwo));
        assertThat((List<GiftSentEvent>) argument.getAllValues().get(1), hasItems(messageThree, messageFour));
    }
}
