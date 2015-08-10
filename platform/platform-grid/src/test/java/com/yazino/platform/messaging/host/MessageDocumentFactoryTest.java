package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.host.format.HostDocumentDeserialiser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

public class MessageDocumentFactoryTest {
    private static final long GAME_ID = 2L;
    private static final String COMMAND_UUID = "aCommand";
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(100);
    private static final ParameterisedMessage MESSAGE = new ParameterisedMessage("aMessage");

    @Mock
    private Destination destination;
    @Mock
    private DocumentDispatcher documentDispatcher;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void messageDocumentIsBuiltCorrectly() {
        final MessageHostDocument factory = new MessageHostDocument(
                GAME_ID, COMMAND_UUID, TABLE_ID, MESSAGE, destination);
        factory.send(documentDispatcher);

        final ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(destination).send(documentCaptor.capture(), eq(documentDispatcher));
        final Document document = documentCaptor.getValue();

        assertThat(document.getType(), is(equalTo(DocumentType.MESSAGE.getName())));

        final Map<String, Object> result = new HostDocumentDeserialiser(document).body();
        assertThat((Long) result.get("gameId"), is(equalTo(GAME_ID)));

        assertTrue(result.containsKey("changes"));
        assertFalse(result.containsKey("tableProperies"));
        assertThat((String) result.get("commandUUID"), is(equalTo(COMMAND_UUID)));
        assertThat((ParameterisedMessage) result.get("message"), is(equalTo(MESSAGE)));
    }
}
