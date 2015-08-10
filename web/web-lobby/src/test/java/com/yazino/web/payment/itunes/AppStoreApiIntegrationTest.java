package com.yazino.web.payment.itunes;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AppStoreApiIntegrationTest {

    private static final String FAILURE_RESPONSE = "{\"status\":21002, \"exception\":\"java.lang.IllegalArgumentException\"}";
    private static final String SUCCESS_RESPONSE = "{\"receipt\":{\"original_purchase_date_pst\":\"2012-08-06 06:15:53 America/Los_Angeles\", \"unique_identifier\":\"56aaf561cd581f025bb941d405c737df61890882\", \"original_transaction_id\":\"1000000054050596\", \"bvrs\":\"3.0-beta1\", \"transaction_id\":\"1000000054050596\", \"quantity\":\"1\", \"product_id\":\"USD3_BUYS_5K\", \"item_id\":\"547570224\", \"purchase_date_ms\":\"1344258953722\", \"purchase_date\":\"2012-08-06 13:15:53 Etc/GMT\", \"original_purchase_date\":\"2012-08-06 13:15:53 Etc/GMT\", \"purchase_date_pst\":\"2012-08-06 06:15:53 America/Los_Angeles\", \"bid\":\"yazino.WheelDeal\", \"original_purchase_date_ms\":\"1344258953722\"}, \"status\":0}";

    private final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    private final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    private final AppStoreApiIntegration underTest = new AppStoreApiIntegration();

    @Test
    public void shouldReturnResponseWithFailedStatusAndNoReceiptWhenAppleFails() throws Exception {
        setupClientToReturnResponse(FAILURE_RESPONSE);
        AppStoreOrder order = underTest.retrieveOrder("12345", httpClient);
        assertEquals(21002, order.getStatusCode());
    }

    @Test
    public void shouldReturnResponseWithSuccessStatusAndReceiptWhenAppleSucceeds() throws Exception {
        setupClientToReturnResponse(SUCCESS_RESPONSE);
        AppStoreOrder order = underTest.retrieveOrder("12345", httpClient);
        assertEquals(0, order.getStatusCode());
        assertEquals("USD3_BUYS_5K", order.getProductId());
        assertEquals("1000000054050596", order.getOrderId());
    }

    @Test
    public void shouldReturnResponseWithOriginalReceiptWhenAppleFails() throws Exception {
        setupClientToReturnResponse(FAILURE_RESPONSE);
        AppStoreOrder order = underTest.retrieveOrder("12345", httpClient);
        String message = order.getMessage();
        assertTrue(message.contains(FAILURE_RESPONSE.replaceAll(", ", ","))); // apple response contains spaces whereas when we serialize we dont
        assertTrue(message.contains("12345"));
    }

    @Test
    public void shouldReturnResponseWithOriginalReceiptWhenAppleSucceeds() throws Exception {
        setupClientToReturnResponse(SUCCESS_RESPONSE);
        AppStoreOrder order = underTest.retrieveOrder("12345", httpClient);
        assertTrue(order.getMessage().contains(SUCCESS_RESPONSE.replaceAll(", ", ",")));
        assertTrue(order.getMessage().contains("12345"));
    }

    @Test
    public void shouldShutDownClientManagerFactoryOnSuccessResponse() throws Exception {
        setupClientToReturnResponse(SUCCESS_RESPONSE);
        underTest.retrieveOrder("12345", httpClient);
        verify(httpClient).close();
    }

    @Test
    public void shouldShutDownClientManagerFactoryOnFailureResponse() throws Exception {
        setupClientToReturnResponse(FAILURE_RESPONSE);
        underTest.retrieveOrder("12345", httpClient);
        verify(httpClient).close();
    }

    @Test
    public void shouldShutDownClientManagerFactoryWhenConnectionFailure() throws IOException {
        when(httpClient.execute(any(HttpPost.class))).thenThrow(new IOException());
        try {
            underTest.retrieveOrder("12345", httpClient);
        } catch (IOException e) {
            // ignore
        }
        verify(httpClient).close();
    }

    private void setupClientToReturnResponse(String responseBody) throws IOException {
        when(httpClient.execute(any(HttpPost.class))).thenReturn(this.response);
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(responseBody.getBytes("UTF-8")));
        when(this.response.getEntity()).thenReturn(entity);

    }

}
