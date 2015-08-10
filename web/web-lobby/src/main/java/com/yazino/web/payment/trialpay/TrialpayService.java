package com.yazino.web.payment.trialpay;

import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PlayerService;
import com.yazino.web.domain.email.EarnedChipsEmailBuilder;
import com.yazino.web.service.PurchaseTracking;
import com.yazino.web.service.QuietPlayerEmailer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletException;
import java.math.BigDecimal;
import java.util.Currency;

@Service
public class TrialpayService {

    private static final Logger LOG = LoggerFactory.getLogger(TrialpayService.class);

    private final TrialpayValidator trialPayValidationService;
    private final PlayerService playerService;
    private final WalletService walletService;
    private final CommunityService communityService;
    private final QuietPlayerEmailer emailer;
    private final PurchaseTracking purchaseTracking;

    @Autowired
    public TrialpayService(final TrialpayValidator trialPayValidationService,
                           final CommunityService communityService,
                           final WalletService walletService,
                           final PlayerService playerService,
                           final QuietPlayerEmailer emailer,
                           final PurchaseTracking purchaseTracking) {
        this.trialPayValidationService = trialPayValidationService;
        this.communityService = communityService;
        this.walletService = walletService;
        this.playerService = playerService;
        this.emailer = emailer;
        this.purchaseTracking = purchaseTracking;
    }

    public String payoutChipsAndNotifyPlayer(final BigDecimal playerId,
                                             final BigDecimal amount,
                                             final BigDecimal revenue,
                                             final String transactionRef,
                                             final String expectedHash)
            throws WalletServiceException, ServletException {
        final String body = String.format("oid=%s&sid=%s&reward_amount=%s&revenue=%s",
                transactionRef, playerId, amount, revenue);
        if (trialPayValidationService.validate(expectedHash, body)) {
            LOG.debug("Successfully validated the sender as trialpay");

            final BigDecimal accountId = playerService.getAccountId(playerId);

            postExternalTransaction(accountId, transactionRef, revenue, amount, playerId);
            communityService.asyncPublishBalance(playerId);

            final EarnedChipsEmailBuilder builder = new EarnedChipsEmailBuilder(playerId, transactionRef, amount);
            final boolean success = emailer.quietlySendEmail(builder);

            if (success) {
                LOG.debug("Successfully awarded player[" + playerId + "] with [" + amount + "] chips");
            }

            purchaseTracking.trackSuccessfulPurchase(playerId);

            return "payment/trialpay/success";
        } else {
            throw new ServletException("Failed To verify sender was Trialpay");
        }
    }

    private void postExternalTransaction(final BigDecimal accountId,
                                         final String transactionId,
                                         final BigDecimal amountCash,
                                         final BigDecimal amountChips,
                                         final BigDecimal playerId) throws WalletServiceException {
        final ExternalTransaction externalTransaction = ExternalTransaction.newExternalTransaction(accountId)
                .withInternalTransactionId(transactionId)
                .withExternalTransactionId(transactionId)
                .withMessage("", new DateTime())
                .withAmount(Currency.getInstance("USD"), amountCash)
                .withPaymentOption(amountChips, null)
                .withCreditCardNumber("")
                .withCashierName("Trialpay")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType("")
                .withPlayerId(playerId)
                .withPromotionId(null)
                .withPlatform(Platform.WEB)
                .build();
        LOG.debug("Trying to log an external transaction");
        walletService.record(externalTransaction);
        LOG.debug("Successfully logged an external transaction");
    }

}
