package com.yazino.platform.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.yazino.platform.grid.Executor;
import com.yazino.platform.grid.ExecutorTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceRemotingMetricsServiceTest {
    @Mock
    private MetricRegistry metrics;
    @Mock
    private Meter meterOneOne;
    @Mock
    private Meter meterOneTwo;
    @Mock
    private Meter meterTwoOne;
    @Mock
    private Meter meterTwoTwo;

    private GigaspaceRemotingMetricsService underTest;

    @Before
    public void setUp() {
        when(metrics.getMeters())
                .thenReturn(metersForPartitionOne())
                .thenReturn(metersForPartitionTwo());

        final Map<String, Object> services = new HashMap<>();
        services.put("metrics", metrics);
        final Executor executor = ExecutorTestUtils.mockExecutorWith(2, services);

        underTest = new GigaspaceRemotingMetricsService(executor);
    }

    private SortedMap<String, Meter> metersForPartitionOne() {
        final SortedMap<String, Meter> meters = new TreeMap<>();
        meters.put("meter-1", meterOneOne);
        meters.put("meter-2", meterOneTwo);
        return meters;
    }

    private SortedMap<String, Meter> metersForPartitionTwo() {
        final SortedMap<String, Meter> meters = new TreeMap<>();
        meters.put("meter-1", meterTwoOne);
        meters.put("meter-2", meterTwoTwo);
        return meters;
    }

    @Test
    public void getMetersShouldGetMetersFromAllPartitions() {
        when(meterOneOne.getOneMinuteRate()).thenReturn(3D);
        when(meterOneTwo.getOneMinuteRate()).thenReturn(5D);
        when(meterTwoOne.getOneMinuteRate()).thenReturn(7D);
        when(meterTwoTwo.getOneMinuteRate()).thenReturn(11D);

        final Map<String,BigDecimal> actualResults = underTest.getMeters(MetricsService.Range.ONE_MINUTE);

        final Map<String, BigDecimal> expectedResults = new HashMap<>();
        expectedResults.put("meter-1", new BigDecimal("10.000"));
        expectedResults.put("meter-2", new BigDecimal("16.000"));
        assertThat(actualResults, is(equalTo(expectedResults)));
    }

    @Test
    public void getMetersShouldRoundResultsToThreeDecimalPlaces() {
        when(meterOneOne.getOneMinuteRate()).thenReturn(3.12345);
        when(meterOneTwo.getOneMinuteRate()).thenReturn(5.23456);
        when(meterTwoOne.getOneMinuteRate()).thenReturn(7.34567);
        when(meterTwoTwo.getOneMinuteRate()).thenReturn(11.45678);

        final Map<String,BigDecimal> actualResults = underTest.getMeters(MetricsService.Range.ONE_MINUTE);

        final Map<String, BigDecimal> expectedResults = new HashMap<>();
        expectedResults.put("meter-1", new BigDecimal("10.469"));
        expectedResults.put("meter-2", new BigDecimal("16.692"));
        assertThat(actualResults, is(equalTo(expectedResults)));
    }

    @Test
    public void getMetersShouldRetrieveResultsForTheFiveMinuteRange() {
        when(meterOneOne.getFiveMinuteRate()).thenReturn(3.12345);
        when(meterOneTwo.getFiveMinuteRate()).thenReturn(5.23456);
        when(meterTwoOne.getFiveMinuteRate()).thenReturn(7.34567);
        when(meterTwoTwo.getFiveMinuteRate()).thenReturn(11.45678);

        final Map<String,BigDecimal> actualResults = underTest.getMeters(MetricsService.Range.FIVE_MINUTES);

        final Map<String, BigDecimal> expectedResults = new HashMap<>();
        expectedResults.put("meter-1", new BigDecimal("10.469"));
        expectedResults.put("meter-2", new BigDecimal("16.692"));
        assertThat(actualResults, is(equalTo(expectedResults)));
    }

    @Test
    public void getMetersShouldRetrieveResultsForTheFifteenMinuteRange() {
        when(meterOneOne.getFifteenMinuteRate()).thenReturn(3.12345);
        when(meterOneTwo.getFifteenMinuteRate()).thenReturn(5.23456);
        when(meterTwoOne.getFifteenMinuteRate()).thenReturn(7.34567);
        when(meterTwoTwo.getFifteenMinuteRate()).thenReturn(11.45678);

        final Map<String,BigDecimal> actualResults = underTest.getMeters(MetricsService.Range.FIFTEEN_MINUTES);

        final Map<String, BigDecimal> expectedResults = new HashMap<>();
        expectedResults.put("meter-1", new BigDecimal("10.469"));
        expectedResults.put("meter-2", new BigDecimal("16.692"));
        assertThat(actualResults, is(equalTo(expectedResults)));
    }

    @Test
    public void getMetersShouldRetrieveResultsForTheMeanRange() {
        when(meterOneOne.getMeanRate()).thenReturn(3.12345);
        when(meterOneTwo.getMeanRate()).thenReturn(5.23456);
        when(meterTwoOne.getMeanRate()).thenReturn(7.34567);
        when(meterTwoTwo.getMeanRate()).thenReturn(11.45678);

        final Map<String,BigDecimal> actualResults = underTest.getMeters(MetricsService.Range.MEAN);

        final Map<String, BigDecimal> expectedResults = new HashMap<>();
        expectedResults.put("meter-1", new BigDecimal("10.469"));
        expectedResults.put("meter-2", new BigDecimal("16.692"));
        assertThat(actualResults, is(equalTo(expectedResults)));
    }

    @Test
    public void getMetersShouldRetrieveResultsForTheCountRange() {
        when(meterOneOne.getCount()).thenReturn(3L);
        when(meterOneTwo.getCount()).thenReturn(5L);
        when(meterTwoOne.getCount()).thenReturn(7L);
        when(meterTwoTwo.getCount()).thenReturn(11L);

        final Map<String,BigDecimal> actualResults = underTest.getMeters(MetricsService.Range.COUNT);

        final Map<String, BigDecimal> expectedResults = new HashMap<>();
        expectedResults.put("meter-1", new BigDecimal("10.000"));
        expectedResults.put("meter-2", new BigDecimal("16.000"));
        assertThat(actualResults, is(equalTo(expectedResults)));
    }

    @Test(expected = NullPointerException.class)
    public void getMetersShouldThrowANullPointerExceptionForANullRange() {
        underTest.getMeters(null);
    }
}
