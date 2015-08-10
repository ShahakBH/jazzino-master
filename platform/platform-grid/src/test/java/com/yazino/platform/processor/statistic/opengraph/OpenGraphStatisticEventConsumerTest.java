package com.yazino.platform.processor.statistic.opengraph;

import com.google.common.base.Function;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.opengraph.OpenGraphAction;
import com.yazino.platform.opengraph.OpenGraphActionMessage;
import com.yazino.platform.opengraph.OpenGraphObject;
import com.yazino.platform.playerstatistic.StatisticEvent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class OpenGraphStatisticEventConsumerTest {

    private OpenGraphStatisticEventConsumer underTest;
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final String GAME_TYPE = "SLOTS";
    private static final StatisticEvent EVENT_1 = new StatisticEvent("event1");
    private static final StatisticEvent EVENT_2 = new StatisticEvent("event2");
    private static final OpenGraphAction ACTION_1 = new OpenGraphAction("action1", new OpenGraphObject("object1", "http://sample.url/object1"));
    private static final OpenGraphAction ACTION_2 = new OpenGraphAction("action2", new OpenGraphObject("object2", "http://sample.url/object2"));
    private static final String PUBLISHING_ENABLED = "true";
    private static final String PUBLISHING_DISABLED = "false";

    private QueuePublishingService<OpenGraphActionMessage> openGraphActionQueuePublishingService;
    private Function<StatisticEvent, OpenGraphAction> transformer;

    @Before
    public void setUp() {
        openGraphActionQueuePublishingService = mock(QueuePublishingService.class);
        transformer = mock(Function.class);
        underTest = new OpenGraphStatisticEventConsumer(transformer, openGraphActionQueuePublishingService, PUBLISHING_ENABLED);
    }

    @Test
    public void shouldTransformStatisticEventToOpenGraphActionAndPublishToQueue() {
        when(transformer.apply(EVENT_1)).thenReturn(ACTION_1);
        when(transformer.apply(EVENT_2)).thenReturn(ACTION_2);

        underTest.processEvents(PLAYER_ID, GAME_TYPE, asList(EVENT_1, EVENT_2));

        verify(openGraphActionQueuePublishingService).send(argThat(is(messageLike(new OpenGraphActionMessage(PLAYER_ID.toBigInteger(), GAME_TYPE, ACTION_1)))));
        verify(openGraphActionQueuePublishingService).send(argThat(is(messageLike(new OpenGraphActionMessage(PLAYER_ID.toBigInteger(), GAME_TYPE, ACTION_2)))));
    }

    private Matcher<OpenGraphActionMessage> messageLike(OpenGraphActionMessage expectedMessage) {
        return new MessageLike(expectedMessage);
    }

    @Test
    public void shouldDoNothingIfStatisticEventDoesNotTransformToOpenGraphAction() {
        when(transformer.apply(EVENT_1)).thenReturn(null);

        underTest.processEvents(PLAYER_ID, GAME_TYPE, asList(EVENT_1));

        verify(openGraphActionQueuePublishingService, never()).send(any(OpenGraphActionMessage.class));
    }

    @Test
    public void shouldNotPropagateExceptions() {
        when(transformer.apply(EVENT_1)).thenThrow(new RuntimeException("sample exception"));

        underTest.processEvents(PLAYER_ID, GAME_TYPE, asList(EVENT_1));
    }

    @Test
    public void shouldNotStopProcessingWhenExceptionOccurs() {
        when(transformer.apply(EVENT_1)).thenThrow(new RuntimeException("sample exception"));
        when(transformer.apply(EVENT_2)).thenReturn(ACTION_2);

        underTest.processEvents(PLAYER_ID, GAME_TYPE, asList(EVENT_1, EVENT_2));

        verify(openGraphActionQueuePublishingService).send(argThat(is(equalTo(new OpenGraphActionMessage(PLAYER_ID.toBigInteger(), GAME_TYPE, ACTION_2)))));
    }

    @Test
    public void shouldIgnoreEventsWhenOpenGraphPublishingEnabledPropertyIsFalse() {
        when(transformer.apply(EVENT_1)).thenReturn(ACTION_1);

        underTest = new OpenGraphStatisticEventConsumer(
                transformer, openGraphActionQueuePublishingService, PUBLISHING_DISABLED);

        underTest.processEvents(PLAYER_ID, GAME_TYPE, asList(EVENT_1));

        verify(openGraphActionQueuePublishingService, never()).send(any(OpenGraphActionMessage.class));
    }

    private static class MessageLike extends TypeSafeDiagnosingMatcher<OpenGraphActionMessage> {

        private OpenGraphActionMessage expectedMessage;

        public MessageLike(OpenGraphActionMessage expectedMessage) {
            this.expectedMessage = expectedMessage;
        }

        @Override
        protected boolean matchesSafely(OpenGraphActionMessage item, Description mismatchDescription) {
            return ObjectUtils.nullSafeEquals(item.getGameType(), expectedMessage.getGameType()) &&
                    ObjectUtils.nullSafeEquals(item.getPlayerId(), expectedMessage.getPlayerId()) &&
                    ObjectUtils.nullSafeEquals(item.getAction(), expectedMessage.getAction());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Message like '" + expectedMessage + "'.");
        }
    }
}
