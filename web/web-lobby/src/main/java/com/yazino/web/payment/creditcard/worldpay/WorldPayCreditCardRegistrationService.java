package com.yazino.web.payment.creditcard.worldpay;

import com.google.common.base.Optional;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.NVPResponse;
import com.yazino.payment.worldpay.STLink;
import com.yazino.payment.worldpay.nvp.NVPMessage;
import com.yazino.payment.worldpay.nvp.RedirectGenerateMessage;
import com.yazino.payment.worldpay.nvp.RedirectQueryOTTMessage;
import com.yazino.web.domain.payment.CardRegistrationResult;
import com.yazino.web.domain.payment.CardRegistrationResultBuilder;
import com.yazino.web.domain.payment.CardRegistrationTokenResult;
import com.yazino.web.domain.payment.CardRegistrationTokenResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.google.common.base.Optional.absent;
import static com.yazino.payment.worldpay.MessageCode.forCode;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class WorldPayCreditCardRegistrationService {
    private static final Logger LOG = LoggerFactory.getLogger(WorldPayCreditCardRegistrationService.class);
    public static final String PROPERTY_WORLDPAY_ENABLED = "payment.worldpay.stlink.enabled";
    public static final String PROPERTY_WORLDPAY_GATEWAY = "payment.worldpay.stlink.gateway";
    public static final String PROPERTY_WORLDPAY_TEST_GATEWAY = "payment.worldpay.stlink.testgateway";
    public static final String PROPERTY_WORLDPAY_TEST_MODE = "payment.worldpay.stlink.testmode";
    public static final String REGISTER_CARD_ACTION = "A";
    public static final String ONE_TIME_TOKEN_PARAMETER = "OTT";
    public static final String IS_TEST_PARAMETER = "IsTest";
    public static final String CARD_ID_PARAMETER = "CardId";
    public static final String ACTION_PARAMETER = "Action";
    public static final String OTT_RESULT_URL_PARAMETER = "OTTResultURL";
    public static final String OTT_PROCESS_URL_PARAMETER = "OTTProcessURL";
    public static final String CUSTOMER_ID_PARAMETER = "CustomerId";
    public static final String ACCT_NUMBER_PARAMETER = "AcctNumber";
    public static final String ACCOUNT_NAME_PARAMETER = "AcctName";
    public static final String EXPIRY_DATE_PARAMETER = "ExpDate";
    public static final String MESSAGE_CODE_PARAMETER = "MessageCode";
    public static final String MESSAGE_PARAMETER = "Message";

    private final YazinoConfiguration yazinoConfiguration;
    private final STLink stLink;

    @Autowired
    public WorldPayCreditCardRegistrationService(final YazinoConfiguration yazinoConfiguration, final STLink stLink) {
        notNull(yazinoConfiguration, "yazinoConfiguration is null");
        notNull(stLink, "stLink is null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.stLink = stLink;
    }

    public Optional<CardRegistrationTokenResult> prepareCardRegistration(BigDecimal playerId, String forwardURL) {
        notNull(playerId, "playerId cannot be null");
        notBlank(forwardURL, "forwardURL cannot be null");

        LOG.debug("Generating card registration token for player {}", playerId);
        if (yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_ENABLED, false)) {
            final NVPResponse response = stLink.send(createTokenMessage(playerId, forwardURL));
            if (response.get(ONE_TIME_TOKEN_PARAMETER).isPresent()) {
                CardRegistrationTokenResultBuilder builder = new CardRegistrationTokenResultBuilder().withTest(isTestMode());
                String registrationURL = null;
                if (isTestMode()) {
                    registrationURL = yazinoConfiguration.getString(PROPERTY_WORLDPAY_TEST_GATEWAY);
                } else if (response.get(OTT_PROCESS_URL_PARAMETER).isPresent()) {
                    registrationURL = response.get(OTT_PROCESS_URL_PARAMETER).get();
                }
                if (isBlank(registrationURL)) {
                    LOG.error("WorldPay OTTProcessURL was not present");
                    return absent();
                }
                return Optional.of(builder
                        .withToken(response.get(ONE_TIME_TOKEN_PARAMETER).get())
                        .withRegistrationURL(registrationURL)
                        .build());
            } else {
                boolean worldPayMessageIsPresent = response.get("Message").isPresent();
                if (worldPayMessageIsPresent) {
                    LOG.error("Unable to generate token for WorldPay card registration. Message code is:" + response.get("MessageCode").get());
                } else {
                    LOG.error("Unable to generate token for WorldPay card registration. With no message from WorldPay");
                }
            }
        }
        return absent();
    }

    public CardRegistrationResult retrieveCardRegistrationResult(final String oneTimeToken) {
        notBlank(oneTimeToken, "oneTimeToken cannot be null");

        NVPMessage nvpMessage = new RedirectQueryOTTMessage();
        if (isTestMode()) {
            nvpMessage = nvpMessage.withValue(IS_TEST_PARAMETER, 1);
        }
        final NVPResponse response = stLink.send(nvpMessage.withValue(ONE_TIME_TOKEN_PARAMETER, oneTimeToken));

        final Optional<String> messageCodeResponse = response.get(MESSAGE_CODE_PARAMETER);
        if (!messageCodeResponse.isPresent() || forCode(messageCodeResponse.get()) == null) {
            throw new IllegalStateException(format("NVP Payment Response is missing or invalid."));
        }
        final CardRegistrationResultBuilder builder = CardRegistrationResultBuilder.builder()
                .withMessageCode(response.get(MESSAGE_CODE_PARAMETER).get())
                .withMessage(response.get(MESSAGE_PARAMETER).get());

        if (forCode(messageCodeResponse.get()).isSuccessful()) {
            return builder.withCardId(response.get(CARD_ID_PARAMETER).get())
                    .withCustomerId(response.get(CUSTOMER_ID_PARAMETER).get())
                    .withObscuredCardNumber(response.get(ACCT_NUMBER_PARAMETER).get())
                    .withAccountName(response.get(ACCOUNT_NAME_PARAMETER).get())
                    .withExpiryDate(response.get(EXPIRY_DATE_PARAMETER).get())
                    .build();
        }
        return builder.build();
    }

    private NVPMessage createTokenMessage(final BigDecimal playerId, String forwardURL) {
        NVPMessage generateTokenMessage = new RedirectGenerateMessage()
                .withValue(ACTION_PARAMETER, REGISTER_CARD_ACTION)
                .withValue(CUSTOMER_ID_PARAMETER, playerId.toString())
                .withValue(OTT_RESULT_URL_PARAMETER, forwardURL);
        if (isTestMode()) {
            generateTokenMessage = generateTokenMessage.withValue(IS_TEST_PARAMETER, 1);
        }
        return generateTokenMessage;
    }

    private boolean isTestMode() {
        return yazinoConfiguration.getBoolean(PROPERTY_WORLDPAY_TEST_MODE, false);
    }
}
