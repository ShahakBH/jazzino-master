package com.yazino.web.payment.tapjoy;

import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.PlayerService;
import com.yazino.web.domain.email.EarnedChipsEmailBuilder;
import com.yazino.web.service.QuietPlayerEmailer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Controller which handles callbacks from Tapjoy.
 * These urls are configured at dashboard.tapjoy.com
 */
@Controller
public class TapJoyController {
    private static final Logger LOG = LoggerFactory.getLogger(TapJoyController.class);

    public static final String TAPJOY = "TAPJOY";

    public static final int FORBIDDEN = 403;
    public static final int OK = 200;
    //Tapjoy Conversion Rate $1=5000 chips so 1/5000
    public static final BigDecimal CHIP_MULTIPLIER = new BigDecimal("0.0002");
    public static final Currency CURRENCY = Currency.getInstance("USD");

    // Platform identfiers used in callback urls
    private static final String PLATFORM_IOS = "ios";
    private static final String PLATFORM_ANDROID = "android";
    private static final String PLATFORM_AMAZON = "amazon";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss");
    private static final AtomicLong ATOMIC_LONG = new AtomicLong();

    private final Map<String, String> secretKeys = new HashMap<String, String>();
    private final PlayerService playerService;
    private final WalletService walletService;
    private final QuietPlayerEmailer emailer;

    @Autowired(required = true)
    public TapJoyController(final PlayerService playerService,
                            final WalletService walletService,
                            final QuietPlayerEmailer emailer) {
        this.playerService = playerService;
        this.walletService = walletService;
        this.emailer = emailer;

        this.secretKeys.put("SLOTS_IOS", "zezlQ4Tx0ASFzmO3olgw");
        this.secretKeys.put("BLACKJACK_IOS", "Dd2rBY2s6vG2TLgsdrDa");
        this.secretKeys.put("HIGH_STAKES_IOS", "G5LrD6sJGOJsCj0WqlLB");

        this.secretKeys.put("TEXAS_HOLDEM_ANDROID", "haCdPgjvNTqxmSJlx1Gc");
        this.secretKeys.put("SLOTS_ANDROID", "P4nZdHWklgSQjJuRhUk1");
    }

    private boolean isValidPlatform(final String platform) {
        return (PLATFORM_IOS.equals(platform) || PLATFORM_ANDROID.equals(platform));
    }

    @Deprecated
    @RequestMapping("/mobile/tapjoy/callback/{platform}/blackjack")
    public void tapjoyCallBackBlackjack(@PathVariable final String platform,
                                        @RequestParam(value = "snuid", required = true) final String snuid,
                                        @RequestParam(value = "currency", required = true) final String currency,
                                        @RequestParam(value = "id", required = true) final String id,
                                        @RequestParam(value = "verifier", required = true) final String verifier,
                                        final HttpServletResponse response) throws WalletServiceException {

        tapjoyCallBack(platform, snuid, currency, id, verifier, "BLACKJACK", response);
    }

    @Deprecated
    @RequestMapping("/mobile/tapjoy/callback/{platform}/wheeldeal")
    public void tapjoyCallBackWheeldeal(@PathVariable final String platform,
                                        @RequestParam(value = "snuid", required = true) final String snuid,
                                        @RequestParam(value = "currency", required = true) final String currency,
                                        @RequestParam(value = "id", required = true) final String id,
                                        @RequestParam(value = "verifier", required = true) final String verifier,
                                        final HttpServletResponse response) throws WalletServiceException {

        tapjoyCallBack(platform, snuid, currency, id, verifier, "SLOTS", response);
    }

    @Deprecated
    @RequestMapping("/mobile/tapjoy/callback/{platform}/highstakes")
    public void tapjoyCallBackHighStakes(@PathVariable final String platform,
                                         @RequestParam(value = "snuid", required = true) final String snuid,
                                         @RequestParam(value = "currency", required = true) final String currency,
                                         @RequestParam(value = "id", required = true) final String id,
                                         @RequestParam(value = "verifier", required = true) final String verifier,
                                         final HttpServletResponse response) throws WalletServiceException {

        tapjoyCallBack(platform, snuid, currency, id, verifier, "HIGH_STAKES", response);
    }

    @Deprecated
    @RequestMapping("/mobile/tapjoy/callback/{platform}/texasholdem")
    public void tapjoyCallBackTexasHoldem(@PathVariable final String platform,
                                          @RequestParam(value = "snuid", required = true) final String snuid,
                                          @RequestParam(value = "currency", required = true) final String currency,
                                          @RequestParam(value = "id", required = true) final String id,
                                          @RequestParam(value = "verifier", required = true) final String verifier,
                                          final HttpServletResponse response) throws WalletServiceException {

        tapjoyCallBack(platform, snuid, currency, id, verifier, "TEXAS_HOLDEM", response);
    }


    @RequestMapping("/mobile/tapjoy/callback/{platform}/{application}")
    public void tapjoyCallBack(@PathVariable final String platform,
                               @RequestParam(value = "snuid", required = true) final String snuid,
                               @RequestParam(value = "currency", required = true) final String currency,
                               @RequestParam(value = "id", required = true) final String id,
                               @RequestParam(value = "verifier", required = true) final String verifier,
                               @PathVariable final String application,
                               final HttpServletResponse response) throws WalletServiceException {

        LOG.debug(String.format("platform:%s snuid:%s currency:%s id:%s verifier:%s",
                platform, snuid, currency, id, verifier));

        if (!isValidPlatform(platform)) {
            final String msg = String.format(
                    "Unsupported platform in call from tapjoy, sending failure response: "
                            + "platform: %ssnuid:%s currency:%s id:%s verifier:%s",
                    platform,
                    snuid,
                    currency,
                    id,
                    verifier);
            LOG.warn(msg);
            sendFailureResponse(response);
            return;
        }

        BigDecimal amount = null;
        BigDecimal playerId = null;

        try {
            amount = new BigDecimal(currency);
            playerId = new BigDecimal(snuid);
        } catch (NumberFormatException nex) {
            final String msg = String.format(
                    "Unexpected call from tapjoy, sending failure response: snuid:%s currency:%s id:%s verifier:%s",
                    snuid,
                    currency,
                    id,
                    verifier);

            LOG.warn(msg);
        }
        if (amount == null || playerId == null) {
            sendFailureResponse(response);
            return;
        }

        if (!verify(snuid, currency, id, verifier, platform, application)) {
            sendFailureResponse(response);
            return;
        }
        Platform derivedPlatform;

        if (PLATFORM_IOS.equals(platform)) {
            derivedPlatform = Platform.IOS;
        } else if (PLATFORM_ANDROID.equals(platform)) {
            derivedPlatform = Platform.ANDROID;
        }  else if (PLATFORM_AMAZON.equals(platform)) {
            derivedPlatform = Platform.AMAZON;
        } else {
            sendFailureResponse(response);
            return;
        }

        final BigDecimal accountId = playerService.getAccountId(playerId);
        if (accountId == null) {
            sendFailureResponse(response);
            return;
        }
        final DateTime now = new DateTime();
        final BigDecimal cashAmount = CHIP_MULTIPLIER.multiply(amount);
        final String cashierName = TAPJOY + "_" + platform.toUpperCase();
        final String internalId = buildInternalId(now, accountId, cashierName);

        final ExternalTransaction externalTransaction = ExternalTransaction.newExternalTransaction(accountId)
                .withInternalTransactionId(internalId)
                .withExternalTransactionId(id)
                .withMessage(snuid, now)
                .withAmount(CURRENCY, cashAmount)
                .withPaymentOption(amount, null)
                .withCreditCardNumber("x-x-x")
                .withCashierName(cashierName)
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(application)
                .withPlayerId(playerId)
                .withPromotionId(null)
                .withPlatform(derivedPlatform)
                .build();

        walletService.record(externalTransaction);
        response.setStatus(OK);

        final EarnedChipsEmailBuilder builder = new EarnedChipsEmailBuilder(playerId, id, amount);
        emailer.quietlySendEmail(builder);
    }


    String buildInternalId(final DateTime messageTimeStamp,
                           final BigDecimal accountId,
                           final String productIdentifier) {
        final DateTime dtLondon = messageTimeStamp.withZone(DateTimeZone.forID("Europe/London"));
        final String date = DATE_TIME_FORMATTER.print(dtLondon);
        final long nextIncrement = ATOMIC_LONG.getAndIncrement();
        return String.format("%s_%s_%s_%s", productIdentifier, accountId.toPlainString(), date, nextIncrement);
    }

    private void sendFailureResponse(final HttpServletResponse response) {
        response.setStatus(FORBIDDEN);
    }

    private boolean verify(final String snuid,
                           final String currency,
                           final String id,
                           final String verifier,
                           final String platform,
                           final String application) {

        final String secret = this.secretKeys.get(application + "_" + platform.toUpperCase());
        if (isEmpty(secret)) {
            return false;
        }

        final String concatenatedString = id + ":" + snuid + ":" + currency + ":" + secret;
        final String hashedString = DigestUtils.md5DigestAsHex(concatenatedString.getBytes());
        final boolean hashMatches = verifier.equals(hashedString);
        LOG.debug(String.format("verify(concatenatedString:%s verifier:%s)", concatenatedString, verifier));
        if (hashMatches) {
            LOG.debug("verified");
            return true;
        }
        LOG.debug("NOT verified");

        return false;
    }

}
