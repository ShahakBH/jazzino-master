package com.yazino.platform.processor.chat;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.model.chat.*;
import net.sf.json.test.JSONAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

public class ChatChannelStatusDocumentFactoryTest {
    private static final String EXPECTED_JSON = "{\"id\":\"channel id\",\"type\":\"personal\",\"locationId\":\"location Id\","
            + "\"chatParticipants\":[{\"nickname\":\"nickname 1\",\"playerId\":1},{\"nickname\":\"nickname 2\","
            + "\"playerId\":10}],\"canAddParticipant\":true,\"allowedActions\":[\"AddParticipants\","
            + "\"Leave\",\"SendMessage\"]}";

    @Mock
    private DocumentDispatcher documentDispatcher;
    @Mock
    private Destination destination;

    private ChatChannelStatusHostDocument unit;
    private ChatChannelAggregate aggregate;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final ChatChannel channel = new ChatChannel(ChatChannelType.personal, "location Id");
        channel.setChannelId("channel id");
        final ChatParticipant participant1 = new ChatParticipant(BigDecimal.ONE, channel.getChannelId(), "nickname 1");
        final ChatParticipant participant2 = new ChatParticipant(BigDecimal.TEN, channel.getChannelId(), "nickname 2");
        aggregate = new ChatChannelAggregate(channel, participant1, participant2);

        unit = new ChatChannelStatusHostDocument(aggregate, destination);
    }

    @Test
    public void aChatChannelStatusDocumentIsBuiltFromTheAggregate() {
        unit.send(documentDispatcher);

        final ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(destination).send(documentCaptor.capture(), eq(documentDispatcher));

        final Document document = documentCaptor.getValue();

        assertThat(document.getType(), is(equalTo(DocumentType.CHAT_CHANNEL_STATUS.getName())));
        assertThat(document.getHeaders().size(), is(equalTo(2)));
        assertThat(document.getHeaders().get("channelId"), is(equalTo(aggregate.getId())));
        assertThat(document.getHeaders().get("locationId"), is(equalTo(aggregate.getLocationId())));

        JSONAssert.assertJsonEquals(EXPECTED_JSON, document.getBody());
    }

}
