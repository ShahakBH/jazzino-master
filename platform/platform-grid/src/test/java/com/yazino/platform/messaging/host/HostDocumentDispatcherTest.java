package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.DocumentDispatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HostDocumentDispatcherTest {

    @Mock
    private DocumentDispatcher documentDispatcher;
    @Mock
    private HostDocument hostDocument;

    private HostDocumentDispatcher underTest;

    @Before
    public void setUp() {
        underTest = new HostDocumentDispatcher(documentDispatcher);
    }

    @Test(expected = NullPointerException.class)
    public void aNullDocumentDispatcherThrowsANullPointerException() {
        new HostDocumentDispatcher(null);
    }

    @Test(expected = NullPointerException.class)
    public void aNullHostDocumentThrowsANullPointerExceptionOnSend() {
        underTest.send(null);
    }

    @Test
    public void aHostDocumentSendsItself() {
        underTest.send(hostDocument);

        verify(hostDocument).send(documentDispatcher);
    }

}
