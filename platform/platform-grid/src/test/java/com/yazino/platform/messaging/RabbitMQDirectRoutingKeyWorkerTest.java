package com.yazino.platform.messaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RabbitMQDirectRoutingKeyWorkerTest {
    private static final String DOCUMENT_TYPE = "test document type";
    RabbitMQRoutingKeyWorker underTest = new RabbitMQDirectRoutingKeyWorker();

    @Test
    public void playerRoutingKey() {
        String result = underTest.getRoutingKey(DOCUMENT_TYPE, "12", null);
        assertEquals("PLAYER.12", result);
    }

    @Test
    public void playerTableRoutingKey() {
        String result = underTest.getRoutingKey(DOCUMENT_TYPE, "12", "15");
        assertEquals("PLAYERTABLE.12.15", result);
    }

    @Test
    public void tableRoutingKey() {
        String result = underTest.getRoutingKey(DOCUMENT_TYPE, null, "12");
        assertEquals("TABLE.12", result);
    }
}
