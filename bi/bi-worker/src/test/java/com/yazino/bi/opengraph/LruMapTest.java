package com.yazino.bi.opengraph;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class LruMapTest {

    private static final int MAXIMUM_CAPACITY = 5;
    private static final int LEAST_RECENTLY_ACCESSED = MAXIMUM_CAPACITY - 1;
    private LruMap<Integer, Integer> underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new LruMap<>(MAXIMUM_CAPACITY);
    }

    @Test
    public void shouldNotGrowBeyondCapacity() {
        for (int i = 0; i < MAXIMUM_CAPACITY + 1; i++) {
            underTest.put(i, i);
        }
        System.out.println(underTest.size());
        Assert.assertThat(underTest.size(), is(equalTo(MAXIMUM_CAPACITY)));
    }

    @Test
    public void shouldDropLeastRecentlyUsedElementAfterInsertionIntoFullMap() {
        for (int i = 0; i < MAXIMUM_CAPACITY; i++) {
            underTest.put(i, i);
            System.out.println("putting" + i);
        }
        //this proves it is LRU not FIFO
        touchValuesInOrder(4, 3, 2, 1, 0);
        underTest.put(MAXIMUM_CAPACITY, MAXIMUM_CAPACITY);


        assertThat(underTest.get(MAXIMUM_CAPACITY), is(equalTo(MAXIMUM_CAPACITY)));
        Assert.assertThat(underTest.size(), is(equalTo(MAXIMUM_CAPACITY)));
        assertThat(underTest.get(LEAST_RECENTLY_ACCESSED), nullValue());

        for (int i = 0; i < LEAST_RECENTLY_ACCESSED; i++) {
            System.out.println(underTest.get(i));
            assertThat(underTest.get(i), is(CoreMatchers.equalTo(i)));
        }
    }

    private void touchValuesInOrder(int... values) {
        for (int value : values) {
            assertThat(underTest.get(value), is(CoreMatchers.equalTo(value)));
            System.out.println("getting" + value);
        }
    }
}
