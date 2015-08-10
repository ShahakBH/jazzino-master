package com.yazino.platform.messaging.destination;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

public class ObserversDestinationTest {
    @Mock
    private DocumentDispatcher documentDispatcher;

    private ObserversDestination unit;
    private Document document;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        unit = new ObserversDestination();

        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("aTestHeader", "aTestValue");
        document = new Document("TEST_MSG", "aBody", headers);
    }

    @Test
    public void documentShouldBeDispatchedToAllObservers() {
        unit.send(document, documentDispatcher);

        verify(documentDispatcher).dispatch(document);
    }

    @Test
    public void destinationsAreAlwaysEqual() {
        final ObserversDestination destination1 = new ObserversDestination();
        final ObserversDestination destination2 = new ObserversDestination();

        assertThat(destination1, is(equalTo(destination2)));
        assertThat(destination1.hashCode(), is(equalTo(destination2.hashCode())));
    }
}
