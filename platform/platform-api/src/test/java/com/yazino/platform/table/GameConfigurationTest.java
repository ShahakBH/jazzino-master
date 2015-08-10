package com.yazino.platform.table;

import com.yazino.platform.SerializationTestHelper;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class GameConfigurationTest {

    @Test
    public void testSerialization() throws ClassNotFoundException, IOException {
        final List<GameConfigurationProperty> props = Arrays.asList(
                new GameConfigurationProperty(BigDecimal.ONE, "GAME_ID", "propertyName", "value")
        );
        final GameConfiguration underTest = new GameConfiguration("gameId",
                "shortName",
                "displayName",
                new HashSet<String>(Arrays.asList("a1", "a2")),
                0).withProperties(props);
        SerializationTestHelper.testSerializationRoundTrip(underTest, true);
    }
}
