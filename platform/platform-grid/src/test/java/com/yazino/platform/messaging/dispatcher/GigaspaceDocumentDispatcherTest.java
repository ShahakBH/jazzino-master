package com.yazino.platform.messaging.dispatcher;

import com.yazino.platform.grid.Routing;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentWrapper;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.HashMap;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceDocumentDispatcherTest {
    private static final BigDecimal PLAYER_1 = new BigDecimal("1");
    private static final BigDecimal PLAYER_2 = new BigDecimal("2");
    private static final BigDecimal PLAYER_3 = new BigDecimal("3");

    @Mock
    private GigaSpace tableSpace;
    @Mock
    private Routing routing;

    private final Document dummyDocument = new Document("type", "body", new HashMap<String, String>());
    private final DocumentWrapper expectedDocument = new DocumentWrapper(dummyDocument, newHashSet(PLAYER_1, PLAYER_2, PLAYER_3));

    private GigaspaceDocumentDispatcher underTest;

    @Before
    public void setUp() throws Exception {
        when(routing.partitionId()).thenReturn(1);
        underTest = new GigaspaceDocumentDispatcher(tableSpace, routing);
    }

    @Test
    public void testDispatchedDocumentsAreCorrectlyConstructedFromPlayerIds() {
        underTest.dispatch(dummyDocument, newHashSet(PLAYER_1, PLAYER_2, PLAYER_3));

        verify(tableSpace).write(argThat(matches(expectedDocument)));
    }

    @Test
    public void testDispatchedDocumentsAreCorrectlyConstructedFromRecipients() {
        underTest.dispatch(dummyDocument, newHashSet(PLAYER_1, PLAYER_2, PLAYER_3));

        verify(tableSpace).write(argThat(matches(expectedDocument)));
    }

    private DocumentMatcher matches(final DocumentWrapper expectedDocument) {
        return new DocumentMatcher(expectedDocument);
    }

    private static class DocumentMatcher extends TypeSafeMatcher<DocumentWrapper> {
        private DocumentWrapper expected;

        private DocumentMatcher(final DocumentWrapper expected) {
            this.expected = expected;
        }

        @Override
        public boolean matchesSafely(final DocumentWrapper actual) {
            return expected.getRecipients().equals(actual.getRecipients())
                    && expected.getDocument().equals(actual.getDocument());
        }

        @Override
        public void describeTo(final Description description) {
            description.appendValue(expected);
        }
    }
}
