package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.*;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.host.format.HostDocumentDeserialiser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.Command;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Boolean.parseBoolean;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class InitialGameStatusDocumentFactoryTest {
    private static final long GAME_ID = 2L;
    private static final String COMMAND_UUID = "aCommand";
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(100);

    @Mock
    private Destination destination;
    @Mock
    private DocumentDispatcher documentDispatcher;

    private Command command;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        command = new Command(null, null, null, COMMAND_UUID, null);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void initialGameStatusDocumentIsBuiltCorrectly() {
        final ObservableDocumentContext context = new ObservableDocumentContext.Builder(
                TABLE_ID, GAME_ID, new ObservableStatusStub(), 1L, 1L)
                .withCommand(command)
                .withIsAPlayer(true)
                .withTableProperties(tableProperties())
                .build();

        final InitialGameStatusHostDocument factory = new InitialGameStatusHostDocument(context, destination);

        factory.send(documentDispatcher);

        final ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(destination).send(documentCaptor.capture(), eq(documentDispatcher));
        final Document document = documentCaptor.getValue();

        assertThat(document.getType(), is(equalTo(DocumentType.INITIAL_GAME_STATUS.getName())));
        assertThat(parseBoolean(document.getHeaders().get(DocumentHeaderType.IS_A_PLAYER.getHeader())), is(true));

        final Map<String, Object> result = new HostDocumentDeserialiser(document).body();

        assertThat((Long) result.get("gameId"), is(equalTo(GAME_ID)));

        assertThat((Map<String, String>) result.get("tableProperies"), is(equalTo(tableProperties())));
        assertThat((String) result.get("commandUUID"), is(equalTo(COMMAND_UUID)));
        assertTrue(result.containsKey("changes"));

        assertFalse(result.containsKey("message"));
    }

    private Map<String, String> tableProperties() {
        final Map<String, String> tableProperties = new HashMap<String, String>();
        tableProperties.put("foo", "bar");
        tableProperties.put("foo2", "bar2");
        return tableProperties;
    }

}
