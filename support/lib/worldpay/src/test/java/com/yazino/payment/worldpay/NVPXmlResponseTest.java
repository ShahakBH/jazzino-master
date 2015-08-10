package com.yazino.payment.worldpay;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

public class NVPXmlResponseTest {


    @Test
    public void shouldParseXmlForMultipleCardsResponse() {
        final NVPXmlResponse response = new NVPXmlResponse("<?xml version=\"1.0\"?>\n" +
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
                "<ExpDate>0220014</ExpDate>\n" +
                "<IssueNumber>1</IssueNumber>\n" +
                "<StartDate>022005</StartDate>\n" +
                "<IsDefault>0</IsDefault>\n" +
                "</Card>\n" +
                "<MessageCode>7100</MessageCode>\n" +
                "<Message>Transaction Approved</Message>\n" +
                "</TMSTN>");

        assertThat("100000", is(equalTo(response.getProperty("MerchantId").getValue())));
        assertThat("RD", is(equalTo(response.getProperty("TransactionType").getValue())));
        assertThat("6663889074", is(equalTo(response.getProperty("OrderNumber").getValue())));
        assertThat("301298824", is(equalTo(response.getProperty("StrId").getValue())));
        assertThat("Q", is(equalTo(response.getProperty("RequestType").getValue())));
        assertThat("1007865", is(equalTo(response.getProperty("CustomerId").getValue())));
        assertThat("Transaction Approved", is(equalTo(response.getProperty("Message").getValue())));

        final List<NVPXmlResponse.Property> cardProperties = response.getProperty("Card").getNestedPropertyGroup();
        assertThat(cardProperties, notNullValue());

        for (NVPXmlResponse.Property cardProperty : cardProperties) {
            assertThat(cardProperty.getNestedProperty("CardId").getValue(), not(isEmptyOrNullString()));
            assertThat(cardProperty.getNestedProperty("AcctNumber").getValue(), not(isEmptyOrNullString()));
            assertThat(cardProperty.getNestedProperty("CreditCardType").getValue(), not(isEmptyOrNullString()));
            assertThat(cardProperty.getNestedProperty("IssueCountry").getValue(), not(isEmptyOrNullString()));
            assertThat(cardProperty.getNestedProperty("CardIssuer").getValue(), not(isEmptyOrNullString()));
            assertThat(cardProperty.getNestedProperty("ExpDate").getValue(), not(isEmptyOrNullString()));
            assertThat(cardProperty.getNestedProperty("IssueNumber").getValue(), not(isEmptyOrNullString()));
            assertThat(cardProperty.getNestedProperty("StartDate").getValue(), not(isEmptyOrNullString()));
            assertThat(cardProperty.getNestedProperty("IsDefault").getValue(), not(isEmptyOrNullString()));
        }
    }
}
