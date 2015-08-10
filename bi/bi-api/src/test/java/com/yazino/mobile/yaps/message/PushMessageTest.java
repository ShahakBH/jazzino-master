package com.yazino.mobile.yaps.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class PushMessageTest {

    private final JsonHelper jsonHelper = new JsonHelper(true);

    @Test(expected = NullPointerException.class)
    public void cannotBeCreatedWithNullGameType() {
        new PushMessage(null, toBD(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotBeCreatedWithEmptyGameType_1() {
        new PushMessage("", toBD(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotBeCreatedWithEmptyGameType_2() {
        new PushMessage("  ", toBD(1));
    }

    @Test(expected = NullPointerException.class)
    public void cannotBeCreatedWithNullPlayerId() {
        new PushMessage("foo", null);
    }

    @Test
    public void shouldntBeEqualWhenDifferentGameTypes() {
        PushMessage messageA = new PushMessage("foo", toBD(1));
        PushMessage messageB = new PushMessage("bar", toBD(1));
        assertFalse(messageA.equals(messageB));
    }

    @Test
    public void shouldntBeEqualWhenDifferentPlayerId() {
        PushMessage messageA = new PushMessage("foo", toBD(1));
        PushMessage messageB = new PushMessage("foo", toBD(3));
        assertFalse(messageA.equals(messageB));
    }

    @Test
    public void shouldntBeEqualWhenDifferentExpiryDate() {
        PushMessage messageA = new PushMessage("foo", toBD(1));
        messageA.setExpiryDateSecondsSinceEpoch(5);
        PushMessage messageB = new PushMessage("foo", toBD(1));
        messageB.setExpiryDateSecondsSinceEpoch(7);
        assertFalse(messageA.equals(messageB));
    }

    @Test
    public void shouldntBeEqualWhenDifferentAlert() {
        PushMessage messageA = new PushMessage("foo", toBD(1));
        messageA.setAlert("abc");
        PushMessage messageB = new PushMessage("foo", toBD(1));
        messageB.setAlert("def");
        assertFalse(messageA.equals(messageB));
    }

    @Test
    public void shouldntBeEqualWhenDifferentSound() {
        PushMessage messageA = new PushMessage("foo", toBD(1));
        messageA.setSound("abc");
        PushMessage messageB = new PushMessage("foo", toBD(1));
        messageB.setSound("def");
        assertFalse(messageA.equals(messageB));
    }

    @Test
    public void shouldntBeEqualWhenDifferentBadge() {
        PushMessage messageA = new PushMessage("foo", toBD(1));
        messageA.setBadge(7);
        PushMessage messageB = new PushMessage("foo", toBD(1));
        messageB.setBadge(9);
        assertFalse(messageA.equals(messageB));
    }

    @Test
    public void shouldBeEqualWhenGameTypeAndPlayerIdsMatch() {
        PushMessage messageA = new PushMessage("foo", toBD(1));
        PushMessage messageB = new PushMessage("foo", toBD(1));
        assertTrue(messageA.equals(messageB));
        assertEquals(messageA.hashCode(), messageB.hashCode());
    }

    @Test
    public void shouldBeEqualWhenGameTypeAndPlayerIdsAndOtherPropertiesMatch() {
        PushMessage messageA = new PushMessage("foo", toBD(1));
        messageA.setAlert("foo");
        messageA.setBadge(9);
        PushMessage messageB = new PushMessage("foo", toBD(1));
        messageB.setAlert("foo");
        messageB.setBadge(9);
        assertTrue(messageA.equals(messageB));
        assertEquals(messageA.hashCode(), messageB.hashCode());
    }

    @Test
    public void shouldSerializeAndDeserialiseWithoutIdentifier() {
        PushMessage message = new PushMessage("foo", toBD(1));
        String jsonString = jsonHelper.serialize(message);

        PushMessage deserializedPushMessage = jsonHelper.deserialize(PushMessage.class, jsonString);
        assertThat(deserializedPushMessage, is(message));

    }

    @Test
    public void shouldDeserializeWithoutIdentifier() {
        PushMessage message = new PushMessage("foo", toBD(1));
        assertEquals(message, jsonHelper.deserialize(PushMessage.class, "{\"gameType\":\"foo\",\"playerId\":1,\"expiryDateSecondsSinceEpoch\":0}"));
    }

    @Test
    public void shouldSerializeWithExpiryDate() throws IOException {
        PushMessage message = new PushMessage("foo", toBD(1));
        message.setExpiryDateSecondsSinceEpoch(9888);

        final String serialised = jsonHelper.serialize(message);

        final JsonNode jsonNodes = new ObjectMapper().readTree(serialised);
        assertThat(jsonNodes.get("gameType").textValue(), is(equalTo("foo")));
        assertThat(jsonNodes.get("playerId").intValue(), is(equalTo(1)));
        assertThat(jsonNodes.get("expiryDateSecondsSinceEpoch").intValue(), is(equalTo(9888)));
    }

    @Test
    public void shouldDeserializeWithExpiryDate() {
        PushMessage message = new PushMessage("foo", toBD(2));
        message.setExpiryDateSecondsSinceEpoch(9888);
        assertEquals(message, jsonHelper.deserialize(PushMessage.class, "{\"gameType\":\"foo\",\"playerId\":2,\"expiryDateSecondsSinceEpoch\":9888}"));
    }

    @Test
    public void shouldSerializeWithAlert() throws IOException {
        PushMessage message = new PushMessage("foo", toBD(1));
        message.setAlert("bar");

        final String serialised = jsonHelper.serialize(message);

        final JsonNode jsonNodes = new ObjectMapper().readTree(serialised);
        assertThat(jsonNodes.get("gameType").textValue(), is(equalTo("foo")));
        assertThat(jsonNodes.get("playerId").intValue(), is(equalTo(1)));
        assertThat(jsonNodes.get("expiryDateSecondsSinceEpoch").intValue(), is(equalTo(0)));
        assertThat(jsonNodes.get("alert").textValue(), is(equalTo("bar")));
    }

    @Test
    public void shouldDeserializeWhenAlert() {
        PushMessage message = new PushMessage("foo", toBD(2));
        message.setAlert("bar");
        assertEquals(message, jsonHelper.deserialize(PushMessage.class, "{\"gameType\":\"foo\",\"playerId\":2,\"expiryDateSecondsSinceEpoch\":0,\"alert\":\"bar\"}"));
    }

    @Test
    public void shouldSerializeWhenSound() throws IOException {
        PushMessage message = new PushMessage("foo", toBD(1));
        message.setSound("default");

        final String serialised = jsonHelper.serialize(message);

        final JsonNode jsonNodes = new ObjectMapper().readTree(serialised);
        assertThat(jsonNodes.get("gameType").textValue(), is(equalTo("foo")));
        assertThat(jsonNodes.get("playerId").intValue(), is(equalTo(1)));
        assertThat(jsonNodes.get("expiryDateSecondsSinceEpoch").intValue(), is(equalTo(0)));
        assertThat(jsonNodes.get("sound").textValue(), is(equalTo("default")));
    }

    @Test
    public void shouldDeserializeWhenSound() {
        PushMessage message = new PushMessage("foo", toBD(2));
        message.setSound("default");
        assertEquals(message, jsonHelper.deserialize(PushMessage.class, "{\"gameType\":\"foo\",\"playerId\":2,\"expiryDateSecondsSinceEpoch\":0,\"sound\":\"default\"}"));
    }

    @Test
    public void shouldSerializeWhenBadge() throws IOException {
        PushMessage message = new PushMessage("foo", toBD(2));
        message.setBadge(5);

        final String serialised = jsonHelper.serialize(message);

        final JsonNode jsonNodes = new ObjectMapper().readTree(serialised);
        assertThat(jsonNodes.get("gameType").textValue(), is(equalTo("foo")));
        assertThat(jsonNodes.get("playerId").intValue(), is(equalTo(2)));
        assertThat(jsonNodes.get("expiryDateSecondsSinceEpoch").intValue(), is(equalTo(0)));
        assertThat(jsonNodes.get("badge").intValue(), is(equalTo(5)));
    }

    @Test
    public void shouldDeserializeWhenBadge() {
        PushMessage message = new PushMessage("foo", toBD(1));
        message.setBadge(5);
        assertEquals(message, jsonHelper.deserialize(PushMessage.class, "{\"gameType\":\"foo\",\"playerId\":1,\"expiryDateSecondsSinceEpoch\":0,\"badge\":5}"));
    }


    private static BigDecimal toBD(int value) {
        return BigDecimal.valueOf(value);
    }

}
