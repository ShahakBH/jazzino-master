package com.yazino.platform.messaging.destination;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

public class PlayersDestinationTest {
    private static final BigDecimal PLAYER_2 = BigDecimal.valueOf(20);
    private static final BigDecimal PLAYER_1 = BigDecimal.valueOf(10);

    @Mock
    private DocumentDispatcher documentDispatcher;

    private PlayersDestination unit;
    private Document document;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        unit = new PlayersDestination(set(PLAYER_1, PLAYER_2));

        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("aTestHeader", "aTestValue");
        document = new Document("TEST_MSG", "aBody", headers);
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void destinationShouldRejectANullList() {
        new PlayersDestination(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void destinationShouldRejectAnEmptyList() {
        new PlayersDestination(Collections.<BigDecimal>emptySet());
    }

    @Test
    public void documentShouldBeDispatchedToAllPlayers() {
        unit.send(document, documentDispatcher);

        verify(documentDispatcher).dispatch(document, set(PLAYER_1, PLAYER_2));
    }

    @Test
    public void destinationsAreEqualIfThePlayerSetIsEqual() {
        final PlayersDestination destination1 = new PlayersDestination(set(PLAYER_1, PLAYER_2));
        final PlayersDestination destination2 = new PlayersDestination(set(PLAYER_1, PLAYER_2));

        assertThat(destination1, is(equalTo(destination2)));
        assertThat(destination1.hashCode(), is(equalTo(destination2.hashCode())));
    }

    @Test
    public void destinationsAreNotEqualIfThePlayerSetsDiffer() {
        final PlayersDestination destination1 = new PlayersDestination(set(PLAYER_1, PLAYER_2));
        final PlayersDestination destination2 = new PlayersDestination(set(PLAYER_1));

        assertThat(destination1, is(not(equalTo(destination2))));
        assertThat(destination1.hashCode(), is(not(equalTo(destination2.hashCode()))));
    }

    private <T> Set<T> set(final T... objs) {
        final Set<T> set = new HashSet<T>();
        if (objs != null) {
            set.addAll(Arrays.asList(objs));
        }
        return set;
    }
}
