package com.yazino.web.payment.creditcard.worldpay;

import com.google.common.base.Optional;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.MessageCode;
import com.yazino.payment.worldpay.NVPResponse;
import com.yazino.payment.worldpay.STLink;
import com.yazino.payment.worldpay.nvp.NVPMessage;
import com.yazino.payment.worldpay.nvp.RedirectQueryOTTMessage;
import com.yazino.web.domain.payment.CardRegistrationResult;
import com.yazino.web.domain.payment.CardRegistrationTokenResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.math.BigDecimal;

import static com.yazino.web.payment.creditcard.worldpay.WorldPayCreditCardRegistrationService.*;
import static java.math.BigDecimal.ONE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class WorldPayCreditCardRegistrationServiceTest {

    public static final String SUCCESSFUL_OTT_RESPONSE = "~MerchantId^200161~TransactionType^RD~OrderNumber^8616616592~StrId^816536720~RDID^6290604~RequestType^G~MessageCode^7050~Message^Request Pending~CustomerId^4B8FFD03-2~OTT^E66DEB6AFA4341B9B91FBFAF2A18549A856D1BA4B88B4BAC845E687DF910BD60~OTTProcessURL^https://ott9.wpstn.com/test/";
    public static final String MISSING_OTT_PROCESS_URL = "~MerchantId^200161~TransactionType^RD~OrderNumber^8616616592~StrId^816536720~RDID^6290604~RequestType^G~MessageCode^7050~Message^Request Pending~CustomerId^4B8FFD03-2~OTT^E66DEB6AFA4341B9B91FBFAF2A18549A856D1BA4B88B4BAC845E687DF910BD60";
    public static final String MISSING_OTT_RESPONSE = "~MerchantId^200161~TransactionType^RD~OrderNumber^8616616592~StrId^816536720~RDID^6290604~RequestType^G~MessageCode^7050~Message^Request Pending~CustomerId^4B8FFD03-2~OTT^~OTTProcessURL^https://ott9.wpstn.com/test/";
    public static final String INFORMATION_ADDED_RESPONSE = "~MerchantId^200161~TransactionType^RD~OrderNumber^8079493168~StrId^816519370~RequestType^A~CustomerId^4B8FFD03-2~CardId^100487182~AcctNumber^********1000~AcctName^John Smith~CreditCardType^MA~IssueCountry^ES~CardIssuer^SISTEMA 4B S.A.~ExpDate^122013~OTT^FBFE954BB43F4770A854976764AED0864AB8F35A1C3E4176AE437B679DB665E6~OTTPostBackURL^~MessageCode^7102~Message^Customer Information Added";
    public static final String UNEXPECTED_MESSAGE_CODE = "~MerchantId^200161~TransactionType^RD~OrderNumber^8079493168~StrId^816519370~RequestType^A~CustomerId^4B8FFD03-2~CardId^100487182~AcctNumber^********1000~AcctName^John Smith~CreditCardType^MA~IssueCountry^ES~CardIssuer^SISTEMA 4B S.A.~ExpDate^122013~OTT^FBFE954BB43F4770A854976764AED0864AB8F35A1C3E4176AE437B679DB665E6~OTTPostBackURL^~MessageCode^9999999~Message^Customer Information Added";
    public static final String NO_MESSAGE_CODE = "~MerchantId^200161~TransactionType^RD~OrderNumber^8079493168~StrId^816519370~RequestType^A~CustomerId^4B8FFD03-2~CardId^100487182~AcctNumber^********1000~AcctName^John Smith~CreditCardType^MA~IssueCountry^ES~CardIssuer^SISTEMA 4B S.A.~ExpDate^122013~OTT^FBFE954BB43F4770A854976764AED0864AB8F35A1C3E4176AE437B679DB665E6~OTTPostBackURL^~MessageCode^~Message^Customer Information Added";
    public static final String MISSING_EXPIRY_DATE_CODE = "~MerchantId^200161~TransactionType^RD~OrderNumber^8079493168~StrId^816519370~RequestType^A~CustomerId^4B8FFD03-2~CardId^100487182~AcctNumber^********1000~AcctName^John Smith~CreditCardType^MA~IssueCountry^ES~CardIssuer^SISTEMA 4B S.A.~ExpDate^122013~OTT^FBFE954BB43F4770A854976764AED0864AB8F35A1C3E4176AE437B679DB665E6~OTTPostBackURL^~MessageCode^7212^~Message^Invalid credit card expiration";
    public static final String ONE_TIME_TOKEN = "ADC288D2D2384E96B057F9AC06609DC463A1105F4B6B4D0FA7B645D542FC7408";
    private static final BigDecimal PLAYER_ID = ONE;
    private static final String FORWARD_URL = "http://xyz.com";
    private static final String WORLD_PAY_PROPERTY_GATEWAY = "https://worldpay/gateway/";
    public static final String OTT_PROCESS_URL = "https://ott9.wpstn.com/test/";

    private WorldPayCreditCardRegistrationService underTest;

    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private STLink stLink;

    @Before
    public void setup() {
        initMocks(this);
        underTest = new WorldPayCreditCardRegistrationService(yazinoConfiguration, stLink);
    }


    /* prepareCardRegistration */
    @Test
    public void prepareCardRegistrationShouldContainWorldPayResponse() {
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_TEST_MODE, false)).thenReturn(false);
        when(yazinoConfiguration.getString(PROPERTY_WORLDPAY_GATEWAY)).thenReturn(WORLD_PAY_PROPERTY_GATEWAY);
        when(stLink.send(any(NVPMessage.class))).thenReturn(new NVPResponse("", SUCCESSFUL_OTT_RESPONSE));

        Optional<CardRegistrationTokenResult> result = underTest.prepareCardRegistration(PLAYER_ID, FORWARD_URL);

        assertThat("Result is present", result.isPresent());
        assertThat("Result is not in test mode", !result.get().isTest());
        assertThat("OTT matches NVPResponse", result.get().getToken(), is(equalTo("E66DEB6AFA4341B9B91FBFAF2A18549A856D1BA4B88B4BAC845E687DF910BD60")));
        assertThat("OTTProcessURL matches NVPResponse", result.get().getRegistrationURL(), is(equalTo("https://ott9.wpstn.com/test/")));
    }

    @Test
    public void prepareCardRegistrationShouldReturnAbsentIfWorldPayIsNotEnabled() {
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(false);

        Optional<CardRegistrationTokenResult> result = underTest.prepareCardRegistration(PLAYER_ID, FORWARD_URL);

        assertThat("Card registration result is absent", !result.isPresent());
    }

    @Test
    public void prepareCardRegistrationShouldNotBeAbsentIfWorldPayIsEnabled() {
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(stLink.send(any(NVPMessage.class))).thenReturn(new NVPResponse("", SUCCESSFUL_OTT_RESPONSE));

        Optional<CardRegistrationTokenResult> result = underTest.prepareCardRegistration(PLAYER_ID, FORWARD_URL);

        assertThat("Card registration token result is present", result.isPresent());
    }

    @Test
    public void prepareCardRegistrationShouldNotIncludeTestIfTestModeIsDisabled() {
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_TEST_MODE, false)).thenReturn(false);
        when(stLink.send(any(NVPMessage.class))).thenReturn(new NVPResponse("", SUCCESSFUL_OTT_RESPONSE));

        Optional<CardRegistrationTokenResult> result = underTest.prepareCardRegistration(PLAYER_ID, FORWARD_URL);

        assertThat("Card registration token result is not present", result.isPresent());
        assertThat("Registration should not be in test mode", !result.get().isTest());
    }

    @Test
    public void prepareCardRegistrationShouldIncludeTestIfTestModeEnabled() {
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_TEST_MODE, false)).thenReturn(true);
        when(yazinoConfiguration.getString(PROPERTY_WORLDPAY_TEST_GATEWAY)).thenReturn(OTT_PROCESS_URL);
        when(stLink.send(any(NVPMessage.class))).thenReturn(new NVPResponse("", SUCCESSFUL_OTT_RESPONSE));

        Optional<CardRegistrationTokenResult> result = underTest.prepareCardRegistration(PLAYER_ID, FORWARD_URL);

        assertThat("Card registration token result is present", result.isPresent());
        assertThat("WorldPay should be in test mode", result.get().isTest());
    }

    @Test
    public void prepareCardRegistrationShouldIncludeTestGatewayIfInTestMode() {
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_TEST_MODE, false)).thenReturn(true);
        when(stLink.send(any(NVPMessage.class))).thenReturn(new NVPResponse("", SUCCESSFUL_OTT_RESPONSE));

        underTest.prepareCardRegistration(PLAYER_ID, FORWARD_URL);

        verify(yazinoConfiguration, times(1)).getString(PROPERTY_WORLDPAY_TEST_GATEWAY);
    }

    @Test
    public void prepareCardRegistrationShouldIncludeWorldPayGatewayWhenNotInTestMode() {
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_TEST_MODE, false)).thenReturn(false);
        final NVPResponse response = new NVPResponse("", SUCCESSFUL_OTT_RESPONSE);
        when(stLink.send(any(NVPMessage.class))).thenReturn(response);

        Optional<CardRegistrationTokenResult> tokeResult = underTest.prepareCardRegistration(PLAYER_ID, FORWARD_URL);

        assertThat("Ott process URL is from the response", tokeResult.get().getRegistrationURL(), is(equalTo("https://ott9.wpstn.com/test/")));
    }

    @Test
    public void prepareCardRegistrationShouldHandleMissingOneTimeToken() {
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_TEST_MODE, false)).thenReturn(false);
        when(stLink.send(any(NVPMessage.class))).thenReturn(new NVPResponse("", MISSING_OTT_RESPONSE));

        Optional<CardRegistrationTokenResult> result = underTest.prepareCardRegistration(PLAYER_ID, FORWARD_URL);

        assertThat("Missing OTT returns absent result", !result.isPresent());
    }

    @Test
    public void prepareCardRegistrationShouldReturnAbsentWhenUnableToGetOTTPProcessURL() {
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_TEST_MODE, false)).thenReturn(false);
        when(stLink.send(any(NVPMessage.class))).thenReturn(new NVPResponse("", MISSING_OTT_PROCESS_URL));

        Optional<CardRegistrationTokenResult> result = underTest.prepareCardRegistration(PLAYER_ID, FORWARD_URL);

        assertThat("Missing OTT Process URL returns absent result", !result.isPresent());
    }



    /* retrieveCardRegistrationResult */
    @Test
    public void retrieveCardRegistrationResultShouldReturnAllCardDetailsOnSuccess() {
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_TEST_MODE, false)).thenReturn(false);
        final NVPResponse nvpResponse = new NVPResponse("", INFORMATION_ADDED_RESPONSE);
        when(stLink.send(any(NVPMessage.class))).thenReturn(nvpResponse);

        CardRegistrationResult registrationResult = underTest.retrieveCardRegistrationResult(ONE_TIME_TOKEN);

        MessageCode messageCode = MessageCode.forCode(registrationResult.getMessageCode());
        assertThat("Obscured card number matches NVPResponse", registrationResult.getObscuredCardNumber(), is(equalTo("********1000")));
        assertThat("Customer Id matches NVPResponse", registrationResult.getCustomerId(), is(equalTo("4B8FFD03-2")));
        assertThat("Expiry month matches NVPResponse", registrationResult.getExpiryMonth(), is(equalTo("12")));
        assertThat("Expiry year matches NVPResponse", registrationResult.getExpiryYear(), is(equalTo("2013")));
        assertThat("Account name matches NVPResponse", registrationResult.getCustomerName(), is(equalTo("John Smith")));
        assertThat("Card Id matches NVPResponse", registrationResult.getCardId(), is(equalTo("100487182")));
        assertThat("Message Code matches NVPResponse", messageCode, is(equalTo(MessageCode.CUSTOMER_INFORMATION_ADDED)));
    }

    @Test
    public void retrieveCardRegistrationResultShouldAddTestParameterToNVPMessageIfConfigurationIsInTestMode() {
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_TEST_MODE, false)).thenReturn(true);
        final NVPResponse nvpResponse = new NVPResponse("", INFORMATION_ADDED_RESPONSE);
        when(stLink.send(any(NVPMessage.class))).thenReturn(nvpResponse);

        underTest.retrieveCardRegistrationResult(ONE_TIME_TOKEN);

        verify(stLink).send(new RedirectQueryOTTMessage().withValue(IS_TEST_PARAMETER, 1).withValue(ONE_TIME_TOKEN_PARAMETER, ONE_TIME_TOKEN));
    }

    @Test
    public void retrieveCardRegistrationResultShouldNotAddTestParameterToNVPMessageIfConfigurationIsNotInTestMode() {
        when(yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_TEST_MODE, false)).thenReturn(false);
        final NVPResponse nvpResponse = new NVPResponse("", INFORMATION_ADDED_RESPONSE);
        when(stLink.send(any(NVPMessage.class))).thenReturn(nvpResponse);

        underTest.retrieveCardRegistrationResult(ONE_TIME_TOKEN);

        verify(stLink).send(new RedirectQueryOTTMessage().withValue(ONE_TIME_TOKEN_PARAMETER, ONE_TIME_TOKEN));
    }

    @Test
    public void retrieveCardRegistrationResultShouldWorldPayRequestPendingResponse() {
        when(stLink.send(any(NVPMessage.class))).thenReturn(new NVPResponse("", INFORMATION_ADDED_RESPONSE));

        underTest.retrieveCardRegistrationResult(ONE_TIME_TOKEN);

        verify(stLink, times(1)).send(any(NVPMessage.class));
    }

    @Test(expected = IllegalStateException.class)
    public void retrieveCardRegistrationResultShouldThrowIllegalStateExceptionWhenUnexpectedMessageCodeIsReturned() {
        when(stLink.send(any(NVPMessage.class))).thenReturn(new NVPResponse("", UNEXPECTED_MESSAGE_CODE));

        underTest.retrieveCardRegistrationResult(ONE_TIME_TOKEN);
    }

    @Test(expected = IllegalStateException.class)
    public void retrieveCardRegistrationResultShouldThrowIllegalStateExceptionWhenNoMessageCodeIsReturned() {
        when(stLink.send(any(NVPMessage.class))).thenReturn(new NVPResponse("", NO_MESSAGE_CODE));

        underTest.retrieveCardRegistrationResult(ONE_TIME_TOKEN);
    }

    @Test
    public void retrieveCardRegistrationResultShouldContainOnlyTheMessageIfNotSuccessful() {
         when(stLink.send(any(NVPMessage.class))).thenReturn(new NVPResponse("", MISSING_EXPIRY_DATE_CODE));

        CardRegistrationResult registrationResult = underTest.retrieveCardRegistrationResult(ONE_TIME_TOKEN);

        MessageCode messageCode = MessageCode.forCode(registrationResult.getMessageCode());
        assertThat("Message Code matches NVPResponse", messageCode, is(equalTo(MessageCode.INVALID_CARD_EXPIRY_DATE)));
        assertThat("NVPResponse does not contain obscured card number", registrationResult.getObscuredCardNumber(), is(nullValue()));
        assertThat("NVPResponse does not contain card ID", registrationResult.getCardId(), is(nullValue()));
        assertThat("NVPResponse does not contain customer ID", registrationResult.getCustomerId(), is(nullValue()));
        assertThat("NVPResponse does not contain customer name", registrationResult.getCustomerName(), is(nullValue()));
        assertThat("NVPResponse does not contain expiry year", registrationResult.getExpiryYear(), is(nullValue()));
        assertThat("NVPResponse does not contain expiry month", registrationResult.getExpiryMonth(), is(nullValue()));
    }

}
