package com.yazino.payment.worldpay;

import com.google.common.base.Charsets;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.nvp.NVPMessage;
import com.yazino.payment.worldpay.nvp.PaymentTrustAuthorisationMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class STLinkTest {

    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse httpResponse;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private ArgumentCaptor<HttpPost> postCaptor;

    private STLink underTest;

    @Before
    public void setUp() throws IOException {
        when(yazinoConfiguration.getString("payment.worldpay.stlink.gateway")).thenReturn("http://a.gateway/");
        when(yazinoConfiguration.getString("payment.worldpay.stlink.pt.username")).thenReturn("ptUser");
        when(yazinoConfiguration.getString("payment.worldpay.stlink.pt.password")).thenReturn("ptPassword");
        when(yazinoConfiguration.getStringArray("payment.worldpay.stlink.merchant")).thenReturn(new String[]{"10", "20", "30"});
        when(yazinoConfiguration.getInt("payment.worldpay.stlink.merchant.default")).thenReturn(20);
        when(yazinoConfiguration.getInt("payment.worldpay.stlink.merchant.10.GBP.storeid")).thenReturn(100);
        when(yazinoConfiguration.getInt("payment.worldpay.stlink.merchant.10.GBP.storeid.3dsecure", 100)).thenReturn(101);
        when(yazinoConfiguration.getStringArray("payment.worldpay.stlink.merchant.10.currencies")).thenReturn(new String[] {"GBP"});
        when(yazinoConfiguration.getInt("payment.worldpay.stlink.merchant.20.USD.storeid")).thenReturn(200);
        when(yazinoConfiguration.getInt("payment.worldpay.stlink.merchant.20.USD.storeid.3dsecure", 200)).thenReturn(201);
        when(yazinoConfiguration.getStringArray("payment.worldpay.stlink.merchant.20.currencies")).thenReturn(new String[] {"USD"});
        when(yazinoConfiguration.getInt("payment.worldpay.stlink.merchant.30.AUD.storeid")).thenReturn(300);
        when(yazinoConfiguration.getInt("payment.worldpay.stlink.merchant.30.AUD.storeid.3dsecure", 300)).thenReturn(301);
        when(yazinoConfiguration.getInt("payment.worldpay.stlink.merchant.30.NZD.storeid")).thenReturn(310);
        when(yazinoConfiguration.getInt("payment.worldpay.stlink.merchant.30.NZD.storeid.3dsecure", 310)).thenReturn(311);
        when(yazinoConfiguration.getStringArray("payment.worldpay.stlink.merchant.30.currencies")).thenReturn(new String[] {"AUD","NZD"});

        postCaptor = ArgumentCaptor.forClass(HttpPost.class);
        when(httpClient.execute(postCaptor.capture())).thenReturn(httpResponse);
        when(httpResponse.getEntity()).thenReturn(anEntityWithContext("~MerchantId^200161~TransactionType^PT~OrderNumber^5933461527~StrId^803607750~PTTID^503032580~MOP^CC~CurrencyId^826~Amount^56.78~AuthCode^BE5622~RequestType^S~MessageCode^2100~Message^Transaction Approved"));

        underTest = new STLink(httpClient, yazinoConfiguration);
    }

    @Test
    public void merchantsAreReloadedOnAConfigurationChangeEvent() throws IOException {
        reset(yazinoConfiguration);
        when(yazinoConfiguration.getString("payment.worldpay.stlink.gateway")).thenReturn("http://a.gateway/");
        when(yazinoConfiguration.getString("payment.worldpay.stlink.pt.username")).thenReturn("ptUser");
        when(yazinoConfiguration.getString("payment.worldpay.stlink.pt.password")).thenReturn("ptPassword");
        when(yazinoConfiguration.getStringArray("payment.worldpay.stlink.merchant")).thenReturn(new String[]{"500"});
        when(yazinoConfiguration.getInt("payment.worldpay.stlink.merchant.500.GBP.storeid")).thenReturn(5000);
        when(yazinoConfiguration.getStringArray("payment.worldpay.stlink.merchant.500.currencies")).thenReturn(new String[] {"GBP"});

        underTest.configurationChanged(null);

        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "122013")
                .withValue("CurrencyId", 826)
                .withValue("Amount", "56.78");
        underTest.send(authMessage);
        assertThat(postedMessage(), allOf(containsString("~MerchantId^500~"), containsString("~StoreID^5000~")));
    }

    @Test(expected = IllegalStateException.class)
    public void aPaymentInACurrencyWithNoMerchantThrowsAnIllegalStateException() throws IOException {
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "122013")
                .withValue("CurrencyId", 578)
                .withValue("Amount", "56.78");

        underTest.send(authMessage);
    }

    @Test
    public void aGBPPaymentIsGivenTheGBPMerchantIdAndStore() throws IOException {
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "122013")
                .withValue("CurrencyId", 826)
                .withValue("Amount", "56.78");

        underTest.send(authMessage);

        assertThat(postedMessage(), allOf(containsString("~MerchantId^10~"), containsString("~StoreID^100~")));
    }

    @Test
    public void aGBPPaymentWhen3DSecureIsEnabledIsGivenTheGBPMerchantIdAnd3DSecureStore() throws IOException {
        when(yazinoConfiguration.getBoolean("payment.worldpay.stlink.3dsecure.enabled", false)).thenReturn(true);

        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "122013")
                .withValue("CurrencyId", 826)
                .withValue("Amount", "56.78");

        underTest.send(authMessage);

        assertThat(postedMessage(), allOf(containsString("~MerchantId^10~"), containsString("~StoreID^101~")));
    }

    @Test
    public void aUSDPaymentIsGivenTheUSDMerchantIdAndStore() throws IOException {
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "122013")
                .withValue("CurrencyId", 840)
                .withValue("Amount", "56.78");

        underTest.send(authMessage);

        assertThat(postedMessage(), allOf(containsString("~MerchantId^20~"), containsString("~StoreID^200~")));
    }

    @Test
    public void anAUDPaymentIsGivenTheAUDMerchantIdAndStore() throws IOException {
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "122013")
                .withValue("CurrencyId", 36)
                .withValue("Amount", "56.78");

        underTest.send(authMessage);

        assertThat(postedMessage(), allOf(containsString("~MerchantId^30~"), containsString("~StoreID^300~")));
    }

    @Test
    public void aNZDPaymentIsGivenTheNZDMerchantIdAndStore() throws IOException {
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "122013")
                .withValue("CurrencyId", 554)
                .withValue("Amount", "56.78");

        underTest.send(authMessage);

        assertThat(postedMessage(), allOf(containsString("~MerchantId^30~"), containsString("~StoreID^310~")));
    }

    @Test
    public void aNZDPaymentWhen3DSecureIsEnabledIsGivenTheNZDMerchantIdAnd3dSecureStore() throws IOException {
        when(yazinoConfiguration.getBoolean("payment.worldpay.stlink.3dsecure.enabled", false)).thenReturn(true);
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "122013")
                .withValue("CurrencyId", 554)
                .withValue("Amount", "56.78");

        underTest.send(authMessage);

        assertThat(postedMessage(), allOf(containsString("~MerchantId^30~"), containsString("~StoreID^311~")));
    }

    private String postedMessage() throws IOException {
        return toString(postCaptor.getValue().getEntity().getContent());
    }

    private String toString(final InputStream inputStream) throws IOException {
        final Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        if (s.hasNext()) {
            return s.next();
        }
        return "";
    }

    private BasicHttpEntity anEntityWithContext(final String content) {
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(content.getBytes(Charsets.UTF_8)));
        entity.setContentLength(content.length());
        return entity;
    }

}
