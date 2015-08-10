package com.yazino.metrics.reporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yazino.configuration.YazinoConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MetricsClientTest {
    private static final String JSON_MESSAGE = "aJsonMessage";

    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private HttpClient httpClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private HttpResponse response;
    @Mock
    private StatusLine statusLine;

    private MetricsClient underTest;

    @Before
    public void setUp() throws IOException {
        when(yazinoConfiguration.getString("metrics.aggregation.server", "http://localhost:7900/metrics/metrics"))
                .thenReturn("http://aServer:123/metrics");
        when(objectMapper.writeValueAsString(aMessage())).thenReturn(JSON_MESSAGE);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);

        underTest = new MetricsClient(httpClient, objectMapper, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void clientCannotBeCreatedWithANullHttpClient() {
        new MetricsClient(null, objectMapper, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void clientCannotBeCreatedWithANullObjectMapper() {
        new MetricsClient(httpClient, null, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void clientCannotBeCreatedWithANullYazinoConfiguration() {
        new MetricsClient(httpClient, objectMapper, null);
    }

    @Test
    public void sendingANullMapResultsInNoActionBeingTaken() {
        underTest.send(null);

        verifyZeroInteractions(yazinoConfiguration, httpClient, objectMapper);
    }

    @Test
    public void sendingAnEmptyMapResultsInNoActionBeingTaken() {
        underTest.send(Collections.<String, Object>emptyMap());

        verifyZeroInteractions(yazinoConfiguration, httpClient, objectMapper);
    }

    @Test
    public void exceptionsGeneratedWhileSendingAreNotPropagated() throws IOException {
        when(httpClient.execute(any(HttpPost.class))).thenThrow(new IOException("aTestException"));

        underTest.send(aMessage());
    }

    @Test
    public void sendingAMapPostsTheJSONRepresentationToTheServer() throws IOException {
        underTest.send(aMessage());

        final ArgumentCaptor<HttpPost> postCaptor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClient).execute(postCaptor.capture());

        final HttpPost post = postCaptor.getValue();
        assertThat(post.getEntity(), is(not(nullValue())));
        assertThat(EntityUtils.toString(post.getEntity()), is(equalTo(JSON_MESSAGE)));
    }

    @Test
    public void sendingAMapPostsAContentTypeOfJSON() throws IOException {
        underTest.send(aMessage());

        final ArgumentCaptor<HttpPost> postCaptor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClient).execute(postCaptor.capture());

        final HttpPost post = postCaptor.getValue();
        assertThat(post.getEntity(), is(not(nullValue())));
        assertThat(post.getEntity().getContentType().getValue(), is(equalTo("application/json; charset=UTF-8")));
    }

    @Test
    public void sendingAMapPostsToTheRemoteServer() throws IOException {
        underTest.send(aMessage());

        final ArgumentCaptor<HttpPost> postCaptor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClient).execute(postCaptor.capture());

        final HttpPost post = postCaptor.getValue();

        assertThat(post.getURI(), is(equalTo(URI.create("http://aServer:123/metrics"))));
        assertThat(post.getEntity().getContentType().getValue(), is(equalTo("application/json; charset=UTF-8")));
    }

    private Map<String, Object> aMessage() {
        final Map<String, Object> message = new HashMap<>();
        message.put("name1", "value1");
        message.put("name2", "value2");
        return message;
    }

}
