package com.yazino.payment.worldpay;

import com.yazino.payment.worldpay.nvp.RedirectAddMessage;
import com.yazino.payment.worldpay.nvp.RedirectQueryMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class STLinkQueryCardsExternalIntegrationTest {
    private static final String CUSTOMER_ID_1 = "4B8FFD03-1";
    private static final String CUSTOMER_ID_2 = "4B8FFD03-3";

    @Autowired
    private STLink stLink;

    @Test
    public void shouldReturnAllCardsForCustomerId() {
        stLink.send(new RedirectAddMessage()
                .withValue("CustomerId", CUSTOMER_ID_1)
                .withValue("IsTest", "1")
                .withValue("AcctName", "Bob Jones")
                .withValue("AcctNumber", "4544320001228342")
                .withValue("ExpDate", "122015"));

        final NVPXmlResponse responseForAllCards = stLink.expectXMLSend(new RedirectQueryMessage()
                .withValue("IsTest", 1)
                .withValue("CustomerId", CUSTOMER_ID_1));

        NVPXmlResponse.Property card = responseForAllCards.getProperty("Card");
        assertThat(card.getNestedProperty("CardId").getValue(), notNullValue());
        assertThat(card.getNestedProperty("IsDefault").getValue(), is(equalTo("1")));
        assertThat(card.getNestedProperty("ExpDate").getValue(), is(equalTo("122015")));
        assertThat(card.getNestedProperty("AcctName").getValue(), is(equalTo("Bob Jones")));
    }

    @Test
    public void saveTwoCardsForCustomerIdAndQueryAllCards() throws IOException {
        /* save card 6766770009991000 */
        final NVPResponse cardOneRegistrationResponse = stLink.send(new RedirectAddMessage()
                .withValue("CustomerId", CUSTOMER_ID_2)
                .withValue("IsTest", "1")
                .withValue("AcctName", "Peter Jones")
                .withValue("AcctNumber", "6766770009991000")
                .withValue("ExpDate", "122015"));

        /* save card 4544320001228342 */
        final NVPResponse cardTwoRegistrationResponse = stLink.send(new RedirectAddMessage()
                .withValue("CustomerId", CUSTOMER_ID_2)
                .withValue("IsTest", "1")
                .withValue("AcctName", "John W Smith")
                .withValue("AcctNumber", "4544320001228342")
                .withValue("ExpDate", "122015"));

        /* query all cards for the customer with this ID */
        final NVPXmlResponse responseForAllCards = stLink.expectXMLSend(new RedirectQueryMessage()
                .withValue("IsTest", 1)
                .withValue("CustomerId", CUSTOMER_ID_2));

        /* confirm card addition */
        assertThat(cardOneRegistrationResponse.get("AcctNumber").get(), is(equalTo("********1000")));
        assertThat(cardOneRegistrationResponse.get("AcctName").get(), is(equalTo("Peter Jones")));
        assertThat(cardOneRegistrationResponse.get("CreditCardType").get(), is(equalTo("MA")));
        assertThat(cardOneRegistrationResponse.get("IssueCountry").get(), is(equalTo("ES")));
        assertThat(cardOneRegistrationResponse.get("CardIssuer").get(), is(equalTo("SISTEMA 4B S.A.")));
        assertThat(cardOneRegistrationResponse.get("MessageCode").get(), is(equalTo("7102")));
        assertThat(cardOneRegistrationResponse.get("Message").get(), is(equalTo("Customer Information Added")));

        /* confirm second card addition */
        assertThat(cardTwoRegistrationResponse.get("AcctNumber").get(), is(equalTo("********8342")));
        assertThat(cardTwoRegistrationResponse.get("AcctName").get(), is(equalTo("John W Smith")));
        assertThat(cardTwoRegistrationResponse.get("CreditCardType").get(), is(equalTo("VD")));
        assertThat(cardTwoRegistrationResponse.get("IssueCountry").get(), is(equalTo("GB")));
        assertThat(cardTwoRegistrationResponse.get("CardIssuer").get(), is(equalTo("LLOYDS TSB BANK PLC")));
        assertThat(cardTwoRegistrationResponse.get("MessageCode").get(), is(equalTo("7102")));
        assertThat(cardTwoRegistrationResponse.get("Message").get(), is(equalTo("Customer Information Added")));

        /* check both cards are returned */
        assertThat(responseForAllCards.getProperty("Card").getNestedPropertyGroup(), hasSize(2));
        for (NVPXmlResponse.Property card : responseForAllCards.getProperty("Card").getNestedPropertyGroup()) {
            assertThat(card.getNestedProperty("CardId").getValue(), not(isEmptyOrNullString()));
            assertThat(card.getNestedProperty("AcctNumber").getValue(), not(isEmptyOrNullString()));
            assertThat(card.getNestedProperty("CardIssuer").getValue(), not(isEmptyOrNullString()));
            assertThat(card.getNestedProperty("CreditCardType").getValue(), not(isEmptyOrNullString()));
            assertThat(card.getNestedProperty("IsDefault").getValue(), not(isEmptyOrNullString()));
        }
    }
}
