package com.yazino.platform.service.statistic;

import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocumentDispatcher;
import com.yazino.platform.messaging.host.NewsEventHostDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(org.mockito.runners.MockitoJUnitRunner.class)
public class DocumentNewsEventPublisherTest {

    private static final String PARTNER_ID = "PARTNER_ID";
    private static final BigDecimal PLAYER_ID_1 = BigDecimal.valueOf(1);
    private static final BigDecimal PLAYER_ID_2 = BigDecimal.valueOf(2);

    @Mock
    private HostDocumentDispatcher hostDocumentDispatcher;
    @Mock
    private DestinationFactory destinationFactory;
    @Mock
    private Destination player1Destination;
    @Mock
    private Destination player2Destination;

    private NewsEvent newsEvent1;
    private NewsEvent newsEvent2;
    private DocumentNewsEventPublisher underTest;

    @Before
    public void setUp() {
        when(destinationFactory.player(PLAYER_ID_1)).thenReturn(player1Destination);
        when(destinationFactory.player(PLAYER_ID_2)).thenReturn(player2Destination);

        newsEvent1 = new NewsEvent.Builder(PLAYER_ID_1, new ParameterisedMessage("news1")).setImage("news1img").build();
        newsEvent2 = new NewsEvent.Builder(PLAYER_ID_2, new ParameterisedMessage("news2")).setImage("news2img").build();

        underTest = new DocumentNewsEventPublisher(hostDocumentDispatcher, PARTNER_ID, destinationFactory);
    }

    @Test
    public void sends_news_documents_using_dispatcher() {
        underTest.send(newsEvent1, newsEvent2);

        verify(hostDocumentDispatcher).send(new NewsEventHostDocument(PARTNER_ID, newsEvent1, player1Destination));
        verify(hostDocumentDispatcher).send(new NewsEventHostDocument(PARTNER_ID, newsEvent2, player2Destination));
    }

}
