package com.yazino.metrics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yazino.metrics.aggregation.MetricAggregator;
import com.yazino.metrics.model.Metric;
import com.yazino.metrics.parser.MetricParser;
import com.yazino.metrics.repository.MetricRepository;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MetricControllerTest {
    private static final String REMOTE_ADDRESS = "aRemoteAddress";
    private static final long NOW = 4353425254324L;

    @Mock
    private MetricParser parser;
    @Mock
    private MetricRepository repository;
    @Mock
    private MetricAggregator aggregator;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private ServletInputStream requestInputStream;
    @Mock
    private HttpServletResponse response;
    @Mock
    private PrintWriter responseWriter;

    private MetricController underTest;

    @Before
    public void setUp() throws IOException {
        DateTimeUtils.setCurrentMillisFixed(NOW);

        when(request.getRemoteAddr()).thenReturn(REMOTE_ADDRESS);
        when(request.getInputStream())
                .thenReturn(requestInputStream)
                .thenThrow(new IllegalStateException("getInputStream should not be called twice"));
        when(response.getWriter())
                .thenReturn(responseWriter)
                .thenThrow(new IllegalStateException("getWriter should not be called twice"));

        underTest = new MetricController(parser, repository, aggregator, objectMapper);
    }

    @After
    public void resetJodaTime() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void controllerCannotBeCreatedWithANullParser() {
        new MetricController(null, repository, aggregator, objectMapper);
    }

    @Test(expected = NullPointerException.class)
    public void controllerCannotBeCreatedWithANullRepository() {
        new MetricController(parser, null, aggregator, objectMapper);
    }

    @Test(expected = NullPointerException.class)
    public void controllerCannotBeCreatedWithANullAggregator() {
        new MetricController(parser, repository, null, objectMapper);
    }

    @Test(expected = NullPointerException.class)
    public void controllerCannotBeCreatedWithANullObjectMapper() {
        new MetricController(parser, repository, aggregator, null);
    }

    @Test
    public void postingARequestPassesTheRemoteAddressAndTheInputStreamToTheParser() throws IOException {
        underTest.post(request, response);

        verify(parser).parse(requestInputStream);
    }

    @Test
    public void postingARequestPassesTheCorrectRemoteAddressWhereAXForwardedForHeaderIsPresentToTheParser() throws IOException {
        when(request.getHeader("X-Forwarded-For")).thenReturn("host1,host2, host3");

        underTest.post(request, response);

        verify(parser).parse(requestInputStream);
    }

    @Test
    public void postingARequestReturnsAnEmptyJSONObject() throws IOException {
        underTest.post(request, response);

        verify(responseWriter).write("{}");
        verifyZeroInteractions(objectMapper);
    }

    @Test
    public void postingARequestSetsTheContentTypeToJSON() throws IOException {
        underTest.post(request, response);

        verify(response).setContentType("application/json");
    }

    @Test
    public void postingARequestSetsTheCharacterEncodingToUTF8() throws IOException {
        underTest.post(request, response);

        verify(response).setCharacterEncoding("UTF-8");
    }

    @Test
    public void postingARequestDoesNotDirectlyAccessTheRepository() throws IOException {
        underTest.post(request, response);

        verifyZeroInteractions(repository);
    }

    @Test
    public void gettingAMetricByNameAndSourceReturnsAJSONViewOfTheValueFromTheRepository() throws IOException {
        final Metric metric = mock(Metric.class);
        when(repository.byNameAndSource("aName", "aSource")).thenReturn(metric);

        underTest.getByNameAndSource(response, "aName", "aSource");

        verify(objectMapper).writeValue(responseWriter, metric);
        verifyZeroInteractions(responseWriter);
    }

    @Test
    public void gettingANonExistentMetricByNameAndSourceReturnsAnEmptyJSONObject() throws IOException {
        when(repository.byNameAndSource("aName", "aSource")).thenReturn(null);

        underTest.getByNameAndSource(response, "aName", "aSource");

        verify(responseWriter).write("{}");
        verifyZeroInteractions(objectMapper);
    }

    @Test
    public void gettingAMetricByNameAndSourceSetsTheContentTypeToJSON() throws IOException {
        when(repository.byNameAndSource("aName", "aSource")).thenReturn(mock(Metric.class));

        underTest.getByNameAndSource(response, "aName", "aSource");

        verify(response).setContentType("application/json");
    }

    @Test
    public void gettingAMetricByNameAndSourceSetsTheCharacterEncodingToUTF8() throws IOException {
        when(repository.byNameAndSource("aName", "aSource")).thenReturn(mock(Metric.class));

        underTest.getByNameAndSource(response, "aName", "aSource");

        verify(response).setCharacterEncoding("UTF-8");
    }

    @Test
    public void gettingAMetricByNameReturnsAJSONViewOfTheValueFromTheAggregator() throws IOException {
        final Map<String, Metric> disparateMetrics = aMapOfMetrics();
        final Metric aggregatedMetric = mock(Metric.class);
        when(repository.byName("aName")).thenReturn(disparateMetrics);
        when(aggregator.aggregate(NOW, disparateMetrics)).thenReturn(aggregatedMetric);

        underTest.getByName(response, "aName");

        verify(objectMapper).writeValue(responseWriter, aggregatedMetric);
        verifyZeroInteractions(responseWriter);
    }

    @Test
    public void gettingANonAggregatableMetricByNameReturnsAnEmptyJSONObject() throws IOException {
        final Map<String, Metric> disparateMetrics = aMapOfMetrics();
        when(repository.byName("aName")).thenReturn(disparateMetrics);
        when(aggregator.aggregate(NOW, disparateMetrics)).thenReturn(null);

        underTest.getByName(response, "aName");

        verify(responseWriter).write("{}");
        verifyZeroInteractions(objectMapper);
    }

    @Test
    public void gettingAnAggregateMetricByNameSetsTheContentTypeToJSON() throws IOException {
        final Map<String, Metric> disparateMetrics = aMapOfMetrics();
        final Metric aggregatedMetric = mock(Metric.class);
        when(repository.byName("aName")).thenReturn(disparateMetrics);
        when(aggregator.aggregate(NOW, disparateMetrics)).thenReturn(aggregatedMetric);

        underTest.getByName(response, "aName");

        verify(response).setContentType("application/json");
    }

    @Test
    public void gettingAnAggregateMetricByNameSetsTheCharacterEncodingToUTF8() throws IOException {
        final Map<String, Metric> disparateMetrics = aMapOfMetrics();
        final Metric aggregatedMetric = mock(Metric.class);
        when(repository.byName("aName")).thenReturn(disparateMetrics);
        when(aggregator.aggregate(NOW, disparateMetrics)).thenReturn(aggregatedMetric);

        underTest.getByName(response, "aName");

        verify(response).setCharacterEncoding("UTF-8");
    }

    private Map<String, Metric> aMapOfMetrics() {
        final Map<String, Metric> metrics = new HashMap<>();
        metrics.put("metric1", mock(Metric.class));
        metrics.put("metric2", mock(Metric.class));
        metrics.put("metric3", mock(Metric.class));
        return metrics;
    }

}
