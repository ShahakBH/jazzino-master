package com.yazino.web.payment.radium;

import com.yazino.email.EmailException;
import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.email.AsyncEmailService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.email.EarnedChipsEmailBuilder;
import com.yazino.web.service.QuietPlayerEmailer;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import javax.servlet.ServletException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RadiumServiceTest {

    public static final String IP_ADDRESS = "123.123.123.123";
    private RadiumValidationService radiumValidationService = mock(RadiumValidationService.class);
    private WalletService walletService = mock(WalletService.class);
    private PlayerService playerDetailsService = mock(PlayerService.class);
    private CommunityService communityService = mock(CommunityService.class);
    private AsyncEmailService emailService = mock(AsyncEmailService.class);
    private final QuietPlayerEmailer emailer = mock(QuietPlayerEmailer.class);

    private RadiumService underTest;
    private BigDecimal accountId = new BigDecimal(321);
    private BigDecimal playerId = new BigDecimal(666);
    private ArgumentCaptor<ExternalTransaction> captor = ArgumentCaptor.forClass(ExternalTransaction.class);

    @Before
    public void before() throws IOException {
        underTest = new RadiumService(
                radiumValidationService,
                "3000",
                playerDetailsService,
                communityService,
                walletService,
                emailService,
                "payments@yazino.com",
                emailer, "from@your.mum");

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(10000);

        when(playerDetailsService.getBasicProfileInformation(playerId)).thenReturn(new BasicProfileInformation(playerId, "name", "pictureUrl", accountId));
        when(radiumValidationService.validateIp("123.123.123.123")).thenReturn(true);

    }

    @After
    public void after() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();

    }

    @Test
    public void callbackShouldThrowExceptionIfCannotValidateSender() throws WalletServiceException, ServletException {
        when(radiumValidationService.validate(anyString(), anyString(), anyString())).thenReturn(false);

        assertFalse(underTest.payoutChipsAndNotifyPlayer("", "", "", "", "user", "pid", IP_ADDRESS));


    }


    @Test
    public void callbackShouldReturnFailureIfCannotValidateSendersIp() throws WalletServiceException, ServletException {
        when(radiumValidationService.validate(anyString(), anyString(), anyString())).thenReturn(true);
        when(radiumValidationService.validateIp("123.123.123.123")).thenReturn(false);

        assertFalse(underTest.payoutChipsAndNotifyPlayer("", "", "", "", "user", "pid", IP_ADDRESS));
    }

    @Test
    public void callbackShouldThrowExceptionIfCannotFindPlayer() throws WalletServiceException, ServletException {
        when(radiumValidationService.validate(anyString(), anyString(), anyString())).thenReturn(true);
        when(playerDetailsService.getBasicProfileInformation(playerId)).thenReturn(new BasicProfileInformation(playerId, "name", "pictureUrl", accountId));

        assertFalse(underTest.payoutChipsAndNotifyPlayer("5", "", "", "", "123", "321", IP_ADDRESS));
    }

    @Test
    public void amountOfIncomingFromRadiumShouldBeConvertedToCentsAndMultipliedUpToChips() throws WalletServiceException, ServletException {
        when(radiumValidationService.validate(anyString(), anyString(), anyString())).thenReturn(true);

        assertTrue(underTest.payoutChipsAndNotifyPlayer("1500", "6131", "XYZ", "abc123", "666", "789", IP_ADDRESS));
        verify(walletService).record(captor.capture());
        assertThat(captor.getValue().getAmountChips(), is(equalTo(BigDecimal.valueOf(1500))));
        assertThat(captor.getValue().getAmountCash(), is(equalTo(new BigDecimal("0.50"))));
    }

    @Test
    public void callbackShouldUpdateBalanceAndReturnSuccessView() throws WalletServiceException, ServletException {
        when(radiumValidationService.validate(anyString(), anyString(), anyString())).thenReturn(true);
        ExternalTransaction externalTransaction = ExternalTransaction.newExternalTransaction(accountId)
                .withInternalTransactionId("789")
                .withExternalTransactionId("789")
                .withMessage("", new DateTime())
                .withAmount(Currency.getInstance("USD"), new BigDecimal("2.00"))
                .withPaymentOption(BigDecimal.valueOf(6000), null)
                .withCreditCardNumber("")
                .withCashierName("radium")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType("")
                .withPlayerId(playerId)
                .withPromotionId(null)
                .withPlatform(Platform.WEB)
                .build();
        when(playerDetailsService.getBasicProfileInformation(playerId)).thenReturn(new BasicProfileInformation(playerId, "name", "pictureUrl", accountId));
        when(radiumValidationService.validate(anyString(), anyString(), anyString())).thenReturn(true);

        assertTrue(underTest.payoutChipsAndNotifyPlayer("6000", "6131", "XYZ", "abc123", "666", "789", IP_ADDRESS));
        verify(communityService).asyncPublishBalance(playerId);
        verify(walletService).record(externalTransaction);
    }

    @Test
    public void callbackShouldEmailPlayer() throws EmailException, WalletServiceException, ServletException {
        when(playerDetailsService.getBasicProfileInformation(playerId)).thenReturn(new BasicProfileInformation(playerId, "name", "pictureUrl", accountId));
        when(radiumValidationService.validate(anyString(), anyString(), anyString())).thenReturn(true);
        String pid = "789";
        assertTrue(underTest.payoutChipsAndNotifyPlayer("1500", "6131", "XYZ", "abc123", "666", pid, IP_ADDRESS));

        ArgumentCaptor<EarnedChipsEmailBuilder> captor = ArgumentCaptor.forClass(EarnedChipsEmailBuilder.class);
        verify(emailer).quietlySendEmail(captor.capture());

        EarnedChipsEmailBuilder builder = captor.getValue();
        assertEquals("1,500", builder.getEarnedChips());
        assertEquals(pid, builder.getIdentifier());
    }

    @Test
    public void callbackShouldDealWithChargeback() throws WalletServiceException, ServletException, EmailException {
        when(playerDetailsService.getBasicProfileInformation(playerId)).thenReturn(new BasicProfileInformation(playerId, "name", "pictureUrl", accountId));
        when(radiumValidationService.validate(anyString(), anyString(), anyString())).thenReturn(true);

        assertTrue(underTest.payoutChipsAndNotifyPlayer("-1500", "6131", "XYZ", "abc123", "666", "789", IP_ADDRESS));
        //this is a big deal m'kay
        verify(emailService).send(eq("payments@yazino.com"), Matchers.<String>any(), anyString(), Matchers.<String>any(), Matchers.<Map<String, Object>>any());
        verify(walletService, never()).postTransaction(Matchers.<BigDecimal>any(), Matchers.<BigDecimal>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.any(TransactionContext.class));
        verify(communityService, never()).asyncPublishBalance(Matchers.<BigDecimal>any());
        verify(walletService, never()).record(Matchers.<ExternalTransaction>any());
    }
}
