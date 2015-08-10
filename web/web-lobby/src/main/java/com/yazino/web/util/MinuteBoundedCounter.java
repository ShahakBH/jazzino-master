package com.yazino.web.util;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

@Component("minuteCounter")
public class MinuteBoundedCounter {

    private DateTime startOfCounterMinute = startOfCurrentMinute();
    private int count = 0;

    public synchronized int incrementAndGet() {
        if (counterIsExpired()) {
            rolloverCounter();
        }

        return incrementCount();
    }

    private int incrementCount() {
        count += 1;
        return count;
    }

    private boolean counterIsExpired() {
        return startOfCurrentMinute().isAfter(startOfCounterMinute);
    }

    private void rolloverCounter() {
        startOfCounterMinute = startOfCurrentMinute();
        count = 0;
    }

    private DateTime startOfCurrentMinute() {
        return new DateTime().withSecondOfMinute(0).withMillisOfSecond(0);
    }

}
