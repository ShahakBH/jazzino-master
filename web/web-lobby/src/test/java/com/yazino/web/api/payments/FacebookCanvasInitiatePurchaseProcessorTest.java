package com.yazino.web.api.payments;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Platform;
import com.yazino.web.payment.TransactionIdGenerator;
import com.yazino.web.payment.facebook.FacebookCanvasInitiatePurchaseProcessor;
import com.yazino.web.payment.facebook.FacebookInitiatePaymentResponse;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;

public class FacebookCanvasInitiatePurchaseProcessorTest {
    public static final String OG_HOST = "http://payment/og/host";
    public static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    public static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(638756l);
    public static final String PRODUCT_ID = "productId";
    public static final long PROMOTION_ID = 123l;
    public static final String GAME_TYPE = "gameType";
    private static final long TRANSACTION_ID = 23344L;

    FacebookCanvasInitiatePurchaseProcessor underTest;

    @Mock
    TransactionIdGenerator transactionIdGenerator;
    @Mock
    YazinoConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(configuration.getString("facebook.openGraphObjectsHost")).thenReturn(OG_HOST);
        when(transactionIdGenerator.generateNumericTransactionId()).thenReturn(TRANSACTION_ID);

        underTest = new FacebookCanvasInitiatePurchaseProcessor(transactionIdGenerator, configuration);

    }

    @Test
    public void testInitiatePurchase() throws Exception {
        final FacebookInitiatePaymentResponse actual = (FacebookInitiatePaymentResponse) underTest.initiatePurchase(PLAYER_ID,
                                                                                                                    PRODUCT_ID,
                                                                                                                    PROMOTION_ID,
                                                                                                                    GAME_TYPE,
                                                                                                                    Platform.FACEBOOK_CANVAS);

        assertThat(actual.getProductUrl(), CoreMatchers.is("http://payment/og/host/fbog/product/productId"));
        assertThat(actual.getRequestId(), CoreMatchers.is(String.valueOf(TRANSACTION_ID)));
    }

    @Test
    public void testGetPlatform() throws Exception {
        assertThat(Platform.FACEBOOK_CANVAS, is(equalTo(underTest.getPlatform())));
    }

    @Test
    public void shouldWriteProductUrl() throws IOException {

        when(configuration.getString("facebook.openGraphObjectsHost")).thenReturn(OG_HOST);
        ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);

        final FacebookInitiatePaymentResponse actual = (FacebookInitiatePaymentResponse) underTest.initiatePurchase(PLAYER_ID,
                                                                                                                    PRODUCT_ID,
                                                                                                                    PROMOTION_ID,
                                                                                                                    GAME_TYPE,
                                                                                                                    Platform.FACEBOOK_CANVAS);

    }
}
