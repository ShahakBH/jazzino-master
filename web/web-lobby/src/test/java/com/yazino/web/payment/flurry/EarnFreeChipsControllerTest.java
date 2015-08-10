package com.yazino.web.payment.flurry;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.PlayerService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.email.EarnedChipsEmailBuilder;
import com.yazino.web.service.QuietPlayerEmailer;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.yazino.web.payment.flurry.EarnFreeChipsController.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class EarnFreeChipsControllerTest {
    private static final String TICKET_SIGNED = "foo";
    private static final String APPLICATION = "wheeldeal";
    private static final String TRANACTION_TYPE = "FLURRY_MOBILE";

    private static final BigDecimal accountId = BigDecimal.ONE;
    private static final BigDecimal playerId = BigDecimal.TEN;
    private static final BigDecimal balance = BigDecimal.valueOf(1000);
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);

    private final WebApiResponses responseHelper = mock(WebApiResponses.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FreeChipsTickets ticketMachine = mock(FreeChipsTickets.class);
    private final WalletService walletService = mock(WalletService.class);
    private final PlayerService playerService = mock(PlayerService.class);
    private final LobbySessionCache lobbySessionCache = mock(LobbySessionCache.class);
    private final LobbySession lobbySession = new LobbySession(SESSION_ID, playerId, "name", "session", Partner.YAZINO, "pic", "email", null, false,
            Platform.IOS, AuthProvider.YAZINO);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final QuietPlayerEmailer emailer = mock(QuietPlayerEmailer.class);

    private final EarnFreeChipsController underTest = new EarnFreeChipsController(responseHelper, walletService, playerService, lobbySessionCache, emailer);

    @Before
    public void setUp() throws Exception {
        underTest.setTicketMachine(ticketMachine);
        when(playerService.getAccountId(playerId)).thenReturn(accountId);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
        when(walletService.record(Matchers.<ExternalTransaction>any())).thenReturn(balance);
        when(ticketMachine.checkTicketAndRemove(TICKET_SIGNED, true)).thenReturn(true);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis());
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldWriteFreeChipsConfigurationForFlurry() throws Exception {
        underTest.earnFreeChipsConfigurationForType(FLURRY_CLIPS_EARN_FREE_CHIPS_VIDEO_HOOK, response);
        EarnFreeChipsController.EarnFreeChipsConfiguration expected = new EarnFreeChipsController.EarnFreeChipsConfiguration(
                FLURRY_CLIPS_EARN_FREE_CHIPS_VIDEO_HOOK,
                200);
        verify(responseHelper).writeOk(response, expected);
    }

    @Test
    public void shouldWriteZeroChipConfigurationIfNotFound() throws Exception {
        underTest.earnFreeChipsConfigurationForType("foo", response);
        EarnFreeChipsController.EarnFreeChipsConfiguration expected = new EarnFreeChipsController.EarnFreeChipsConfiguration("foo", 0);
        verify(responseHelper).writeOk(response, expected);
    }

    @Test
    public void shouldIssueTicket() throws Exception {
        when(ticketMachine.newTicket()).thenReturn(TICKET_SIGNED);
        underTest.issueTicket(response);
        EarnFreeChipsController.FreeChipTicket expected = new EarnFreeChipsController.FreeChipTicket(TICKET_SIGNED);
        verify(responseHelper).writeOk(response, expected);
    }

    @Test
    public void shouldRedeemTicketForValidCodeAndSignedTicket() throws Exception {
        EarnFreeChipsController.FreeChipTicketRedemption expected = new EarnFreeChipsController.FreeChipTicketRedemption(true,
                200,
                null,
                balance);
        makeGoodAwardChipsRequest();

        ArgumentCaptor<ExternalTransaction> captor = ArgumentCaptor.forClass(ExternalTransaction.class);
        verify(walletService).record(captor.capture());

        verifyExternalTransaction(captor.getValue());
        verify(responseHelper).writeOk(response, expected);
    }

    @Test
    public void shouldNotRedeemTicketForInValidCodeAndCorrectlySignedTicket() throws Exception {
        EarnFreeChipsController.FreeChipTicketRedemption expected = new EarnFreeChipsController.FreeChipTicketRedemption(false,
                0,
                NO_CONFIG,
                null);
        underTest.awardFreeChips("bar", TICKET_SIGNED, APPLICATION, request, response);
        verifyNoWalletTransaction();
        verify(responseHelper).writeOk(response, expected);
    }

    @Test
    public void shouldNotRedeemTicketForValidCodeAndIncorrectlySignedTicket() throws Exception {
        when(ticketMachine.checkTicketAndRemove(TICKET_SIGNED, true)).thenReturn(false);
        EarnFreeChipsController.FreeChipTicketRedemption expected = new EarnFreeChipsController.FreeChipTicketRedemption(false,
                200,
                INVALID_TICKET,
                null);
        makeGoodAwardChipsRequest();
        verifyNoWalletTransaction();
        verify(responseHelper).writeOk(response, expected);
    }

    @Test
    public void shouldNotRedeemTicketWhenNoLobbySessionFound() throws Exception {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);
        EarnFreeChipsController.FreeChipTicketRedemption expected = new EarnFreeChipsController.FreeChipTicketRedemption(false,
                200,
                NO_SESSION,
                null);
        makeGoodAwardChipsRequest();
        verifyNoWalletTransaction();
        verify(responseHelper).writeOk(response, expected);
    }

    @Test
    public void shouldNotRedeemTicketWhenNoProfileFound() throws Exception {
        when(playerService.getAccountId(playerId)).thenReturn(null);
        EarnFreeChipsController.FreeChipTicketRedemption expected = new EarnFreeChipsController.FreeChipTicketRedemption(false,
                200,
                NO_PROFILE_FOUND,
                null);
        makeGoodAwardChipsRequest();
        verifyNoWalletTransaction();
        verify(responseHelper).writeOk(response, expected);
    }

    @Test
    public void shouldSendEmailAfterAwardingChips() throws Exception {
        makeGoodAwardChipsRequest();
        verify(emailer).quietlySendEmail(isA(EarnedChipsEmailBuilder.class));
    }

    @Test
    public void shouldPostExternalTransaction() throws Exception {
        makeGoodAwardChipsRequest();
        ArgumentCaptor<ExternalTransaction> captor = ArgumentCaptor.forClass(ExternalTransaction.class);
        verify(walletService).record(captor.capture());

        verifyExternalTransaction(captor.getValue());
    }

    private void verifyExternalTransaction(final ExternalTransaction tranny) {
        assertThat(tranny.getAccountId(), equalTo(accountId));
        assertThat(tranny.getMessageTimeStamp(), equalTo(new DateTime()));
        assertThat(tranny.getAmount().getCurrency(), equalTo(CURRENCY));
        assertThat(tranny.getAmount().getQuantity(), equalTo(new BigDecimal("0.0200")));
        assertThat(tranny.getAmountChips(), equalTo(new BigDecimal("200")));
        assertThat(tranny.getObscuredCreditCardNumber(), equalTo("x-x-x"));
        assertThat(tranny.getCreditCardObscuredMessage(), equalTo(TICKET_SIGNED));
        assertThat(tranny.getCashierName(), equalTo(FLURRY_MOBILE));
        assertThat(tranny.getStatus(), equalTo(ExternalTransactionStatus.SUCCESS));
        assertThat(tranny.getType(), equalTo(ExternalTransactionType.DEPOSIT));
        assertThat(tranny.getGameType(), equalTo(APPLICATION));
        assertThat(tranny.getPlayerId(), equalTo(playerId));
        assertThat(tranny.getPromoId(), nullValue());
        assertThat(tranny.getPlatform(), equalTo(Platform.ANDROID));
    }

    private void verifyNoWalletTransaction() throws WalletServiceException {
        verify(walletService, never()).postTransaction(Matchers.<BigDecimal>any(),
                Matchers.<BigDecimal>any(),
                Matchers.<String>any(),
                Matchers.<String>any(),
                Matchers.any(TransactionContext.class));
    }

    private void makeGoodAwardChipsRequest() throws IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException, IOException, WalletServiceException {
        underTest.awardFreeChips(FLURRY_CLIPS_EARN_FREE_CHIPS_VIDEO_HOOK, TICKET_SIGNED, APPLICATION, request, response);
    }

}

