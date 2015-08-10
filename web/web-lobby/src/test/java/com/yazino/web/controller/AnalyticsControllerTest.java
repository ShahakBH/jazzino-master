package com.yazino.web.controller;

import com.yazino.client.log.ClientLogEvent;
import com.yazino.client.log.ClientLogEventMessageType;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.util.WebApiResponses;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class AnalyticsControllerTest {

    public static final String PAYLOAD = "payload";
    private AnalyticsController underTest;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private QueuePublishingService<ClientLogEvent> queueService;


    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100000);
        MockitoAnnotations.initMocks(this);
        underTest = new AnalyticsController(webApiResponses, queueService);
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void recordShouldReturn200OnSuccess() throws IOException {
        underTest.record(request, response, PAYLOAD);
        verify(webApiResponses).writeOk(response, "ok");
    }

    @Test
    public void recordShouldPutClientLogEventOnQueue() throws IOException {
        underTest.record(request, response, PAYLOAD);
        verify(queueService).send(new ClientLogEvent(new DateTime(),"payload", ClientLogEventMessageType.LOG_ANALYTICS));
    }

    @Test
    public void recordShouldReturnErrorIfNoPayload() throws IOException {
        underTest.record(request, response, null);
        verify(webApiResponses).writeError(response, HttpStatus.BAD_REQUEST.value(), "ctx parameter must contain a JSON string");
    }

    @Test
    public void recordShouldHandleIfQueueThrowsException() throws IOException {
        doThrow(new RuntimeException()).when(queueService).send(any(ClientLogEvent.class));
        underTest.record(request, response, PAYLOAD);
        verify(webApiResponses).writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "service is unavailable please try again later");
    }
}
