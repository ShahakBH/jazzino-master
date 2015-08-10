package com.yazino.platform.messaging;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DocumentProcessorTest {
    @Mock
    private DocumentDispatcher documentDispatcher;

    private DocumentProcessor documentProcessor;
    private Document document = new Document("Test Type", "Body", new HashMap<String, String>());
    private Set<BigDecimal> recipients = new HashSet<BigDecimal>(
            Arrays.asList(new BigDecimal("1.0"), new BigDecimal("2.0")));

    @Before
    public void setup() {
        documentProcessor = new DocumentProcessor(documentDispatcher);
    }

    @Test
    public void if_no_recipients_specified_anonymous_dispatch_called() throws InterruptedException {
        documentProcessor.processDocument(new DocumentWrapper(document, null));

        verify(documentDispatcher).dispatch(document);
    }

    @Test
    public void if_recipients_specified_broadcast_dispatch_called() throws InterruptedException {
        documentProcessor.processDocument(new DocumentWrapper(document, recipients));

        verify(documentDispatcher).dispatch(document, recipients);
    }

}
