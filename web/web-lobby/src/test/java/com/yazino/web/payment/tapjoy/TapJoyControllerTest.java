package com.yazino.web.payment.tapjoy;

import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.community.PlayerService;
import com.yazino.web.domain.email.EarnedChipsEmailBuilder;
import com.yazino.web.service.QuietPlayerEmailer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TapJoyControllerTest {
    public static final String SNUID = "123";
    public static final String ID = "foo";
    public static final String CHIPS = "120.5";
    public static final String VERIFIER_BASE_SLOTS = "foo:123:120.5:zezlQ4Tx0ASFzmO3olgw";
    public static final String VERIFIER_BASE_TEXASHOLDEM = "foo:123:120.5:haCdPgjvNTqxmSJlx1Gc";
    public static final int PLAYER_ID = 123;
    public static final BigDecimal ACCOUNT_ID = BigDecimal.TEN;
    public static final int FAIL = 403;
    public static final int OK = 200;
    public static final String APPLICATION_SLOTS = "SLOTS";
    public static final String APPLICATION_ANDROID = "TEXAS_HOLDEM";

    public static final String PLATFORM_IOS = "ios";
    public static final String PLATFORM_ANDROID = "android";

    public static final String TAPJOY_MARKETPLACE_IOS = "TAPJOY_IOS";
    public static final String TAPJOY_MARKETPLACE_ANDROID = "TAPJOY_ANDROID";

    private final PlayerService playerService = mock(PlayerService.class);
    private final WalletService walletService = mock(WalletService.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final QuietPlayerEmailer emailer = mock(QuietPlayerEmailer.class);
    private final TapJoyController underTest = new TapJoyController(playerService, walletService, emailer);

    private final BigDecimal playerID = BigDecimal.valueOf(PLAYER_ID);
    private final String verifierSlots = DigestUtils.md5DigestAsHex(VERIFIER_BASE_SLOTS.getBytes());
    private final String verifierTexasHoldem = DigestUtils.md5DigestAsHex(VERIFIER_BASE_TEXASHOLDEM.getBytes());

    @Before
    public void setUp() {
        when(playerService.getAccountId(playerID)).thenReturn(ACCOUNT_ID);
    }

    @Test
    public void shouldlogExternalTransactionIOS() throws Exception {
        ArgumentCaptor<ExternalTransaction> captor = ArgumentCaptor.forClass(ExternalTransaction.class);

        underTest.tapjoyCallBack(PLATFORM_IOS, SNUID, CHIPS, ID, verifierSlots, APPLICATION_SLOTS, response);

        verify(walletService).record(captor.capture());
        BigDecimal chipAmount = new BigDecimal(CHIPS);
        BigDecimal cashAmount = chipAmount.multiply(TapJoyController.CHIP_MULTIPLIER);
        assertEquals(captor.getValue().getAccountId(),ACCOUNT_ID);
        assertEquals(captor.getValue().getAmountChips(),chipAmount);
        assertEquals(captor.getValue().getAmountCash(), cashAmount);
        assertEquals(captor.getValue().getCashierName(), TAPJOY_MARKETPLACE_IOS);
    }

    @Test
    public void shouldlogExternalTransactionAndroid() throws Exception {
        ArgumentCaptor<ExternalTransaction> captor = ArgumentCaptor.forClass(ExternalTransaction.class);

        underTest.tapjoyCallBack(PLATFORM_ANDROID, SNUID, CHIPS, ID, verifierTexasHoldem, APPLICATION_ANDROID, response);

        verify(walletService).record(captor.capture());
        BigDecimal chipAmount = new BigDecimal(CHIPS);
        BigDecimal cashAmount = chipAmount.multiply(TapJoyController.CHIP_MULTIPLIER);
        assertEquals(captor.getValue().getAccountId(),ACCOUNT_ID);
        assertEquals(captor.getValue().getAmountChips(),chipAmount);
        assertEquals(captor.getValue().getAmountCash(), cashAmount);
        assertEquals(captor.getValue().getCashierName(), TAPJOY_MARKETPLACE_ANDROID);
    }

    @Test
    public void shouldSendFailureResponseIfInvalidPlatform() throws Exception {

        final HttpServletResponse response1 = mock(HttpServletResponse.class);
        underTest.tapjoyCallBack(PLATFORM_IOS, SNUID, CHIPS, ID, verifierSlots, APPLICATION_SLOTS, response1);
        verify(response1).setStatus(OK);

        final HttpServletResponse response2 = mock(HttpServletResponse.class);
        underTest.tapjoyCallBack(PLATFORM_ANDROID, SNUID, CHIPS, ID, verifierTexasHoldem, APPLICATION_ANDROID, response2);
        verify(response2).setStatus(OK);

        final HttpServletResponse response3 = mock(HttpServletResponse.class);
        underTest.tapjoyCallBack("PLATFORM_X", SNUID, CHIPS, ID, verifierSlots, APPLICATION_SLOTS, response3);
        verify(response3).setStatus(FAIL);
    }

    @Test
    public void shouldSendOKResponseIfNotVerified() throws Exception{
        underTest.tapjoyCallBack(PLATFORM_IOS, SNUID, CHIPS, ID, verifierSlots, APPLICATION_SLOTS, response);
        verify(response).setStatus(OK);
    }

    @Test
    public void shouldSendFailureResponseIfNotVerified() throws Exception{
        underTest.tapjoyCallBack(PLATFORM_IOS, SNUID, CHIPS, ID + "bar", verifierSlots, APPLICATION_SLOTS,response);
        verify(response).setStatus(FAIL);
    }

    @Test
    public void shouldSendFailureResponseIfSnuidNotPlayerId() throws Exception{
        underTest.tapjoyCallBack(PLATFORM_IOS, "ok", CHIPS, ID, verifierSlots, APPLICATION_SLOTS, response);
        verify(response).setStatus(FAIL);
    }

    @Test
    public void shouldNotAddAmountToBalanceIfNotVerified() throws Exception{
        underTest.tapjoyCallBack(PLATFORM_IOS, SNUID, CHIPS, ID + "bar", verifierSlots, APPLICATION_SLOTS, response);
        verify(walletService, never()).postTransaction(Matchers.<BigDecimal>any(), Matchers.<BigDecimal>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.any(TransactionContext.class));
    }

    @Test
    public void shouldSendFailureResponseIfAccountNotFound() throws Exception{
        when(playerService.getAccountId(playerID)).thenReturn(null);
        underTest.tapjoyCallBack(PLATFORM_IOS, SNUID, CHIPS, ID, verifierSlots, APPLICATION_SLOTS,response);
        verify(response).setStatus(FAIL);
    }

    @Test
    public void shouldNotAddAmountToBalanceIfAccountNotFound() throws Exception{
        when(playerService.getBasicProfileInformation(playerID)).thenReturn(null);
        underTest.tapjoyCallBack(PLATFORM_IOS, SNUID, CHIPS, ID, verifierSlots, APPLICATION_SLOTS,response);
        verify(walletService, never()).postTransaction(Matchers.<BigDecimal>any(), Matchers.<BigDecimal>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.any(TransactionContext.class));
    }

    @Test
    public void shouldAttemptToSendEmailOnSuccessfulRecord() throws Exception {
        underTest.tapjoyCallBack(PLATFORM_IOS, SNUID, CHIPS, ID, verifierSlots, APPLICATION_SLOTS, response);
        ArgumentCaptor<EarnedChipsEmailBuilder> captor = ArgumentCaptor.forClass(EarnedChipsEmailBuilder.class);
        verify(emailer).quietlySendEmail(captor.capture());
        EarnedChipsEmailBuilder builder = captor.getValue();
        assertEquals(playerID, builder.getPlayerId());
        assertEquals(ID, builder.getIdentifier());
        assertEquals("120", builder.getEarnedChips());
//        verify(emailService).sendConfirmationOfEarnedChipsEmail(emailAddress, firstName, new BigDecimal(COST), ID);
    }

}
