package com.yazino.platform.payment;

import com.google.common.base.Optional;
import com.yazino.platform.account.*;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.account.PaymentSettlement;
import com.yazino.platform.payment.dispute.PaymentDisputeProcessor;
import com.yazino.platform.payment.settlement.PaymentSettlementProcessor;
import com.yazino.platform.persistence.account.JDBCPaymentSettlementDAO;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileStatus;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Service("paymentService")
public class DatabasePaymentService implements PaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(DatabasePaymentService.class);

    private static final String SYSTEM_USER = "system";
    private static final String PAYMENT_CANCELLED = "Payment cancelled before settlement";

    private final Map<String, PaymentSettlementProcessor> cashiersToSettlementProcessors = new HashMap<>();
    private final Map<String, PaymentDisputeProcessor> cashiersToDisputeProcessors = new HashMap<>();

    private final JDBCPaymentSettlementDAO paymentSettlementDao;
    private final JDBCPaymentDisputeDAO paymentDisputeDao;
    private final PlayerProfileDao playerProfileDao;
    private final WalletService walletService;

    @Autowired
    public DatabasePaymentService(final JDBCPaymentSettlementDAO paymentSettlementDao,
                                  final JDBCPaymentDisputeDAO paymentDisputeDao,
                                  final PlayerProfileDao playerProfileDao,
                                  final WalletService walletService) {
        notNull(paymentSettlementDao, "paymentSettlementDao may not be null");
        notNull(paymentDisputeDao, "paymentDisputeDao may not be null");
        notNull(playerProfileDao, "playerProfileDao may not be null");
        notNull(walletService, "walletService may not be null");

        this.paymentSettlementDao = paymentSettlementDao;
        this.paymentDisputeDao = paymentDisputeDao;
        this.playerProfileDao = playerProfileDao;
        this.walletService = walletService;
    }

    @Resource(name = "paymentSettlementProcessors")
    public void setPaymentSettlementProcessors(final Map<String, PaymentSettlementProcessor> paymentSettlementProcessors) {
        this.cashiersToSettlementProcessors.clear();
        if (paymentSettlementProcessors != null) {
            this.cashiersToSettlementProcessors.putAll(paymentSettlementProcessors);
        }
    }

    @Resource(name = "paymentDisputeProcessors")
    public void setPaymentDisputeProcessors(final Map<String, PaymentDisputeProcessor> paymentDisputeProcessors) {
        this.cashiersToDisputeProcessors.clear();
        if (paymentDisputeProcessors != null) {
            this.cashiersToDisputeProcessors.putAll(paymentDisputeProcessors);
        }
    }

    @Override
    public PagedData<PendingSettlement> findAuthorised(final int page, final int pageSize) {
        return paymentSettlementDao.findSummarisedPendingSettlements(page, pageSize);
    }

    @Override
    public int cancelAllSettlementsForPlayer(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        int cancelledCount = 0;
        for (PaymentSettlement settlement : paymentSettlementDao.findByPlayerId(playerId)) {
            LOG.debug("Attempting to cancel transaction {}", settlement.getInternalTransactionId());

            final PaymentSettlementProcessor processor = cashiersToSettlementProcessors.get(settlement.getCashierName().toLowerCase());
            if (processor == null) {
                LOG.error("No processor is available to settle transaction {} with cashier {}",
                        settlement.getInternalTransactionId(), settlement.getCashierName());
                continue;
            }

            try {
                final ExternalTransaction externalTransaction = processor.cancel(settlement);
                walletService.record(externalTransaction);

                if (externalTransaction.getStatus() != ExternalTransactionStatus.ERROR) {
                    paymentSettlementDao.deleteById(settlement.getInternalTransactionId());
                    ++cancelledCount;
                }

            } catch (Exception e) {
                LOG.error("Cancellation failed for transaction {} with cashier {}",
                        settlement.getInternalTransactionId(), settlement.getCashierName(), e);
            }
        }

        playerProfileDao.updateStatus(playerId, PlayerProfileStatus.BLOCKED, SYSTEM_USER, PAYMENT_CANCELLED);

        return cancelledCount;
    }

    @Override
    public void disputePayment(final PaymentDispute dispute) {
        notNull(dispute, "dispute may not be null");

        final PaymentDisputeProcessor processor = cashiersToDisputeProcessors.get(dispute.getCashierName().toLowerCase());
        if (processor == null) {
            LOG.error("No processor is available to dispute payment {} with cashier {}",
                    dispute.getInternalTransactionId(), dispute.getCashierName());
            return;
        }

        try {
            processor.raise(dispute);
            paymentDisputeDao.save(dispute);

        } catch (Exception e) {
            LOG.error("Dispute creation failed for transaction {} with cashier {}",
                    dispute.getInternalTransactionId(), dispute.getCashierName(), e);
        }
    }

    @Override
    public void resolveDispute(final String internalTransactionId,
                               final DisputeResolution resolution,
                               final String resolvedBy,
                               final String note) {
        notNull(internalTransactionId, "internalTransactionId may not be null");
        notNull(resolution, "resolution may not be null");
        notNull(resolvedBy, "resolvedBy may not be null");

        final Optional<PaymentDispute> disputeResult = paymentDisputeDao.findByInternalTransactionId(internalTransactionId);
        if (!disputeResult.isPresent()) {
            throw new IllegalArgumentException("Transaction does not exist: " + internalTransactionId);
        }

        final PaymentDispute resolvedDispute = PaymentDispute.copy(disputeResult.get())
                .withResolution(filterResolution(resolution, disputeResult.get()),
                        new DateTime(),
                        note,
                        resolvedBy)
                .build();

        final PaymentDisputeProcessor processor = cashiersToDisputeProcessors.get(resolvedDispute.getCashierName().toLowerCase());
        if (processor == null) {
            LOG.error("No processor is available to resolve dispute for payment {} with cashier {}",
                    resolvedDispute.getInternalTransactionId(), resolvedDispute.getCashierName());
            return;
        }

        try {
            processor.resolve(resolvedDispute);

            paymentDisputeDao.save(resolvedDispute);

            switch (resolvedDispute.getResolution()) {
                case REFUNDED_FRAUD:
                case REFUNDED_PLAYER_ERROR:
                case REFUNDED_OTHER:
                    removeChips(resolvedDispute);
                    break;
                case CHIPS_CREDITED:
                    creditChips(resolvedDispute);
                    break;
                default:
                    // no action required
            }

        } catch (Exception e) {
            LOG.error("Dispute resolution failed for transaction {} with cashier {}",
                    resolvedDispute.getInternalTransactionId(), resolvedDispute.getCashierName(), e);
        }
    }

    @Override
    public PagedData<DisputeSummary> findOpenDisputes(final int page, final int pageSize) {
        return paymentDisputeDao.findOpenDisputes(page, pageSize);
    }

    private DisputeResolution filterResolution(final DisputeResolution resolution,
                                               final PaymentDispute disputeResult) {
        switch (resolution) {
            case REFUSED:
                final PlayerProfile playerProfile = playerProfileDao.findByPlayerId(disputeResult.getPlayerId());
                if (playerProfile == null) {
                    throw new IllegalArgumentException("Player does not exist: " + disputeResult.getPlayerId());
                }

                if (playerProfile.getStatus() == PlayerProfileStatus.BLOCKED
                        || playerProfile.getStatus() == PlayerProfileStatus.CLOSED) {
                    return DisputeResolution.REFUSED_BANNED;
                }
                return resolution;

            default:
                return resolution;
        }
    }

    private void creditChips(final PaymentDispute resolvedDispute) throws WalletServiceException {
        walletService.postTransaction(resolvedDispute.getAccountId(), resolvedDispute.getChips(),
                transactionTypeFor(resolvedDispute), "Re-credit of transaction " + resolvedDispute.getInternalTransactionId(),
                TransactionContext.transactionContext().withPlayerId(resolvedDispute.getPlayerId()).build());
    }

    private void removeChips(final PaymentDispute resolvedDispute) throws WalletServiceException {
        try {
            walletService.postTransaction(resolvedDispute.getAccountId(), BigDecimal.ZERO.subtract(resolvedDispute.getChips()),
                    transactionTypeFor(resolvedDispute), "Refund of transaction " + resolvedDispute.getInternalTransactionId(),
                    TransactionContext.transactionContext().withPlayerId(resolvedDispute.getPlayerId()).build());
        } catch (Exception e) {
            LOG.info("Unable to reverse transaction {}", resolvedDispute.getInternalTransactionId(), e);
        }
    }

    private String transactionTypeFor(final PaymentDispute resolvedDispute) {
        return String.format("%s %s", resolvedDispute.getCashierName(),
                StringUtils.capitalize(resolvedDispute.getTransactionType().toString().toLowerCase()));
    }
}
