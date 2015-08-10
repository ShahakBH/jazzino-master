package com.yazino.platform.lightstreamer.adapter;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class PlayerIdResolverTest {

    private PlayerIdResolver resolver;

    @Before
    public void setUp() {
        resolver = new PlayerIdResolver();
    }

    @Test
    public void canResolvePlayerIdFromItem() {
        assertEquals(BigDecimal.valueOf(230), resolver.resolve("PLAYER.230"));
        assertEquals(BigDecimal.valueOf(230), resolver.resolve("PLAYERTABLE.230.520"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void breaksIfItemDoesNotMatchExpectedFormat() {
        resolver.resolve("GLOBAL");
    }

    @Test(expected = IllegalArgumentException.class)
    public void breaksIfItemIsNull() {
        resolver.resolve(null);
    }
}
