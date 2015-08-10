package com.yazino.platform.messaging.destination;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class DestinationFactoryTest {
    private static final BigDecimal PLAYER_2 = BigDecimal.valueOf(20);
    private static final BigDecimal PLAYER_1 = BigDecimal.valueOf(10);

    private DestinationFactory unit;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        unit = new DestinationFactory();
    }

    @Test
    public void playerReturnsAPlayerDestinationObject() {
        assertThat((PlayerDestination) unit.player(PLAYER_1), isA(PlayerDestination.class));
    }

    @Test
    public void playersReturnsAPlayersDestinationObject() {
        assertThat((PlayersDestination) unit.players(set(PLAYER_1, PLAYER_2)), isA(PlayersDestination.class));
    }

    @Test
    public void observersReturnsAnObserversDestinationobject() {
        assertThat((ObserversDestination) unit.observers(), isA(ObserversDestination.class));
    }

    @Test
    public void theSameObserversObjectIsAlwaysReturned() {
        assertThat(unit.observers(), is(sameInstance(unit.observers())));
    }

    private <T> Set<T> set(final T... objs) {
        final Set<T> set = new HashSet<T>();
        if (objs != null) {
            set.addAll(Arrays.asList(objs));
        }
        return set;
    }
}
