package com.yazino.controller;

import com.yazino.model.document.SendGameMessageRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ui.ModelMap;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentHeaderType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static java.math.BigDecimal.valueOf;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class GameControllerTest {

    private GameController underTest;
    private DocumentDispatcher documentDispatcher;

    @Before
    public void setUp() {
        documentDispatcher = mock(DocumentDispatcher.class);
        underTest = new GameController(null, null, null, documentDispatcher);
    }

    @Test
    public void shouldSendGameMessageToPlayer() {
        final SendGameMessageRequest request = new SendGameMessageRequest();
        request.setBody("body");
        request.setType("MY_DOC");
        request.setPlayerIds("1, 2");
        underTest.sendGameMessage(request, new ModelMap());
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentDispatcher).dispatch(captor.capture(), eq(set(valueOf(1), valueOf(2))));

        final Document expected = new Document("MY_DOC", "body", Collections.<String, String>emptyMap());
        assertDocumentsMatch(expected, captor.getValue());
    }

    @Test
    public void shouldSendGameMessageToPlayerAtTable(){
        final SendGameMessageRequest request = new SendGameMessageRequest();
        request.setBody("body");
        request.setType("MY_DOC");
        request.setPlayerIds("1, 2");
        request.setTableMessage(true);
        underTest.sendGameMessage(request, new ModelMap());
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentDispatcher).dispatch(captor.capture(), eq(set(valueOf(1), valueOf(2))));

        final Map<String, String> headers = new HashMap<String, String>();
        headers.put(DocumentHeaderType.TABLE.getHeader(), "1");
        final Document expected = new Document("MY_DOC", "body", headers);
        assertDocumentsMatch(expected, captor.getValue());

    }

    private void assertDocumentsMatch(final Document expected, final Document actual) {
        assertEquals(expected.getBody(), actual.getBody());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getHeaders(), actual.getHeaders());
    }

    private HashSet<BigDecimal> set(BigDecimal... value) {
        return new HashSet<BigDecimal>(asList(value));
    }

}
