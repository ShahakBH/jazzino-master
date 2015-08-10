package com.yazino.web.payment.creditcard.worldpay;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.NVPXmlResponse;
import com.yazino.payment.worldpay.STLink;
import com.yazino.payment.worldpay.nvp.NVPMessage;
import com.yazino.payment.worldpay.nvp.RedirectQueryMessage;
import com.yazino.web.domain.payment.RegisteredCardQueryResult;
import com.yazino.web.domain.payment.RegisteredCardsQueryResultBuilder;
import com.yazino.web.domain.payment.RegisteredCreditCardDetails;
import com.yazino.web.domain.payment.RegisteredCreditCardDetailsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.yazino.payment.worldpay.NVPXmlResponse.isXmlResponse;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class WorldPayCreditCardQueryService {
    private static final Logger LOG = LoggerFactory.getLogger(WorldPayCreditCardQueryService.class);
    public static final String PROPERTY_WORLDPAY_ENABLED = "payment.worldpay.stlink.enabled";
    public static final String PROPERTY_WORLDPAY_TEST_MODE = "payment.worldpay.stlink.testmode";
    public static final String CARD_ID_PROPERTY = "CardId";
    public static final String ACCOUNT_NUMBER_PROPERTY = "AcctNumber";
    public static final String ACCOUNT_NAME_PROPERTY = "AcctName";
    public static final String EXPIRY_DATE_PROPERTY = "ExpDate";
    public static final String ISSUE_COUNTRY_PROPERTY = "IssueCountry";
    public static final String CARD_ISSUER_PROPERTY = "CardIssuer";
    public static final String CREDIT_CARD_TYPE_PROPERTY = "CreditCardType";
    public static final String IS_DEFAULT_PROPERTY = "IsDefault";

    private final YazinoConfiguration yazinoConfiguration;
    private final STLink stLink;

    @Autowired
    public WorldPayCreditCardQueryService(final YazinoConfiguration yazinoConfiguration, final STLink stLink) {
        notNull(yazinoConfiguration, "yazinoConfiguration is null");
        notNull(stLink, "stLink is null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.stLink = stLink;
    }

    public RegisteredCardQueryResult retrieveCardsFor(BigDecimal playerId) {
        notNull(playerId, "playerId cannot be null");

        LOG.debug("Querying WorldPay for registered cards for player with ID {}", playerId);
        if (isWorldPayEnabled()) {
            return queryAllCardsForCustomer(playerId.toString());
        }
        return new RegisteredCardsQueryResultBuilder().build();
    }

    private RegisteredCardQueryResult queryAllCardsForCustomer(final String playerId) {
        RegisteredCardsQueryResultBuilder builder = new RegisteredCardsQueryResultBuilder();
        NVPMessage message = new RedirectQueryMessage().withValue("CustomerId", playerId);
        if (isTestMode()) {
            message = message.withValue("IsTest", 1);
        }
        final String response = stLink.sendWithoutParsing(message);
        if (isXmlResponse(response)) {
            final NVPXmlResponse.Property cardProperty = new NVPXmlResponse(response).getProperty("Card");
            if (cardProperty == null) {
                LOG.debug("No cards found for player with ID {}", playerId);
                return builder.build();
            }
            if (cardProperty.hasPropertiesWithSameName()) { // has multiple cards
                LOG.debug("Found multiple cards for player with ID {}", playerId);
                for (NVPXmlResponse.Property nestedCardProperty : cardProperty.getNestedPropertyGroup()) {
                    builder = builder.withCreditCard(cardFrom(nestedCardProperty));
                }
            } else if (cardProperty.hasNestedProperties()) { // has only one card
                LOG.debug("Found one card for player with ID {}", playerId);
                builder = builder.withCreditCard(cardFrom(cardProperty));
            }
        }
        return builder.build();
    }

    private RegisteredCreditCardDetails cardFrom(final NVPXmlResponse.Property cardProperties) {
        RegisteredCreditCardDetailsBuilder builder = RegisteredCreditCardDetailsBuilder.valueOf()
                .withCardId(cardProperties.getNestedProperty(CARD_ID_PROPERTY).getValue())
                .withObscuredNumber(cardProperties.getNestedProperty(ACCOUNT_NUMBER_PROPERTY).getValue())
                .withAccountName(cardProperties.getNestedProperty(ACCOUNT_NAME_PROPERTY).getValue())
                .withIsDefault(cardProperties.getNestedProperty(IS_DEFAULT_PROPERTY).getValue());

        final NVPXmlResponse.Property expiryDateProperty = cardProperties.getNestedProperty(EXPIRY_DATE_PROPERTY);
        if (expiryDateProperty != null) {
            builder = builder.withExpiryDate(expiryDateProperty.getValue());
        }
        final NVPXmlResponse.Property issueCountryProperty = cardProperties.getNestedProperty(ISSUE_COUNTRY_PROPERTY);
        if (issueCountryProperty != null) {
            builder = builder.withIssueCountry(issueCountryProperty.getValue());
        }
        final NVPXmlResponse.Property cardIssuerProperty = cardProperties.getNestedProperty(CARD_ISSUER_PROPERTY);
        if (cardIssuerProperty != null) {
            builder = builder.withCardIssuer(cardIssuerProperty.getValue());
        }
        final NVPXmlResponse.Property creditCardTypeProperty = cardProperties.getNestedProperty(CREDIT_CARD_TYPE_PROPERTY);
        if (creditCardTypeProperty != null) {
            builder = builder.withCreditCardType(creditCardTypeProperty.getValue());
        }
        return builder.build();
    }

    private boolean isWorldPayEnabled() {
        return yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_ENABLED, false);
    }

    private boolean isTestMode() {
        return yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_TEST_MODE, false);
    }
}
