package com.yazino.metrics.aggregation;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.metrics.model.Meter;
import com.yazino.metrics.model.Metric;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MetricAggregatorTest {

    private static final DateTime BASE_TIMESTAMP = new DateTime(5423134653454L);
    private static final int WINDOW_IN_SECONDS = 120;

    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private MetricAggregator underTest;

    @Before
    public void setUp() {
        when(yazinoConfiguration.getInt("metrics.update.window", 120)).thenReturn(120);

        underTest = new MetricAggregator(yazinoConfiguration);
    }

    @Test
    public void aggregatingANullMapReturnsNull() {
        assertThat(underTest.aggregate(0L, null), is(nullValue()));
    }

    @Test
    public void aggregatingAnEmptyMapReturnsNull() {
        assertThat(underTest.aggregate(0L, new HashMap<String, Metric>()), is(nullValue()));
    }

    @Test
    public void aggregatingASingleMetricWithinTheWindowReturnsThatMetric() {
        final HashMap<String, Metric> metrics = new HashMap<>();
        metrics.put("aMeter", aMeter(10, 1));

        assertThat(underTest.aggregate(BASE_TIMESTAMP.getMillis(), metrics), is(equalTo(aMeter(10, 1))));
    }

    @Test
    public void aggregatingMultipleMetricsWithinTheWindowReturnsTheSumOfTheMetrics() {
        final HashMap<String, Metric> metrics = new HashMap<>();
        metrics.put("aMeter", aMeter(10, 1));
        metrics.put("anotherMeter", aMeter(20, 2));
        metrics.put("yetAnotherMeter", aMeter(30, 3));
        final Metric expectedMeter = new Meter(BASE_TIMESTAMP.minusSeconds(10), 36, 6.06, 7.8, 6.9, 6.6);

        assertThat((Meter) underTest.aggregate(BASE_TIMESTAMP.getMillis(), metrics), is(closeTo((Meter) expectedMeter)));
    }

    @Test
    public void aggregatingMultipleMetricsExcludesThoseOutsideTheWindow() {
        final HashMap<String, Metric> metrics = new HashMap<>();
        metrics.put("aMeter", aMeter(10, 1));
        metrics.put("anotherMeter", aMeter(WINDOW_IN_SECONDS + 1, 2));
        metrics.put("yetAnotherMeter", aMeter(30, 3));
        final Metric expectedMeter = new Meter(BASE_TIMESTAMP.minusSeconds(10), 24, 4.04, 5.2, 4.6, 4.4);

        assertThat((Meter) underTest.aggregate(BASE_TIMESTAMP.getMillis(), metrics), is(closeTo((Meter) expectedMeter)));
    }

    @Test
    public void aggregatingMultipleMetricsReturnsNullWhenAllResultsOutsideTheWindow() {
        final HashMap<String, Metric> metrics = new HashMap<>();
        metrics.put("aMeter", aMeter(WINDOW_IN_SECONDS + 1, 1));
        metrics.put("anotherMeter", aMeter(WINDOW_IN_SECONDS + 2, 2));
        metrics.put("yetAnotherMeter", aMeter(WINDOW_IN_SECONDS + 3, 3));

        assertThat(underTest.aggregate(BASE_TIMESTAMP.getMillis(), metrics), is(nullValue()));
    }

    private Matcher<Meter> closeTo(final Meter expected) {
        return new TypeSafeMatcher<Meter>() {
            @Override
            protected boolean matchesSafely(final Meter actual) {
                return actual == expected
                        || expected.getTimestamp().equals(actual.getTimestamp())
                        && expected.getCount() == actual.getCount()
                        && Matchers.closeTo(expected.getOneMinuteRatePerSecond(), 0.1).matches(actual.getOneMinuteRatePerSecond())
                        && Matchers.closeTo(expected.getFiveMinuteRatePerSecond(), 0.1).matches(actual.getFiveMinuteRatePerSecond())
                        && Matchers.closeTo(expected.getFifteenMinuteRatePerSecond(), 0.1).matches(actual.getFifteenMinuteRatePerSecond())
                        && Matchers.closeTo(expected.getMeanRatePerSecond(), 0.1).matches(actual.getMeanRatePerSecond());
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("is close to ").appendValue(expected);
            }
        };
    }

    private Metric aMeter(final int offset,
                          final int base) {
        return new Meter(BASE_TIMESTAMP.minusSeconds(offset), 10 + base, base + 0.02, base + 0.6, base + 0.3, base + 0.2);
    }

}
