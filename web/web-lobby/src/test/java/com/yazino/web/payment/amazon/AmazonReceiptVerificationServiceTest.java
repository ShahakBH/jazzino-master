package com.yazino.web.payment.amazon;

import com.yazino.configuration.YazinoConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AmazonReceiptVerificationServiceTest {


    private AmazonReceiptVerificationService underTest;

    @Mock
    private HttpClient client;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    @Before
    public void setUp() throws Exception {
        stub(yazinoConfiguration.getBoolean("amazon.rvs.enabled")).toReturn(true);
        stub(yazinoConfiguration.getString("amazon.rvs.host")).toReturn("https://appstore-sdk.amazon.com");
        stub(yazinoConfiguration.getString("amazon.rvs.developerSecret")).toReturn("yazinoDeveloperSecret");
        underTest = new AmazonReceiptVerificationService(yazinoConfiguration, client);
    }

    @Test
    public void shouldMakeRequestWithGivenParametersAndCurrentConfiguration() throws IOException, URISyntaxException {
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        final HttpResponse httpResponse = aResponseWithCode(200);
        when(client.execute(captor.capture())).thenReturn(httpResponse);
        underTest.verify("aUserId", "aPurchaseToken");
        final HttpGet request = (HttpGet) captor.getValue();
        final URI expectedUrl = new URI("https://appstore-sdk.amazon.com/version/2.0/verify/developer/yazinoDeveloperSecret/user/aUserId/purchaseToken/aPurchaseToken");
        assertEquals(expectedUrl, request.getURI());
    }

    @Test
    public void shouldReturnVerificationResultBasedOnStatusCode() throws IOException {
        serverReturning(200);
        assertEquals(VerificationResult.VALID, underTest.verify("validUserId", "validToken"));
    }

    private void serverReturning(int statusCode) throws IOException {
        final HttpResponse httpResponse = aResponseWithCode(statusCode);
        when(client.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);
    }

    @Test(expected = RuntimeException.class)
    public void shouldHandleIOExceptionDuringHttpCall() throws IOException {
        doThrow(new IOException()).when(client.execute(any(HttpUriRequest.class)));
        underTest.verify("validUserId", "validToken");
    }

    private HttpResponse aResponseWithCode(int statusCode) {
        final HttpResponse mock = Mockito.mock(HttpResponse.class);
        when(mock.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), statusCode, ""));
        return mock;
    }
}
