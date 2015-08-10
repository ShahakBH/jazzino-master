package com.yazino.web.payment.creditcard.worldpay;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.STLink;
import com.yazino.payment.worldpay.nvp.NVPMessage;
import com.yazino.payment.worldpay.nvp.RedirectQueryMessage;
import com.yazino.web.domain.payment.RegisteredCardQueryResult;
import com.yazino.web.domain.payment.RegisteredCreditCardDetails;
import com.yazino.web.domain.payment.RegisteredCreditCardDetailsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.math.BigDecimal;

import static java.math.BigDecimal.ONE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;


public class WorldPayCreditCardQueryServiceTest {
    public static final BigDecimal PLAYER_ID = ONE;
    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private STLink stLink;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        underTest = new WorldPayCreditCardQueryService(yazinoConfiguration, stLink);
    }

    private WorldPayCreditCardQueryService underTest;

    @Test
    public void shouldRetrieveCardsForPlayer() throws Exception {
        when(yazinoConfiguration.getBoolean(WorldPayCreditCardQueryService.PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(stLink.sendWithoutParsing(any(NVPMessage.class))).thenReturn(XML_RESPONSE_MULTIPLE_CARDS);

        RegisteredCardQueryResult registeredCardQueryResult = underTest.retrieveCardsFor(PLAYER_ID);

        assertThat("There are two credit cards", registeredCardQueryResult.getCreditCardDetailList(), hasSize(2));
        assertThat("Retrieved cards contain expected information", registeredCardQueryResult.getCreditCardDetailList(), hasItems(
                builder("2009876", "********0019", "John Smith", "022020", "GB", "National Bank", "MA", "0"),
                builder("2009877", "********4455", "John Smith", "022014", "GB", "Nationwide", "MA", "1"))
        );
    }

    @Test
    public void shouldRetrieveCardsForPlayerWhoHasOneCard() throws Exception {
        when(yazinoConfiguration.getBoolean(WorldPayCreditCardQueryService.PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(stLink.sendWithoutParsing(any(NVPMessage.class))).thenReturn(XML_RESPONSE_SINGLE_CARD);

        RegisteredCardQueryResult registeredCardQueryResult = underTest.retrieveCardsFor(PLAYER_ID);

        assertThat("There is one credit cards", registeredCardQueryResult.getCreditCardDetailList(), hasSize(1));
        assertThat("Retrieved cards contain expected information", registeredCardQueryResult.getCreditCardDetailList(), hasItems(
                builder("2009876", "********0019", "John Smith", "022020", "GB", "National Bank", "MA", "0"))
        );
    }

    @Test
    public void shouldRetrieveCardsForPlayerWhoHasOneCardWithLessInformation() throws Exception {
        when(yazinoConfiguration.getBoolean(WorldPayCreditCardQueryService.PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(stLink.sendWithoutParsing(any(NVPMessage.class))).thenReturn(ONE_CARD_LESS_INFORMATION_RESPONSE);

        RegisteredCardQueryResult registeredCardQueryResult = underTest.retrieveCardsFor(PLAYER_ID);

        assertThat("There is one credit cards", registeredCardQueryResult.getCreditCardDetailList(), hasSize(1));
        assertThat("Retrieved cards contain expected information", registeredCardQueryResult.getCreditCardDetailList(), hasItems(
                builder("100509566", "********0000", "John Smith", "122013", null, null, "VI", "1"))
        );
    }


    @Test
    public void shouldRetrieveNoCardsForPlayerWhoDoesNotExist() throws Exception {
        when(yazinoConfiguration.getBoolean(WorldPayCreditCardQueryService.PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(stLink.sendWithoutParsing(any(NVPMessage.class))).thenReturn(CUSTOMER_DOES_NOT_EXIST);

        RegisteredCardQueryResult registeredCardQueryResult = underTest.retrieveCardsFor(PLAYER_ID);

        assertThat("There is one credit cards", registeredCardQueryResult.getCreditCardDetailList(), hasSize(0));
    }

    @Test
    public void shouldRetrieveNoCardsForPlayer() throws Exception {
        when(yazinoConfiguration.getBoolean(WorldPayCreditCardQueryService.PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(stLink.sendWithoutParsing(any(NVPMessage.class))).thenReturn(XML_RESPONSE_NO_CARDS);

        RegisteredCardQueryResult registeredCardQueryResult = underTest.retrieveCardsFor(PLAYER_ID);

        assertThat("Retrieved cards contain expected information", registeredCardQueryResult.getCreditCardDetailList(), hasSize(0));
    }

    @Test
    public void worldPayCreditCardQueryServiceShouldReturnNoCardsWhenWorldPayIsDisabled() throws Exception {

        RegisteredCardQueryResult registeredCardQueryResult = underTest.retrieveCardsFor(PLAYER_ID);

        assertThat("Cards are empty", registeredCardQueryResult.getCreditCardDetailList(), hasSize(0));
    }

    @Test
    public void worldPayCreditCardQueryServiceShouldHaveTestVariableIfInTestMode() throws Exception {
        when(yazinoConfiguration.getBoolean(WorldPayCreditCardQueryService.PROPERTY_WORLDPAY_ENABLED, false)).thenReturn(true);
        when(yazinoConfiguration.getBoolean(WorldPayCreditCardQueryService.PROPERTY_WORLDPAY_TEST_MODE, false)).thenReturn(true);
        when(stLink.sendWithoutParsing(any(NVPMessage.class))).thenReturn(XML_RESPONSE_MULTIPLE_CARDS);

        underTest.retrieveCardsFor(PLAYER_ID);

        verify(stLink, times(1)).sendWithoutParsing(new RedirectQueryMessage().withValue("CustomerId", PLAYER_ID).withValue("IsTest", 1));
    }

    private RegisteredCreditCardDetails builder(final String cardId,
                                                final String obscuredNumber,
                                                final String accountName,
                                                final String expiryDate,
                                                final String issueCountry,
                                                final String cardIssuer,
                                                final String creditCardType,
                                                final String aDefault) {
        return RegisteredCreditCardDetailsBuilder.valueOf()
                .withCardId(cardId)
                .withAccountName(accountName)
                .withObscuredNumber(obscuredNumber)
                .withExpiryDate(expiryDate)
                .withIssueCountry(issueCountry)
                .withCardIssuer(cardIssuer)
                .withCreditCardType(creditCardType)
                .withIsDefault(aDefault).build();
    }

    private static final String XML_RESPONSE_MULTIPLE_CARDS = "<?xml version=\"1.0\"?>\n" +
            "<TMSTN>\n" +
            "<MerchantId>100000</MerchantId>\n" +
            "<TransactionType>RD</TransactionType>\n" +
            "<OrderNumber>6663889074</OrderNumber>\n" +
            "<StrId>301298824</StrId>\n" +
            "<RequestType>Q</RequestType>\n" +
            "<CustomerId>1007865</CustomerId>\n" +
            "<Card>\n" +
                "<CardId>2009876</CardId>\n" +
                "<AcctNumber>********0019</AcctNumber>\n" +
                "<AcctName>John Smith</AcctName>\n" +
                "<CreditCardType>MA</CreditCardType>\n" +
                "<IssueCountry>GB</IssueCountry>\n" +
                "<CardIssuer>National Bank</CardIssuer>\n" +
                "<ExpDate>022020</ExpDate>\n" +
                "<IssueNumber>1</IssueNumber>\n" +
                "<StartDate>022005</StartDate>\n" +
                "<IsDefault>0</IsDefault>\n" +
            "</Card>\n" +
            "<Card>\n" +
                "<CardId>2009877</CardId>\n" +
                "<AcctNumber>********4455</AcctNumber>\n" +
                "<AcctName>John Smith</AcctName>\n" +
                "<CreditCardType>MA</CreditCardType>\n" +
                "<IssueCountry>GB</IssueCountry>\n" +
                "<CardIssuer>Nationwide</CardIssuer>\n" +
                "<ExpDate>022014</ExpDate>\n" +
                "<IssueNumber>1</IssueNumber>\n" +
                "<StartDate>022005</StartDate>\n" +
                "<IsDefault>1</IsDefault>\n" +
            "</Card>\n" +
            "<MessageCode>7100</MessageCode>\n" +
            "<Message>Transaction Approved</Message>\n" +
            "</TMSTN>";

    private static final String XML_RESPONSE_SINGLE_CARD = "<?xml version=\"1.0\"?>\n" +
            "<TMSTN>\n" +
            "<MerchantId>100000</MerchantId>\n" +
            "<TransactionType>RD</TransactionType>\n" +
            "<OrderNumber>6663889074</OrderNumber>\n" +
            "<StrId>301298824</StrId>\n" +
            "<RequestType>Q</RequestType>\n" +
            "<CustomerId>1007865</CustomerId>\n" +
            "<Card>\n" +
                "<CardId>2009876</CardId>\n" +
                "<AcctNumber>********0019</AcctNumber>\n" +
                "<AcctName>John Smith</AcctName>\n" +
                "<CreditCardType>MA</CreditCardType>\n" +
                "<IssueCountry>GB</IssueCountry>\n" +
                "<CardIssuer>National Bank</CardIssuer>\n" +
                "<ExpDate>022020</ExpDate>\n" +
                "<IssueNumber>1</IssueNumber>\n" +
                "<StartDate>022005</StartDate>\n" +
                "<IsDefault>0</IsDefault>\n" +
            "</Card>\n" +
            "<MessageCode>7100</MessageCode>\n" +
            "<Message>Transaction Approved</Message>\n" +
            "</TMSTN>";

    private static final String XML_RESPONSE_NO_CARDS = "<?xml version=\"1.0\"?>" +
            "<TMSTN>" +
            "<MerchantId>200161</MerchantId>" +
            "<TransactionType>RD</TransactionType>" +
            "<OrderNumber>6883431576</OrderNumber>" +
            "<StrId>817095960</StrId>" +
            "<RequestType>Q</RequestType>" +
            "<CustomerId>D17A5E30-E</CustomerId>" +
            "<MessageCode>7300</MessageCode>" +
            "<Message>No Cards Found</Message>" +
            "</TMSTN>";

    private static final String ONE_CARD_LESS_INFORMATION_RESPONSE = "<?xml version=\"1.0\"?>" +
            "<TMSTN>" +
            "<MerchantId>200161</MerchantId>" +
            "<TransactionType>RD</TransactionType>" +
            "<OrderNumber>8623882469</OrderNumber>" +
            "<StrId>817514610</StrId>" +
            "<RequestType>Q</RequestType>" +
            "<CustomerId>100000</CustomerId>" +
            "<Card>" +
                "<CardId>100509566</CardId>" +
                "<AcctNumber>********0000</AcctNumber>" +
                "<AcctName><![CDATA[John Smith]]></AcctName>" +
                "<CreditCardType>VI</CreditCardType>" +
                "<ExpDate>122013</ExpDate>" +
                "<IsDefault>1</IsDefault>" +
            "</Card>" +
            "<MessageCode>7100</MessageCode>" +
            "<Message>Transaction Approved</Message>" +
            "</TMSTN>";

    private static final String CUSTOMER_DOES_NOT_EXIST = "~MerchantId^200161~TransactionType^RD~OrderNumber^683679273~StrId^817598280~RequestType^Q~MessageCode^7310~Message^No Customer Found";


}
