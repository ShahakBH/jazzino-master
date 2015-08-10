package com.yazino.web.payment.facebook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.gigaspaces.internal.utils.StringUtils;
import com.restfb.DefaultFacebookClient;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.account.WalletServiceException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
@Service
public class FacebookPaymentIntegrationService {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookPaymentIntegrationService.class);

    private final SimpleDateFormat fbDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private final YazinoConfiguration yazinoConfiguration;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Autowired
    public FacebookPaymentIntegrationService(YazinoConfiguration yazinoConfiguration) {
        this.yazinoConfiguration = yazinoConfiguration;

        jsonMapper.registerModule(new JodaModule());
    }

    public FacebookPayment retrievePayment(String gameType, String paymentId) throws WalletServiceException {
        LOG.info("Retrieving {} facebook payment {}", gameType, paymentId);
        final String token = yazinoConfiguration.getString("facebook.clientAccessToken." + gameType);
        final DefaultFacebookClient client = getDefaultFacebookClient(token);
        try {
            String paymentJson = client.fetchObject(paymentId, String.class);
            Map<String, Object> paymentDetails = jsonMapper.readValue(paymentJson, Map.class);
            final List<Map<String, String>> actions = (List<Map<String, String>>) paymentDetails.get("actions");
            final Map<String, String> action = actions.get(actions.size() - 1); // get the latest

            FacebookPayment.Status status = parseStatus(action);
            FacebookPayment.Type type = parseType(action);

            String disputeReason = null;
            DateTime disputeDate = null;
            final List<Map<String, String>> disputes = (List<Map<String, String>>) paymentDetails.get("disputes");
            if (disputes != null) {
                for (Map<String, String> dispute : disputes) {
                    if (StringUtils.equals(dispute.get("status"), "pending")) {
                        disputeReason = dispute.get("user_comment");
                        disputeDate = new DateTime(fbDateFormat.parse(dispute.get("time_created")));
                    }
                }
            }

            final List<Map<String, String>> items = (List<Map<String, String>>) paymentDetails.get("items");
            final String product = items.get(0).get("product");

            final String currencyCode = action.get("currency");
            final BigDecimal amount = new BigDecimal(action.get("amount"));
            final String facebookUserId = ((Map<String, String>) paymentDetails.get("user")).get("id");
            return new FacebookPayment(product, status, type,
                    facebookUserId, (String) paymentDetails.get("request_id"), currencyCode, amount,
                    disputeReason, disputeDate);
        } catch (Exception e) {
            LOG.error("Could not fetch Facebook payment object " + paymentId, e);
            throw new WalletServiceException(e.getMessage());
        }
    }

    private FacebookPayment.Type parseType(final Map<String, String> action) throws WalletServiceException {
        String paymentType = action.get("type");
        LOG.info("Payment type= " + paymentType);
        if (paymentType != null) {
            try {
                return FacebookPayment.Type.valueOf(paymentType);
            } catch (IllegalArgumentException ignored) {
                // ignored
            }
        }
        throw new WalletServiceException("unknown payment Type type:" + paymentType);
    }

    private FacebookPayment.Status parseStatus(final Map<String, String> action) throws WalletServiceException {
        String paymentStatus = action.get("status");
        LOG.info("Payment status = " + paymentStatus);
        if (paymentStatus != null) {
            try {
                return FacebookPayment.Status.valueOf(paymentStatus);
            } catch (IllegalArgumentException ignored) {
                // ignored
            }
        }
        throw new WalletServiceException("unknown payment Status type:" + paymentStatus);
    }

    protected DefaultFacebookClient getDefaultFacebookClient(final String token) {
        return new DefaultFacebookClient(token);
    }

}
