package com.yazino.metrics.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yazino.metrics.model.Meter;
import com.yazino.metrics.repository.MetricRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MetricParserTest {
    private static final DateTime TIMESTAMP = new DateTime(45883459034756L);

    @Mock
    private MetricRepository repository;

    private MetricParser underTest;

    @Before
    public void setUp() {
        underTest = new MetricParser(repository);
    }

    @Test(expected = NullPointerException.class)
    public void aParserCannotBeCreatedWithANullRepository() {
        new MetricParser(null);
    }

    @Test(expected = NullPointerException.class)
    public void aParserCannotBeCreatedWithANullInputStream() throws IOException {
        underTest.parse(null);
    }

    @Test
    public void aParserCanParseAnEmptyObject() throws IOException {
        underTest.parse(aStreamOf(Collections.<String, Object>emptyMap()));

        verifyZeroInteractions(repository);
    }

    @Test
    public void aParserCanParseAnEmptyMeterBlock() throws IOException {
        underTest.parse(aStreamOf(jsonWithMeters()));

        verifyZeroInteractions(repository);
    }

    @Test(expected = IllegalStateException.class)
    public void aParserWillThrowAnIllegalStateExceptionIfAMeterIsPresentWithoutAClientID() throws IOException {
        final Map<String, Object> message = jsonWithMeters(aMeter("aMeter", 1));
        message.remove("clientId");

        underTest.parse(aStreamOf(message));
    }

    @Test
    public void aParserCanParseASingleMeter() throws IOException {
        underTest.parse(aStreamOf(jsonWithMeters(aMeter("aMeter", 1))));

        verify(repository).save("aMeter", "aSource", aParsedMeter(1));
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void aParserCanParseAMultipleMeters() throws IOException {
        underTest.parse(aStreamOf(jsonWithMeters(aMeter("aMeter", 1), aMeter("anotherMeter", 2))));

        verify(repository).save("aMeter", "aSource", aParsedMeter(1));
        verify(repository).save("anotherMeter", "aSource", aParsedMeter(2));
        verifyNoMoreInteractions(repository);
    }

    private Map<String, Object> aMeter(final String name,
                                       final int base) {
        final Map<String, Object> meter = new HashMap<>();
        meter.put("name", name);
        meter.put("timestamp", TIMESTAMP.getMillis() + base);
        meter.put("count", 10 + base);
        meter.put("m1Rate", base + 0.023);
        meter.put("m5Rate", base + 0.653);
        meter.put("m15Rate", base + 0.342);
        meter.put("meanRate", base + 0.200);
        return meter;
    }

    private Meter aParsedMeter(final int base) {
        return new Meter(new DateTime(TIMESTAMP.getMillis() + base), 10 + base, base + 0.023, base + 0.653, base + 0.342, base + 0.200);
    }

    private Map<String, Object> jsonWithMeters(final Map<String, Object>... meters) {
        final Map<String, Object> json = new HashMap<>();
        if (meters == null || meters.length == 0) {
            json.put("meters", Collections.emptyList());
        } else {
            json.put("meters", asList(meters));
        }
        json.put("clientId", "aSource");
        return json;
    }

    private InputStream aStreamOf(final Map<String, Object> json) throws JsonProcessingException {
        return new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(json));
    }

}
