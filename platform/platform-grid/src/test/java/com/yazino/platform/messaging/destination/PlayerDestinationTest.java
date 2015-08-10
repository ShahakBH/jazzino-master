package com.yazino.platform.messaging.destination;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

public class PlayerDestinationTest {
    private static final BigDecimal PLAYER = BigDecimal.valueOf(10);

    @Mock
    private DocumentDispatcher documentDispatcher;

    private PlayerDestination unit;
    private Document document;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        unit = new PlayerDestination(PLAYER);

        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("aTestHeader", "aTestValue");
        document = new Document("TEST_MSG", "aBody", headers);
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void destinationShouldRejectANullList() {
        new PlayerDestination(null);
    }

    @Test
    public void documentShouldBeDispatchedToSpecifiedPlayer() {
        unit.send(document, documentDispatcher);

        verify(documentDispatcher).dispatch(document, Collections.singleton(PLAYER));
    }

    @Test
    public void destinationsAreEqualIfThePlayerIsEqual() {
        final PlayerDestination destination1 = new PlayerDestination(PLAYER);
        final PlayerDestination destination2 = new PlayerDestination(PLAYER);

        assertThat(destination1, is(equalTo(destination2)));
        assertThat(destination1.hashCode(), is(equalTo(destination2.hashCode())));
    }

    @Test
    public void destinationsAreNotEqualIfThePlayerDiffers() {
        final PlayerDestination destination1 = new PlayerDestination(PLAYER);
        final PlayerDestination destination2 = new PlayerDestination(PLAYER.add(BigDecimal.TEN));

        assertThat(destination1, is(not(equalTo(destination2))));
        assertThat(destination1.hashCode(), is(not(equalTo(destination2.hashCode()))));
    }
}
