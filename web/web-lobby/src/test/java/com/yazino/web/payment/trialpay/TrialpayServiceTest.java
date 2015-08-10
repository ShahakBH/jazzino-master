package com.yazino.web.payment.trialpay;

import com.yazino.email.EmailException;
import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PlayerService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.email.EarnedChipsEmailBuilder;
import com.yazino.web.service.PurchaseTracking;
import com.yazino.web.service.QuietPlayerEmailer;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.ServletException;
import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TrialpayServiceTest {
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(123);
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(1940);

    private TrialpayValidator trialPayValidationService = mock(TrialpayValidator.class);
    private WalletService walletService = mock(WalletService.class);
    private PlayerService playerService = mock(PlayerService.class);
    private CommunityService communityService = mock(CommunityService.class);
    private final QuietPlayerEmailer emailer = mock(QuietPlayerEmailer.class);
    private PurchaseTracking purchaseTracking = mock(PurchaseTracking.class);

    private TrialpayService underTest;
    private String body;
    private BigDecimal rewardAmount = BigDecimal.ONE;
    private BigDecimal revenue = BigDecimal.TEN;
    private String transactionRef = "CW89CWY";
    private String expectedHash = "XYZ";

    @Before
    public void before() {

        underTest = new TrialpayService(
                trialPayValidationService,
                communityService,
                walletService,
                playerService,
                emailer,
                purchaseTracking);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(10000);

        body = "oid=CW89CWY&sid=1940&reward_amount=1&revenue=10";
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = ServletException.class)
    public void callbackShouldThrowExceptionIfCannotValidateSender() throws WalletServiceException, ServletException {
        when(trialPayValidationService.validate(expectedHash.toLowerCase(), body)).thenReturn(false);

        underTest.payoutChipsAndNotifyPlayer(PLAYER_ID, rewardAmount, revenue, transactionRef, expectedHash);
    }

    @Test
    public void callbackShouldUpdateBalanceAndReturnSuccessView() throws WalletServiceException, ServletException {

        ExternalTransaction expectedExternalTransaction = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId("CW89CWY")
                .withExternalTransactionId("CW89CWY")
                .withMessage("", new DateTime())
                .withAmount(Currency.getInstance("USD"), revenue)
                .withPaymentOption(rewardAmount, null)
                .withCreditCardNumber("")
                .withCashierName("Trialpay")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType("")
                .withPlayerId(PLAYER_ID)
                .withPromotionId(null)
                .withPlatform(Platform.WEB)
                .build();

        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        when(trialPayValidationService.validate(expectedHash, body)).thenReturn(true);

        assertEquals("payment/trialpay/success",
                underTest.payoutChipsAndNotifyPlayer(PLAYER_ID, rewardAmount, revenue, transactionRef, expectedHash));

        verify(walletService).record(expectedExternalTransaction);
        verify(communityService).asyncPublishBalance(PLAYER_ID);
    }

    @Test
    public void callbackShouldEmailPlayer() throws EmailException, WalletServiceException, ServletException {
        String body = "oid=CW89CWY&sid=1940&reward_amount=1500&revenue=10";

        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        when(playerService.getBasicProfileInformation(PLAYER_ID))
                .thenReturn(new BasicProfileInformation(PLAYER_ID, "name", "pictureUrl", ACCOUNT_ID));

        when(trialPayValidationService.validate(expectedHash, body)).thenReturn(true);

        underTest.payoutChipsAndNotifyPlayer(PLAYER_ID, BigDecimal.valueOf(1500), revenue, transactionRef, expectedHash);

        ArgumentCaptor<EarnedChipsEmailBuilder> captor = ArgumentCaptor.forClass(EarnedChipsEmailBuilder.class);
        verify(emailer).quietlySendEmail(captor.capture());

        EarnedChipsEmailBuilder builder = captor.getValue();
        Assert.assertEquals("1,500", builder.getEarnedChips());
        Assert.assertEquals("CW89CWY", builder.getIdentifier());
    }

    @Test
    public void callbackShouldTrackPurchase() throws WalletServiceException, ServletException {
        String body = "oid=CW89CWY&sid=1940&reward_amount=1500&revenue=10";

        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        when(playerService.getBasicProfileInformation(PLAYER_ID))
                .thenReturn(new BasicProfileInformation(PLAYER_ID, "name", "pictureUrl", ACCOUNT_ID));

        when(trialPayValidationService.validate(expectedHash, body)).thenReturn(true);

        underTest.payoutChipsAndNotifyPlayer(PLAYER_ID, BigDecimal.valueOf(1500), revenue, transactionRef, expectedHash);
        verify(purchaseTracking).trackSuccessfulPurchase(PLAYER_ID);
    }
    
}
