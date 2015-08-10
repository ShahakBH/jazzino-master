package com.yazino.platform.lightstreamer.adapter;

import org.junit.Test;
import org.springframework.amqp.core.ExchangeTypes;

import static org.junit.Assert.assertEquals;

public class RabbitConfigurationTest {
    private final RabbitConfiguration underTest = new RabbitConfiguration();

    @Test
    public void shouldCreateExchangeWithCorrectExchangeName() throws Exception {
        assertEquals("foo", underTest.playerExchange("foo").getName());
    }

    @Test
    public void shouldCreateExchangeWithCorrectType() throws Exception {
        assertEquals(ExchangeTypes.DIRECT, underTest.playerExchange("foo").getType());
    }


}
