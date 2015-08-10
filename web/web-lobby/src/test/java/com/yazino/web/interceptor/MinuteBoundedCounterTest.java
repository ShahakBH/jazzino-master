package com.yazino.web.interceptor;

import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.util.MinuteBoundedCounter;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MinuteBoundedCounterTest {

    private MinuteBoundedCounter minuteBoundedCounter;

    @Before
    public void setup() {
        setTime(new DateTime());
        minuteBoundedCounter = new MinuteBoundedCounter();
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldFirstCountIsOne() {
        assertNextCountIs(1);
    }

    @Test
    public void incrementAndGet_shouldReturnIncrementedCount() {
        for(int i = 1; i <= 10; i++) {
            assertNextCountIs(i);
        }
    }

    private void assertNextCountIs(int i) {
        assertEquals(i, minuteBoundedCounter.incrementAndGet());
    }

    @Test
    public void shouldRollOverCounterWhenMinuteChanges() {
        assertNextCountIs(1);
        for(int i = 0; i < 3; i++) {
            assertNextCountIs(2);
            setTime(new DateTime().plusMinutes(i + 1));
            assertNextCountIs(1);
        }
    }

    private void setTime(DateTime time) {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(time.getMillis());
    }

}
