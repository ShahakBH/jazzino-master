package com.yazino.platform.payment.settlement;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.account.PaymentSettlement;
import com.yazino.platform.persistence.account.JDBCPaymentSettlementDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class PaymentSettlementPoller {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentSettlementPoller.class);

    private static final String PROPERTY_SETTLEMENT_DELAY_IN_HOURS = "payment.worldpay.stlink.settlement-delay-hours";
    private static final int DEFAULT_SETTLEMENT_DELAY_IN_HOURS = 36;
    private static final int TEN_MINUTES = 1000 * 60 * 10;

    private final YazinoConfiguration yazinoConfiguration;
    private final JDBCPaymentSettlementDAO paymentSettlementDao;
    private final QueuePublishingService<PaymentSettlementRequest> publisher;

    @Autowired
    public PaymentSettlementPoller(final YazinoConfiguration yazinoConfiguration,
                                   final JDBCPaymentSettlementDAO paymentSettlementDao,
                                   @Qualifier("paymentSettlementQueuePublishingService") final QueuePublishingService<PaymentSettlementRequest> publisher) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(paymentSettlementDao, "paymentSettlementDao may not be null");
        notNull(publisher, "publisher may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.paymentSettlementDao = paymentSettlementDao;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelay = TEN_MINUTES)
    public void enqueuePaymentsDueForSettlement() {
        try {
            LOG.debug("Polling for pending settlements");
            for (final PaymentSettlement paymentSettlement : paymentSettlementDao.findPendingSettlements(settlementDelayInHours())) {
                LOG.debug("Queuing transaction {} for settlement", paymentSettlement.getInternalTransactionId());
                publisher.send(new PaymentSettlementRequest(paymentSettlement.getInternalTransactionId()));
            }

        } catch (Exception e) {
            LOG.error("Payment Settlement Poll failed", e);
        }
    }

    private int settlementDelayInHours() {
        return yazinoConfiguration.getInt(PROPERTY_SETTLEMENT_DELAY_IN_HOURS, DEFAULT_SETTLEMENT_DELAY_IN_HOURS);
    }
}
