package com.yazino.web.payment.flurry;

import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.PlayerService;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.domain.email.EarnedChipsEmailBuilder;
import com.yazino.web.service.QuietPlayerEmailer;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


@Controller
@AllowPublicAccess
@RequestMapping("/payment/flurry/*")
public class EarnFreeChipsController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss");
    private static final AtomicLong ATOMIC_LONG = new AtomicLong();
    public static final String FLURRY_CLIPS_EARN_FREE_CHIPS_VIDEO_HOOK = "FLURRY_CLIPS_EARN_FREE_CHIPS_VIDEO_HOOK";
    public static final String NO_CONFIG = "NO_CONFIG";
    public static final String INVALID_TICKET = "INVALID_TICKET";
    public static final String NO_SESSION = "NO_SESSION";
    public static final String NO_PROFILE_FOUND = "NO_PROFILE_FOUND";
    public static final String FLURRY_MOBILE = "FLURRY_MOBILE";
    public static final int TWO_HUNDRED = 200;
    public static final BigDecimal CHIP_MULTIPLIER = new BigDecimal("0.0001");
    public static final Currency CURRENCY = Currency.getInstance("USD");
    private final Map<String, EarnFreeChipsConfiguration> configurations = new HashMap<String, EarnFreeChipsConfiguration>();
    private final WebApiResponses responseWriter;
    private FreeChipsTickets ticketMachine = defaultTicketMachine();

    private final WalletService walletService;
    private final PlayerService playerService;
    private final LobbySessionCache lobbySessionCache;
    private final QuietPlayerEmailer emailer;

    @Autowired(required = true)
    public EarnFreeChipsController(
            WebApiResponses responseWriter, final WalletService walletService,
            final PlayerService playerService,
            final LobbySessionCache lobbySessionCache,
            final QuietPlayerEmailer emailer) {
        this.responseWriter = responseWriter;
        final EarnFreeChipsConfiguration flurryConfig = new EarnFreeChipsConfiguration(FLURRY_CLIPS_EARN_FREE_CHIPS_VIDEO_HOOK, TWO_HUNDRED);
        this.configurations.put(FLURRY_CLIPS_EARN_FREE_CHIPS_VIDEO_HOOK, flurryConfig);
        this.playerService = playerService;
        this.walletService = walletService;
        this.lobbySessionCache = lobbySessionCache;
        this.emailer = emailer;
    }

    public void setTicketMachine(final FreeChipsTickets ticketMachine) {
        this.ticketMachine = ticketMachine;
    }

    @RequestMapping("/configuration")
    public void earnFreeChipsConfigurationForType(@RequestParam(value = "code", required = false) final String code,
                                                  final HttpServletResponse response) throws IOException {
        final EarnFreeChipsConfiguration config = findFreeChipsConfigurationForCode(code);
        this.responseWriter.writeOk(response, config);
    }

    private EarnFreeChipsConfiguration findFreeChipsConfigurationForCode(final String code) {
        EarnFreeChipsConfiguration config = this.configurations.get(code);
        if (config == null) {
            config = new EarnFreeChipsConfiguration(code, 0);
        }
        return config;
    }

    @RequestMapping("/issueTicket")
    public void issueTicket(final HttpServletResponse response)
            throws IOException, IllegalBlockSizeException, InvalidKeyException,
            NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException,
            InvalidAlgorithmParameterException, NoSuchProviderException {
        final FreeChipTicket ticket = new FreeChipTicket(this.ticketMachine.newTicket());
        this.responseWriter.writeOk(response, ticket);
    }

    @RequestMapping("/awardFreeChips")
    public void awardFreeChips(@RequestParam(value = "code", required = true) final String code,
                               @RequestParam(value = "signedTicket", required = true) final String signedTicket,
                               @RequestParam(value = "application", required = false, defaultValue = "UNKNOWN") final String application,
                               final HttpServletRequest request,
                               final HttpServletResponse response)
            throws IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
            BadPaddingException, IOException, WalletServiceException {

        final EarnFreeChipsConfiguration config = findFreeChipsConfigurationForCode(code);

        if (config.getChips() <= 0) {
            this.responseWriter.writeOk(response, new FreeChipTicketRedemption(false, 0, NO_CONFIG, null));
            return;
        }

        final boolean isValidTicket = ticketMachine.checkTicketAndRemove(signedTicket, true);
        if (!isValidTicket) {
            final FreeChipTicketRedemption redemption = new FreeChipTicketRedemption(
                    isValidTicket,
                    config.getChips(),
                    INVALID_TICKET,
                    null);

            this.responseWriter.writeOk(response, redemption);
            return;
        }

        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session == null) {
            final FreeChipTicketRedemption redemption = new FreeChipTicketRedemption(
                    false,
                    config.getChips(),
                    NO_SESSION,
                    null);

            this.responseWriter.writeOk(response, redemption);
            return;
        }

        final BigDecimal accountId = playerService.getAccountId(session.getPlayerId());

        if (accountId == null) {
            final FreeChipTicketRedemption redemption = new FreeChipTicketRedemption(
                    false,
                    config.getChips(),
                    NO_PROFILE_FOUND,
                    null);

            this.responseWriter.writeOk(
                    response,
                    redemption);
            return;
        }
        final DateTime now = new DateTime();
        final String internalId = buildInternalId(now, accountId, FLURRY_MOBILE);
        final BigDecimal chips = BigDecimal.valueOf(config.chips);
        final BigDecimal cashAmount = CHIP_MULTIPLIER.multiply(chips);
        final ExternalTransaction externalTransaction = ExternalTransaction.newExternalTransaction(accountId)
                .withInternalTransactionId(internalId)
                .withExternalTransactionId(internalId)
                .withMessage(signedTicket, now)
                .withAmount(CURRENCY, cashAmount)
                .withPaymentOption(chips, null)
                .withCreditCardNumber("x-x-x")
                .withCashierName(FLURRY_MOBILE)
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(application)
                .withPlayerId(session.getPlayerId())
                .withSessionId(session.getSessionId())
                .withPromotionId(null)
                .withPlatform(Platform.ANDROID)
                .build();

        final BigDecimal newBalance = walletService.record(externalTransaction);

        this.responseWriter.writeOk(response, new FreeChipTicketRedemption(true, config.getChips(), null, newBalance));

        final EarnedChipsEmailBuilder builder = new EarnedChipsEmailBuilder(session.getPlayerId(),
                internalId, BigDecimal.valueOf(config.getChips()));
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

    public WebApiResponses getResponseWriter() {
        return responseWriter;
    }

    private static FreeChipsTickets defaultTicketMachine() {
        try {
            return new FreeChipsTickets();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class FreeChipTicketRedemption {
        private final boolean successful;
        private final int chipsAwarded;
        private final String message;
        private final BigDecimal newBalance;


        public FreeChipTicketRedemption(final boolean successful, final int chipsAwarded,
                                        final String message, final BigDecimal newBalance) {
            this.successful = successful;
            this.chipsAwarded = chipsAwarded;
            this.message = message;
            this.newBalance = newBalance;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public int getChipsAwarded() {
            return chipsAwarded;
        }

        public String getMessage() {
            return message;
        }

        public BigDecimal getNewBalance() {
            return newBalance;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FreeChipTicketRedemption that = (FreeChipTicketRedemption) o;
            return new EqualsBuilder()
                    .append(successful, that.successful)
                    .append(chipsAwarded, that.chipsAwarded)
                    .append(message, that.message)
                    .append(newBalance, that.newBalance)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(successful)
                    .append(chipsAwarded)
                    .append(message)
                    .append(newBalance)
                    .hashCode();
        }

        public String toString() {
            return ReflectionToStringBuilder.reflectionToString(this);
        }

    }

    public static class EarnFreeChipsConfiguration {
        private final String code;
        private final int chips;

        public EarnFreeChipsConfiguration(final String code, final int chips) {
            this.code = code;
            this.chips = chips;
        }

        public String getCode() {
            return code;
        }

        public int getChips() {
            return chips;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final EarnFreeChipsConfiguration that = (EarnFreeChipsConfiguration) o;
            return new EqualsBuilder()
                    .append(chips, that.chips)
                    .append(code, that.code)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(chips)
                    .append(code)
                    .hashCode();
        }
    }

    public static class FreeChipTicket {
        private final String ticket;

        public FreeChipTicket(final String ticket) {
            this.ticket = ticket;
        }

        public String getTicket() {
            return ticket;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FreeChipTicket that = (FreeChipTicket) o;
            return new EqualsBuilder()
                    .append(ticket, that.ticket)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(ticket)
                    .hashCode();
        }

    }


}
