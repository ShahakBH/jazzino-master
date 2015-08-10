package com.yazino.platform.messaging.host.format;

import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.messaging.host.ObservableStatusStub;
import org.junit.Test;
import com.yazino.game.api.Command;
import com.yazino.game.api.ObservableChange;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class HostDocumentBodyFormatterTest {

    private static final long GAME_ID = 2L;
    private static final String COMMAND_UUID = "aCommand";
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(100);
    private static final Command COMMAND = new Command(null, null, null, COMMAND_UUID, null);

    @Test
    public void invalidJsonPropertiesAreEscapedWithinTheChanges() {
        // this is an example that failed in production
        final ObservableChange aChange = new ObservableChange(
                2L, new String[]{"Players", "8", "11868478", "\"Sabina\""});

        final String body = new HostDocumentBodyFormatter(GAME_ID, COMMAND_UUID)
                .withChanges(anObservableDocumentContext(aChange))
                .build();

        assertThat(body, containsString(
                "\"changes\":\"2\\t1\\n0\\n1\\n\\nNoTimeout\\n2\\tPlayers\\t8\\t11868478\\t\\\"Sabina\\\"\""));
    }

    private ObservableDocumentContext anObservableDocumentContext(final ObservableChange aChange) {
        return new ObservableDocumentContext.Builder(
                TABLE_ID, GAME_ID, new ObservableStatusStub(), 1L, 1L)
                .withCommand(COMMAND)
                .withIsAPlayer(true)
                .withTableProperties(tableProperties())
                .withLastGameChanges(asList(aChange))
                .withPlayerBalance(BigDecimal.ZERO)
                .build();
    }

    private Map<String, String> tableProperties() {
        final Map<String, String> tableProperties = new HashMap<String, String>();
        tableProperties.put("foo", "bar");
        tableProperties.put("foo2", "bar2");
        return tableProperties;
    }

}
