package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.DocumentDispatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ImmediateDocumentFactoryPublisherTest {
    @Mock
    private DocumentDispatcher documentDispatcher;
    @Mock
    private HostDocument hostDocument1;
    @Mock
    private HostDocument hostDocument2;

    private HostDocumentPublisher unit;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        unit = new ImmediateHostDocumentPublisher(documentDispatcher);
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void constructorDoesNotAcceptANullDocumentContext() {
        new ImmediateHostDocumentPublisher(null);
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void aNullDocumentFactoryListIsRejected() {
        unit.publish(null);
    }

    @Test
    public void anEmptyDocumentFactoryListIsIgnored() {
        unit.publish(Collections.<HostDocument>emptyList());

        verifyZeroInteractions(documentDispatcher);
    }

    @Test
    public void documentsAreSentImmediately() {
        unit.publish(asList(hostDocument1, hostDocument2));

        verify(hostDocument1).send(documentDispatcher);
        verify(hostDocument2).send(documentDispatcher);
    }
}
