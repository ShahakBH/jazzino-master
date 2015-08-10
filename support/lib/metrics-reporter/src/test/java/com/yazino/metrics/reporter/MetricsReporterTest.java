package com.yazino.metrics.reporter;

import com.codahale.metrics.*;
import com.yazino.configuration.YazinoConfiguration;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sun.management.VMManagement;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MetricsReporterTest {
    private static final long TIMESTAMP = 12332434543543L;

    @Mock
    private MetricsClient client;
    @Mock
    private MetricRegistry registry;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private MetricsReporter underTest;

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(TIMESTAMP);

        underTest = new MetricsReporter(registry, client, yazinoConfiguration);
    }

    @After
    public void resetJodaTime() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void clientCannotBeCreatedWithANullMetricsClient() {
        new MetricsReporter(registry, null, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void clientCannotBeCreatedWithANullYazinoConfiguration() {
        new MetricsReporter(registry, client, null);
    }

    @Test
    public void anEmptyReportIsNotSent() {
        underTest.report(emptySortedMap(Gauge.class),
                emptySortedMap(Counter.class),
                emptySortedMap(Histogram.class),
                emptySortedMap(Meter.class),
                emptySortedMap(Timer.class));

        verifyZeroInteractions(client);
    }

    @Test
    public void reportingIgnoresGauges() {
        underTest.report(aDummyMapOf(Gauge.class),
                emptySortedMap(Counter.class),
                emptySortedMap(Histogram.class),
                emptySortedMap(Meter.class),
                emptySortedMap(Timer.class));

        verifyZeroInteractions(client);
    }

    @Test
    public void reportingIgnoresCounters() {
        underTest.report(emptySortedMap(Gauge.class),
                aDummyMapOf(Counter.class),
                emptySortedMap(Histogram.class),
                emptySortedMap(Meter.class),
                emptySortedMap(Timer.class));

        verifyZeroInteractions(client);
    }

    @Test
    public void reportingIgnoresHistogram() {
        underTest.report(emptySortedMap(Gauge.class),
                emptySortedMap(Counter.class),
                aDummyMapOf(Histogram.class),
                emptySortedMap(Meter.class),
                emptySortedMap(Timer.class));

        verifyZeroInteractions(client);
    }

    @Test
    public void reportingIgnoresTimers() {
        underTest.report(emptySortedMap(Gauge.class),
                emptySortedMap(Counter.class),
                emptySortedMap(Histogram.class),
                emptySortedMap(Meter.class),
                aDummyMapOf(Timer.class));

        verifyZeroInteractions(client);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void exceptionsGeneratedDuringReportingAreNotPropagated() {
        doThrow(new RuntimeException("aTestException")).when(client).send(anyMap());

        underTest.report(emptySortedMap(Gauge.class),
                emptySortedMap(Counter.class),
                emptySortedMap(Histogram.class),
                aMapOfMeters(),
                emptySortedMap(Timer.class));
    }

    @Test
    public void reportingSendsJSONRepresentationOfMeters() throws UnknownHostException {
        final Map<String, Object> expectedMeters = new HashMap<>();
        expectedMeters.put("meters", asList(
                aJsonMeterWith("name1", 1, 1.1, 1.5, 1.15, 1.2),
                aJsonMeterWith("name2", 2, 2.1, 2.5, 2.15, 2.2),
                aJsonMeterWith("name3", 3, 3.1, 3.5, 3.15, 3.2)));
        expectedMeters.put("clientId", String.format("%s:%d", InetAddress.getLocalHost().getHostName(), processId()));

        underTest.report(emptySortedMap(Gauge.class),
                emptySortedMap(Counter.class),
                emptySortedMap(Histogram.class),
                aMapOfMeters(),
                emptySortedMap(Timer.class));

        verify(client).send(expectedMeters);
    }

    @Test
    public void aNullReportIsNotSent() {
        underTest.report(null, null, null, null, null);

        verifyZeroInteractions(client);
    }

    private <T extends Metric> SortedMap<String, T> emptySortedMap(Class<T> className) {
        return new TreeMap<>();
    }

    private <T extends Metric> SortedMap<String, T> aDummyMapOf(Class<T> className) {
        final TreeMap<String, T> map = new TreeMap<>();
        map.put("name1", mock(className));
        map.put("name2", mock(className));
        map.put("name3", mock(className));
        return map;
    }

    private SortedMap<String, Meter> aMapOfMeters() {
        final TreeMap<String, Meter> map = new TreeMap<>();
        map.put("name1", aMeterWith(1, 1.1, 1.5, 1.15, 1.2));
        map.put("name2", aMeterWith(2, 2.1, 2.5, 2.15, 2.2));
        map.put("name3", aMeterWith(3, 3.1, 3.5, 3.15, 3.2));
        return map;
    }

    private Meter aMeterWith(final long count,
                             final double m1Rate,
                             final double m5Rate,
                             final double m15Rate,
                             final double meanRate) {
        final Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(count);
        when(meter.getOneMinuteRate()).thenReturn(m1Rate);
        when(meter.getFiveMinuteRate()).thenReturn(m5Rate);
        when(meter.getFifteenMinuteRate()).thenReturn(m15Rate);
        when(meter.getMeanRate()).thenReturn(meanRate);
        return meter;
    }

    private Map<String, Object> aJsonMeterWith(final String name,
                                               final long count,
                                               final double m1Rate,
                                               final double m5Rate,
                                               final double m15Rate,
                                               final double meanRate) {
        final Map<String, Object> meter = new HashMap<>();
        meter.put("name", name);
        meter.put("timestamp", TIMESTAMP);
        meter.put("count", count);
        meter.put("m1Rate", m1Rate);
        meter.put("m5Rate", m5Rate);
        meter.put("m15Rate", m15Rate);
        meter.put("meanRate", meanRate);
        return meter;
    }

    private int processId() {
        try {
            final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            final Field jvm = runtime.getClass().getDeclaredField("jvm");
            jvm.setAccessible(true);
            final VMManagement management = (VMManagement) jvm.get(runtime);
            java.lang.reflect.Method pidMethod = management.getClass().getDeclaredMethod("getProcessId");
            pidMethod.setAccessible(true);
            return (Integer) pidMethod.invoke(management);

        } catch (Exception e) {
            return -1;
        }
    }

}
