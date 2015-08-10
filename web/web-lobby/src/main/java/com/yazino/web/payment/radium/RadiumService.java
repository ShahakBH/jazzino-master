package com.yazino.web.payment.radium;

import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.email.AsyncEmailService;
import com.yazino.web.domain.email.EarnedChipsEmailBuilder;
import com.yazino.web.service.QuietPlayerEmailer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.ServletException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

@Service
public class RadiumService {
    private static final Logger LOG = LoggerFactory.getLogger(RadiumService.class);
    private final RadiumValidationService radiumValidationService;
    private final BigDecimal chipsPerDollar;
    private final PlayerService playerService;
    private final CommunityService communityService;
    private final WalletService walletService;
    private final AsyncEmailService emailService;
    private final String emailAddressForFinance;

    private final QuietPlayerEmailer emailer;
    private final String sender;

    @Autowired
    public RadiumService(final RadiumValidationService radiumValidationService,
                         @Value("${strata.server.lobby.radium.chipsPerDollar}") final String chipsPerDollar,
                         final PlayerService playerService,
                         final CommunityService communityService,
                         final WalletService walletService,
                         final AsyncEmailService emailService,
                         @Value("${strata.server.lobby.radium.emailAddressForFinance}") final String emailAddressForFinance,
                         final QuietPlayerEmailer emailer,
                         @Value("${strata.email.from-address}")final String sender) {
        this.radiumValidationService = radiumValidationService;
        this.emailer = emailer;
        this.sender = sender;
        this.chipsPerDollar = new BigDecimal(chipsPerDollar);
        this.playerService = playerService;
        this.communityService = communityService;
        this.walletService = walletService;
        this.emailService = emailService;
        this.emailAddressForFinance = emailAddressForFinance;
    }

    public boolean payoutChipsAndNotifyPlayer(final String chipAmount,
                                              final String appId,
                                              final String hash,
                                              final String trackId,
                                              final String userId,
                                              final String pid,
                                              final String remoteAddr)
            throws ServletException {
        LOG.info(String.format("Received payoutChipsAndNotifyPlayer from radium for user: %s "
                + "transaction: %s tracking id %s", userId, pid, trackId));

        if (!radiumValidationService.validate(userId, appId, hash)
                || !radiumValidationService.validateIp(remoteAddr)) {
            return false;
        }

        LOG.info("Successfully validated the sender as radium");


        final BigDecimal playerId = new BigDecimal(userId);
        final BigDecimal amountOfChips = new BigDecimal(chipAmount);
        final BigDecimal amountInCash = new BigDecimal(chipAmount).divide(chipsPerDollar, 2, RoundingMode.HALF_EVEN);

        try {
            final BasicProfileInformation profileInformation = playerService.getBasicProfileInformation(playerId);

            if (profileInformation == null) {
                LOG.error("could not find profile info for user " + playerId
                        + " to credit " + amountOfChips + " chips");
                return false;
            }

            if (amountInCash.compareTo(BigDecimal.ZERO) <= 0) {
                notifyFinanceDeptOfChargeback(playerId, amountInCash, pid, profileInformation, amountOfChips);
                LOG.info("Chargeback received");
                return true;
            }

            processExternalTransaction(profileInformation.getAccountId(), pid, amountInCash, amountOfChips, playerId);
            communityService.asyncPublishBalance(playerId);

            final EarnedChipsEmailBuilder builder = new EarnedChipsEmailBuilder(playerId, pid, amountOfChips);
            emailer.quietlySendEmail(builder);

        } catch (WalletServiceException e) {
            LOG.error(String.format("error on processing payoutChipsAndNotifyPlayer playerId:%s "
                    + "amount of chips:%s trackId:%s pId:%s", userId, chipAmount, trackId, pid), e);
            return false;
        }

        if (LOG.isInfoEnabled()) {
            LOG.info(String.format("Successfully awarded player[%s] with [%s] chips", playerId, amountOfChips));
        }

        return true;
    }

    private void notifyFinanceDeptOfChargeback(final BigDecimal playerId,
                                               final BigDecimal amount,
                                               final String transactionRef,
                                               final BasicProfileInformation profileInformation,
                                               final BigDecimal amountOfChips) {

        final DateTime dateTime = new DateTime();
        final String date = dateTime.toString("dd/MM/yyyy");

        final String displayName = profileInformation.getName();

        //notify the user of success
        final String subject = "Chargeback from Radium";
        final String templateName = "lobby/radium-transaction-chargeback.vm";

        final Map<String, Object> templateProperties = new HashMap<String, Object>();
        templateProperties.put("displayName", displayName);
        templateProperties.put("playerId", playerId);
        templateProperties.put("currency", amount);
        templateProperties.put("chips", amountOfChips);
        templateProperties.put("date", date);
        templateProperties.put("internalTransactionId", transactionRef);

        try {
            emailService.send(emailAddressForFinance, sender, subject, templateName, templateProperties);
        } catch (Exception e) {
            LOG.error("Failed to send email", e);
        }
    }

    private void processExternalTransaction(final BigDecimal accountId,
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
                .withCashierName("radium")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType("")
                .withPlayerId(playerId)
                .withPromotionId(null)
                .withPlatform(Platform.WEB)
                .build();
        walletService.record(externalTransaction);
    }

}
