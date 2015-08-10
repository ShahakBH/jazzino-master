package com.yazino.yaps;

import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.Assert.*;

public class MessageIDGeneratorTest {

    private final MessageIDGenerator mGenerator = new MessageIDGenerator();

    @Test
    public void shouldReturnMessageIdIfPresent() throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId("foo");
        Message message = new Message(new byte[0], properties);
        Object key = mGenerator.getKey(message);
        assertEquals("foo", key);
    }

    @Test
    public void shouldGenerateRandomKeyIfMessageIdNotPresent() throws Exception {
        Message message = new Message(new byte[0], new MessageProperties());
        Object key = mGenerator.getKey(message);
        assertNotNull(key);
        assertTrue(key instanceof String);
    }

    @Test
    public void shouldReturnGeneratedKeyOnMultipleCalls() throws Exception {
        Message message = new Message(new byte[0], new MessageProperties());
        Object key = mGenerator.getKey(message);
        assertEquals(key, mGenerator.getKey(message));
        assertEquals(key, mGenerator.getKey(message));
        assertEquals(key, mGenerator.getKey(message));
    }

    @Test
    public void shouldReturnDifferentIDsForDifferentMessages() throws Exception {
        Message messageA = new Message(new byte[0], new MessageProperties());
        Message messageB = new Message(new byte[0], new MessageProperties());
        Message messageC = new Message(new byte[0], new MessageProperties());

        Object keyA = mGenerator.getKey(messageA);
        Object keyB = mGenerator.getKey(messageB);
        Object keyC = mGenerator.getKey(messageC);
        assertFalse(keyA.equals(keyB));
        assertFalse(keyB.equals(keyC));
    }
}
