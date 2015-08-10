package com.yazino.game.api;

import com.yazino.game.api.SimpleRandomizer;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SimpleRandomizerTest {
    private SimpleRandomizer underTest;
    private Random random;

    @Before
    public void setUp() {
        random = mock(Random.class);
        underTest = new SimpleRandomizer();
        underTest.setRandom(random);
    }

    @Test
    public void testNextInt() {
        when(random.nextInt()).thenReturn(-3);

        int result = underTest.nextInt();

        assertEquals(3, result);
    }

    @Test
    public void testNextIntWithRange() {
        when(random.nextInt(200)).thenReturn(-3);

        int result = underTest.nextInt(200);

        assertEquals(3, result);
    }

    @Test
    public void testRandomRange() {
        when(random.nextInt(2)).thenReturn(1);

        final int result = underTest.nextInt(3, 5);

        assertEquals(4, result);
    }

    @Test
    public void testGetRandomNumbers() {
        when(random.nextInt(26)).thenReturn(-3, 5, 5, -13, 7);

        Set<Integer> result = underTest.getRandomNumbers(5, 13, 3);

        Set<Integer> expected = new HashSet<Integer>(Arrays.asList(5, 7, 13));
        assertEquals(expected, result);
    }

    @Test
    public void testGetRandomItemFromCollection() {
        Set<Integer> initial = new HashSet<Integer>(Arrays.asList(5, 7, 13));
        when(random.nextInt()).thenReturn(3, 4, 5);

        Integer result1 = (Integer) underTest.getRandomItemFromCollection(initial);
        Integer result2 = (Integer) underTest.getRandomItemFromCollection(initial);
        Integer result3 = (Integer) underTest.getRandomItemFromCollection(initial);

        assertEquals(new Integer(5), result1);
        assertEquals(new Integer(7), result2);
        assertEquals(new Integer(13), result3);
    }
}
