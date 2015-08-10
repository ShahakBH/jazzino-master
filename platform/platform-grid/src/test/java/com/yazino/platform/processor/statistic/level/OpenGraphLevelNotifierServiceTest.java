package com.yazino.platform.processor.statistic.level;

import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.opengraph.OpenGraphActionMessage;
import com.yazino.platform.service.statistic.OpenGraphLevelNotifierService;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static java.math.BigDecimal.TEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class OpenGraphLevelNotifierServiceTest {
    public static final BigDecimal PLAYER_ID = TEN;
    private QueuePublishingService<OpenGraphActionMessage> publisher = Mockito.mock(QueuePublishingService.class);
    private OpenGraphLevelNotifierService underTest;
    private OpenGraphLevelPrefixes openGraphLevelPrefixes;

    @Before
    public void setUp() throws Exception {
        openGraphLevelPrefixes = mock(OpenGraphLevelPrefixes.class);
        underTest = new OpenGraphLevelNotifierService(publisher, openGraphLevelPrefixes);
    }

    @Test
    public void notifierShouldSendMessageOnPublisher() {
        when(openGraphLevelPrefixes.getLevelPrefix("SLOTS")).thenReturn("wd");
        ArgumentCaptor<OpenGraphActionMessage> messageCaptor = ArgumentCaptor.forClass(OpenGraphActionMessage.class);

        underTest.publishNewLevel(PLAYER_ID, "SLOTS", 2);

        verify(publisher).send(messageCaptor.capture());
        OpenGraphActionMessage message = messageCaptor.getValue();
        assertThat(message.getAction().getName(), Is.is(CoreMatchers.equalTo("gain")));
        assertThat(message.getAction().getObject().getId(), Is.is(CoreMatchers.equalTo("wd_level_2")));
        assertThat(message.getAction().getObject().getType(), Is.is(CoreMatchers.equalTo("level")));
    }

    @Test
    public void notifierShouldNotSendMessageOnPublisherOnUnknownPrefix() {
        when(openGraphLevelPrefixes.getLevelPrefix("GAME_WITHOUT_PREFIX")).thenReturn(null);

        underTest.publishNewLevel(PLAYER_ID, "GAME_WITHOUT_PREFIX", 2);

        verify(publisher, never()).send(Matchers.<OpenGraphActionMessage>anyObject());
    }


}
