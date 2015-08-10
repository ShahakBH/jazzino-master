package com.yazino.web.payment.amazon;

import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.account.ExternalTransactionBuilder;
import com.yazino.platform.community.PlayerService;
import com.yazino.web.payment.Purchase;
import com.yazino.web.payment.PurchaseStatus;
import com.yazino.web.payment.TransactionIdGenerator;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Currency;

import static java.lang.String.format;

@Service
public class AmazonInitiatePurchaseProcessor implements InitiatePurchaseProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(AmazonInitiatePurchaseProcessor.class);
    public static final String CASHIER_NAME = "Amazon";

    private final PlayerService playerService;
    private final WalletService walletService;
    private TransactionIdGenerator transactionIdGenerator;
    private ChipBundleResolver chipBundleResolver;

    @Autowired
    public AmazonInitiatePurchaseProcessor(final PlayerService playerService,
                                           final WalletService walletService,
                                           final TransactionIdGenerator transactionIdGenerator,
                                           final ChipBundleResolver chipBundleResolver) {
        this.playerService = playerService;
        this.walletService = walletService;
        this.transactionIdGenerator = transactionIdGenerator;
        this.chipBundleResolver = chipBundleResolver;
    }

    @Override
    public Object initiatePurchase(final BigDecimal playerId,
                                     final String productId,
                                     final Long promotionId,
                                     final String gameType,
                                     final Platform platform) {
        final Purchase purchase = new Purchase();

        final long internalTransactionId = transactionIdGenerator.generateNumericTransactionId();
        final BigDecimal accountId = playerService.getAccountId(playerId);
        final ExternalTransactionBuilder externalTransactionBuilder = ExternalTransaction.newExternalTransaction(accountId);
        externalTransactionBuilder.withPlayerId(playerId);
        externalTransactionBuilder.withCashierName("AMAZON");
        externalTransactionBuilder.withPromotionId(promotionId);
        externalTransactionBuilder.withGameType(gameType);
        externalTransactionBuilder.withPlatform(platform);
        externalTransactionBuilder.withInternalTransactionId(String.valueOf(internalTransactionId));
        externalTransactionBuilder.withExternalTransactionId(String.valueOf(internalTransactionId));
        externalTransactionBuilder.withMessage(String.format("productId: %s", productId), new DateTime());
        externalTransactionBuilder.withCashierName(CASHIER_NAME);
        externalTransactionBuilder.withCreditCardNumber("none");

        final ChipBundle chipBundleFor = chipBundleResolver.findChipBundleForProductId(gameType, productId);
        if (null == chipBundleFor)   {
            purchase.setStatus(PurchaseStatus.FAILED);
            purchase.setErrorMessage(format("Unknown product id: %s", productId));
            return purchase;
        }

        externalTransactionBuilder.withAmount(Currency.getInstance("USD"), chipBundleFor.getPrice());
        externalTransactionBuilder.withPaymentOption(chipBundleFor.getChips(), new DateTime().toString());

        purchase.setPurchaseId(String.valueOf(internalTransactionId));

        try {
            walletService.record(externalTransactionBuilder.build());
            purchase.setStatus(PurchaseStatus.CREATED);

        } catch (WalletServiceException e) {
            LOG.error(format("could not record external transaction: player_id: %s, product_id: %s, promotion_id: %s, gameType: %s, platform: &s",
                             playerId, productId, promotionId, gameType, platform));
            purchase.setStatus(PurchaseStatus.FAILED);
        }

        return purchase;
    }

    @Override
    public Platform getPlatform() {
        return Platform.AMAZON;
    }
}
