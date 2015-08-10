package com.yazino.metrics.repository;

import com.yazino.metrics.model.Metric;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetricRepositoryTest {
    private static final DateTime CURRENT_DATE = new DateTime(453256243645L);
    private static final DateTime EARLIER_DATE = new DateTime(453256243610L);
    private static final DateTime LATER_DATE = new DateTime(453256243699L);

    private final MetricRepository underTest = new MetricRepository();

    @Test
    public void readingANonExistentMetricByNameReturnsAnEmptyMap() {
        assertThat(underTest.byName("anAbsentName"), is(empty()));
    }

    @Test
    public void readingANonExistentMetricByNameAndSourceReturnsNull() {
        assertThat(underTest.byNameAndSource("anAbsentName", "anAbsentSource"), is(nullValue()));
    }

    @Test
    public void readingANonExistentSourceReturnsNull() {
        underTest.save("aPresentName", "anotherSource", aMetricFrom(CURRENT_DATE));

        assertThat(underTest.byNameAndSource("aPresentName", "anAbsentSource"), is(nullValue()));
    }

    @Test
    public void aMetricCanBeSavedAndReadFromTheRepositoryByNameAndSource() {
        final Metric expectedMetric = aMetricFrom(CURRENT_DATE);

        underTest.save("aName", "aSource", expectedMetric);
        final Metric actualMetric = underTest.byNameAndSource("aName", "aSource");

        assertThat(actualMetric, is(equalTo(expectedMetric)));
    }

    @Test
    public void aMetricCanBeUpdatedAndReplacesAnOlderRecordedMetric() {
        final Metric oldMetric = aMetricFrom(CURRENT_DATE);
        final Metric newMetric = aMetricFrom(LATER_DATE);

        underTest.save("aName", "aSource", oldMetric);
        underTest.save("aName", "aSource", newMetric);

        assertThat(underTest.byNameAndSource("aName", "aSource"), is(equalTo(newMetric)));
    }

    @Test
    public void aMetricCannotBeUpdatedIfTheTimestampIsOlderThanTheCurrentlyRecordedMetric() {
        final Metric oldMetric = aMetricFrom(CURRENT_DATE);
        final Metric newMetric = aMetricFrom(EARLIER_DATE);

        underTest.save("aName", "aSource", oldMetric);
        underTest.save("aName", "aSource", newMetric);

        assertThat(underTest.byNameAndSource("aName", "aSource"), is(equalTo(oldMetric)));
    }

    @Test
    public void metricFromMultipleSourcesCanBeSavedAndReadFromTheRepositoryByName() {
        final Metric expectedOneMetric = aMetricFrom(CURRENT_DATE);
        final Metric expectedTwoMetric = aMetricFrom(CURRENT_DATE);
        final Metric expectedThreeMetric = aMetricFrom(CURRENT_DATE);
        final Map<String, Metric> expectedMetrics = new HashMap<>();
        expectedMetrics.put("sourceOne", expectedOneMetric);
        expectedMetrics.put("sourceTwo", expectedTwoMetric);
        expectedMetrics.put("sourceThree", expectedThreeMetric);

        underTest.save("aName", "sourceOne", expectedOneMetric);
        underTest.save("aName", "sourceTwo", expectedTwoMetric);
        underTest.save("aName", "sourceThree", expectedThreeMetric);
        final Map<String, Metric> actualMetric = underTest.byName("aName");

        assertThat(actualMetric, is(equalTo(expectedMetrics)));
    }

    private Metric aMetricFrom(final DateTime timestamp) {
        final Metric metric = mock(Metric.class);
        when(metric.getTimestamp()).thenReturn(timestamp);
        return metric;
    }

    private Matcher<Map> empty() {
        return new TypeSafeMatcher<Map>() {
            @Override
            protected boolean matchesSafely(final Map item) {
                return item != null && item.isEmpty();
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("is empty");
            }
        };
    }

}
