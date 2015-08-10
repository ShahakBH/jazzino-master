package com.yazino.platform.messaging;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DocumentTypeTest {
    private static final String DOCUMENT_TYPE_NAME = "COUNTDOWN";

    @Test
    public void lookForDocumentTypeCountdownAndVerifyName() {
        assertEquals(DocumentType.COUNTDOWN.name(), DOCUMENT_TYPE_NAME);
        assertEquals(DocumentType.COUNTDOWN.getName(), DOCUMENT_TYPE_NAME);
    }


    //TODO: establish intent of name
    @Ignore
    @Test
    public void ensureAllDocumentTypeNamesMatchTheirName() {
        DocumentType[] types = DocumentType.values();
        for (DocumentType type : types) {
            assertEquals(type.getName(), type.name());
        }
    }


}
