package com.yazino.platform.payment.dispute;

import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.payment.PaymentDispute;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

public class FacebookPaymentDisputeProcessor implements PaymentDisputeProcessor {
    private final YazinoConfiguration yazinoConfiguration;
    private final FacebookClientFactory facebookClientFactory;

    @Autowired
    public FacebookPaymentDisputeProcessor(final YazinoConfiguration yazinoConfiguration,
                                           final FacebookClientFactory facebookClientFactory) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(facebookClientFactory, "facebookClientFactory may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.facebookClientFactory = facebookClientFactory;
    }

    @Override
    public void raise(final PaymentDispute dispute) {
        notNull(dispute, "dispute may not be null");

        // No action required at this point
    }

    @Override
    public void resolve(final PaymentDispute dispute) {
        notNull(dispute, "dispute may not be null");

        if (dispute.getResolution() == null) {
            throw new IllegalStateException("Dispute must be resolved: " + dispute);
        }
        if (dispute.getGameType() == null) {
            throw new IllegalStateException("GameType is not available, cannot resolve dispute for transaction: "
                    + dispute.getInternalTransactionId());
        }

        switch (dispute.getResolution()) {
            case REFUNDED_FRAUD:
                refundPlayer(dispute, "MALICIOUS_FRAUD");
                break;
            case REFUNDED_PLAYER_ERROR:
                refundPlayer(dispute, "FRIENDLY_FRAUD");
                break;
            case REFUNDED_OTHER:
                refundPlayer(dispute, "CUSTOMER_SERVICE");
                break;
            case CHIPS_CREDITED:
                resolveDisputeAs(dispute, "GRANTED_REPLACEMENT_ITEM");
                break;
            case REFUSED_BANNED:
                resolveDisputeAs(dispute, "BANNED_USER");
                break;
            default:
                resolveDisputeAs(dispute, "DENIED_REFUND");
        }
    }

    private void resolveDisputeAs(final PaymentDispute dispute,
                                  final String reason) {
        clientFor(dispute.getGameType()).publish(dispute.getExternalTransactionId() + "/dispute", Boolean.class,
                Parameter.with("reason", reason));
    }

    private void refundPlayer(final PaymentDispute dispute,
                              final String reason) {
        clientFor(dispute.getGameType()).publish(dispute.getExternalTransactionId() + "/refunds", Boolean.class,
                Parameter.with("currency", dispute.getCurrency().getCurrencyCode()),
                Parameter.with("amount", dispute.getPrice()),
                Parameter.with("reason", reason));
    }

    private FacebookClient clientFor(final String gameType) {
        return facebookClientFactory.facebookClientFor(
                yazinoConfiguration.getString("facebook.clientAccessToken." + gameType));
    }

}
