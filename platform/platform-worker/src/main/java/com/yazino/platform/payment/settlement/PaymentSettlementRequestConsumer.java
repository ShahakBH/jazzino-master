package com.yazino.platform.payment.settlement;

import com.google.common.base.Optional;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.platform.model.account.PaymentSettlement;
import com.yazino.platform.persistence.account.JDBCPaymentSettlementDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class PaymentSettlementRequestConsumer implements QueueMessageConsumer<PaymentSettlementRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentSettlementRequestConsumer.class);

    private final Map<String, PaymentSettlementProcessor> cashiersToProcessors = new HashMap<>();

    private final JDBCPaymentSettlementDAO paymentSettlementDao;
    private final WalletService walletService;

    @Autowired
    public PaymentSettlementRequestConsumer(final JDBCPaymentSettlementDAO paymentSettlementDao,
                                            final WalletService walletService) {
        notNull(paymentSettlementDao, "paymentSettlementDao may not be null");
        notNull(walletService, "walletService may not be null");

        this.paymentSettlementDao = paymentSettlementDao;
        this.walletService = walletService;
    }

    @Resource(name = "paymentSettlementProcessors")
    public void setPaymentSettlementProcessors(final Map<String, PaymentSettlementProcessor> paymentSettlementProcessors) {
        this.cashiersToProcessors.clear();
        if (paymentSettlementProcessors != null) {
            this.cashiersToProcessors.putAll(paymentSettlementProcessors);
        }
    }

    @Override
    public void handle(final PaymentSettlementRequest message) {
        if (message == null) {
            LOG.debug("Message was null");
            return;
        }

        LOG.debug("Processing settlement of transaction {}", message.getInternalTransactionId());

        final Optional<PaymentSettlement> paymentSettlement = paymentSettlementDao.findById(message.getInternalTransactionId());
        if (!paymentSettlement.isPresent()) {
            LOG.warn("Transaction {} no longer exists, ignoring.", message.getInternalTransactionId());
            return;
        }

        final PaymentSettlementProcessor processor = cashiersToProcessors.get(paymentSettlement.get().getCashierName().toLowerCase());
        if (processor == null) {
            LOG.error("No processor is available to settle transaction {} with cashier {}",
                    message.getInternalTransactionId(), paymentSettlement.get().getCashierName());
            return;
        }

        try {
            final ExternalTransaction externalTransaction = processor.settle(paymentSettlement.get());
            walletService.record(externalTransaction);

            if (externalTransaction.getStatus() != ExternalTransactionStatus.ERROR) {
                paymentSettlementDao.deleteById(message.getInternalTransactionId());
            }

        } catch (Exception e) {
            LOG.error("Settlement failed for transaction {} with cashier {}",
                    message.getInternalTransactionId(), paymentSettlement.get().getCashierName(), e);
        }
    }

}
