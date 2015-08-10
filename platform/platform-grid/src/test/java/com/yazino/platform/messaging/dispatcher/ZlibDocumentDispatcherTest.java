package com.yazino.platform.messaging.dispatcher;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.messaging.dispatcher.ZLib.deflate;
import static com.yazino.platform.messaging.dispatcher.ZLib.inflate;
import static org.hamcrest.MatcherAssert.assertThat;

public class ZlibDocumentDispatcherTest {
    private static final String MESSAGE_TYPE = "test.document";
    private static final String MESSAGE_BODY = "a.body.of.a.message";
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final BigDecimal ANOTHER_PLAYER_ID = BigDecimal.TEN;

    @Mock
    private DocumentDispatcher delegateDispatcher;

    private ZlibDocumentDispatcher underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis());
        MockitoAnnotations.initMocks(this);

        underTest = new ZlibDocumentDispatcher(delegateDispatcher);
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void constructorShouldThrowANullPointerExceptionWhenGivenANullDelegate() {
        new ZlibDocumentDispatcher(null);
    }

    @Test
    public void aDocumentWithANullBodyIsNotModified() {
        underTest.dispatch(documentWithBody(null));

        Mockito.verify(delegateDispatcher).dispatch(documentWithBody(null));
    }

    @Test
    public void aDocumentWithANullBodyHasNoEncodingSet() {
        underTest.dispatch(documentWithBody(null));

        MatcherAssert.assertThat(delegatedEncoding(), Matchers.is(Matchers.nullValue()));
    }

    @Test
    public void nullDocumentsAreDelegatedVerbatim() {
        underTest.dispatch(null);

        Mockito.verify(delegateDispatcher).dispatch(null);
    }

    @Test
    public void documentBodiesAreZlibedAndBase64Encoded() throws IOException {
        underTest.dispatch(documentWithBody(MESSAGE_BODY));

        assertThat(inflate(delegatedBody()), Matchers.is(Matchers.equalTo(MESSAGE_BODY)));
    }

    @Test
    public void documentsHaveAnEncodingOfDEF() throws IOException {
        underTest.dispatch(documentWithBody(MESSAGE_BODY));

        MatcherAssert.assertThat(delegatedEncoding(), Matchers.is(Matchers.equalTo("DEF")));
    }

    @Test
    public void singlePlayerIdDocumentBodiesAreZlibedAndBase64Encoded() throws IOException {
        underTest.dispatch(documentWithBody(MESSAGE_BODY), PLAYER_ID);

        Mockito.verify(delegateDispatcher).dispatch(deflatedDocumentWithBody(deflate(MESSAGE_BODY)), PLAYER_ID);
    }

    @Test
    public void multiplePlayerIdDocumentBodiesAreZlibedAndBase64Encoded() throws IOException {
        underTest.dispatch(documentWithBody(MESSAGE_BODY), newHashSet(PLAYER_ID, ANOTHER_PLAYER_ID));

        Mockito.verify(delegateDispatcher).dispatch(deflatedDocumentWithBody(deflate(MESSAGE_BODY)),
                newHashSet(PLAYER_ID, ANOTHER_PLAYER_ID));
    }

    @Test
    public void multipleRecipientDocumentBodiesAreZlibedAndBase64Encoded() throws IOException {
        underTest.dispatch(documentWithBody(MESSAGE_BODY),
                newHashSet(PLAYER_ID, ANOTHER_PLAYER_ID));

        Mockito.verify(delegateDispatcher).dispatch(deflatedDocumentWithBody(deflate(MESSAGE_BODY)),
                newHashSet(PLAYER_ID, ANOTHER_PLAYER_ID));
    }

    private String delegatedBody() {
        final ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        Mockito.verify(delegateDispatcher).dispatch(documentCaptor.capture());
        return documentCaptor.getValue().getBody();
    }

    private String delegatedEncoding() {
        final ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        Mockito.verify(delegateDispatcher).dispatch(documentCaptor.capture());
        return documentCaptor.getValue().getEncoding();
    }

    private Document documentWithBody(final String body) {
        return new Document(MESSAGE_TYPE, body, headers());
    }

    private Document deflatedDocumentWithBody(final String body) {
        return new Document(MESSAGE_TYPE, body, headers(), "DEF");
    }

    private Map<String, String> headers() {
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("test.header", "true");
        return headers;
    }
}
