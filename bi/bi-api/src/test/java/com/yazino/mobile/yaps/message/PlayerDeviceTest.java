package com.yazino.mobile.yaps.message;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class PlayerDeviceTest {


    @Test
    public void shouldntBeEqualIfDeviceTokensDiffer() {
        PlayerDevice playerDeviceA = new PlayerDevice("foo", BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
        PlayerDevice playerDeviceB = new PlayerDevice("foo", BigDecimal.ONE, "abcd", "com.yazino.YazinoApp");
        assertFalse(playerDeviceA.equals(playerDeviceB));
    }

    @Test
    public void shouldntBeEqualIfPlayerIdsDiffer() {
        PlayerDevice playerDeviceA = new PlayerDevice("foo", BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
        PlayerDevice playerDeviceB = new PlayerDevice("foo", BigDecimal.TEN, "1234", "com.yazino.YazinoApp");
        assertFalse(playerDeviceA.equals(playerDeviceB));
    }

    @Test
    public void shouldntBeEqualIfGameTypesDiffer() {
        PlayerDevice playerDeviceA = new PlayerDevice("foo", BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
        PlayerDevice playerDeviceB = new PlayerDevice("bar", BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
        assertFalse(playerDeviceA.equals(playerDeviceB));
    }

    @Test
    public void shouldntBeEqualIfBundlesDiffer() {
        PlayerDevice playerDeviceA = new PlayerDevice("foo", BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
        PlayerDevice playerDeviceB = new PlayerDevice("bar", BigDecimal.ONE, "1234", "com.yazino.Blackjack");
        assertFalse(playerDeviceA.equals(playerDeviceB));
    }

    @Test
    public void shouldBeEqualIfDeviceTokenPlayerIdAndGameTypeAndBundleAreSame() {
        PlayerDevice playerDeviceA = new PlayerDevice("foo", BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
        PlayerDevice playerDeviceB = new PlayerDevice("foo", BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
        assertTrue(playerDeviceA.equals(playerDeviceB));
    }

    @Test
    public void shouldHaveMatchingHashcodesWhenEqual() {
        PlayerDevice playerDeviceA = new PlayerDevice("foo", BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
        PlayerDevice playerDeviceB = new PlayerDevice("foo", BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
        assertTrue(playerDeviceA.equals(playerDeviceB));
        assertEquals(playerDeviceA.hashCode(), playerDeviceB.hashCode());
    }

    @Test(expected = NullPointerException.class)
    public void cannotBeCreatedWithNullGameType() {
        new PlayerDevice(null, BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotBeCreatedWithEmptyGameType_1() {
        new PlayerDevice("", BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotBeCreatedWithEmptyGameType_2() {
        new PlayerDevice("   ", BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotBeCreatedWithEmptyBundle_1() {
        new PlayerDevice("ff", BigDecimal.ONE, "1234", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotBeCreatedWithEmptyBundle_2() {
        new PlayerDevice("ff", BigDecimal.ONE, "1234", "   ");
    }

    @Test(expected = NullPointerException.class)
    public void cannotBeCreatedWithNullPlayerId() {
        new PlayerDevice("ff", null, "1234", "com.yazino.YazinoApp");
    }

    @Test(expected = NullPointerException.class)
    public void cannotBeCreatedWithNullDeviceToken() {
        new PlayerDevice("ff", null, null, "com.yazino.YazinoApp");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotBeCreatedWithEmptyDeviceToken_1() {
        new PlayerDevice("ff", BigDecimal.ONE, "", "com.yazino.YazinoApp");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotBeCreatedWithEmptyDeviceToken_2() {
        new PlayerDevice("ff", BigDecimal.ONE, "   ", "com.yazino.YazinoApp");
    }

    @Test
    public void shouldSerializeAndDeserializeUsingJson() {
        PlayerDevice playerDevice = new PlayerDevice("foo", BigDecimal.ONE, "1234", "com.yazino.YazinoApp");
        String jsonString = new JsonHelper().serialize(playerDevice);

        PlayerDevice deserilaizedPlayerDevice = new JsonHelper().deserialize(PlayerDevice.class, jsonString);
        assertEquals(deserilaizedPlayerDevice, playerDevice);
    }

    @Test
    public void shouldDeserialzeFromJson() {
        String json = "{\"gameType\":\"foo\",\"playerId\":1,\"deviceToken\":\"1234\",\"bundle\":\"com.yazino.YazinoApp\"}";
        PlayerDevice playerDevice = new JsonHelper().deserialize(PlayerDevice.class, json);
        assertEquals("foo", playerDevice.getGameType());
        assertEquals(BigDecimal.ONE, playerDevice.getPlayerId());
        assertEquals("1234", playerDevice.getDeviceToken());
        assertEquals("com.yazino.YazinoApp", playerDevice.getBundle());
    }

}
