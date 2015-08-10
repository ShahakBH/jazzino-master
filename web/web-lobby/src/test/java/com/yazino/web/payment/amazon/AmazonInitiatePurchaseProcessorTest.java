package com.yazino.web.payment.amazon;

import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.PlayerService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.payment.Purchase;
import com.yazino.web.payment.PurchaseStatus;
import com.yazino.web.payment.TransactionIdGenerator;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import org.hamcrest.core.Is;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Currency;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AmazonInitiatePurchaseProcessorTest {

    public static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    public static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(638756l);
    public static final String PRODUCT_ID = "productId";
    public static final long PROMOTION_ID = 123l;
    public static final String GAME_TYPE = "gameType";
    private static final long TRANSACTION_ID = 23344L;
    public static final BigDecimal PACKAGE_PRICE = BigDecimal.valueOf(10);
    public static final BigDecimal CHIP_AWARD = BigDecimal.valueOf(2500);

    private AmazonInitiatePurchaseProcessor underTest;

    @Mock
    WalletService walletService;
    @Mock
    PlayerService playerService;
    @Mock
    TransactionIdGenerator transactionIdGenerator;
    @Mock
    ChipBundleResolver chipBundleResolver;
    @Mock
    ChipBundle chipBundle;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(8747363l).getMillis());
        underTest = new AmazonInitiatePurchaseProcessor(playerService, walletService, transactionIdGenerator, chipBundleResolver);
    }

    @Test
    public void testGetPlatformReturnsAmazon() throws Exception {
        assertThat(Platform.AMAZON, Is.is(equalTo(underTest.getPlatform())));
    }

    @Test
    public void testInitiatePurchaseShouldLogExternalTransaction() throws Exception {
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        when(transactionIdGenerator.generateNumericTransactionId()).thenReturn(TRANSACTION_ID);
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID)).thenReturn(chipBundle);
        when(chipBundle.getCurrency()).thenReturn(Currency.getInstance("USD"));
        when(chipBundle.getPrice()).thenReturn(PACKAGE_PRICE);
        when(chipBundle.getChips()).thenReturn(CHIP_AWARD);

        underTest.initiatePurchase(PLAYER_ID, PRODUCT_ID, PROMOTION_ID, GAME_TYPE, Platform.AMAZON);


        ExternalTransaction txn = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(String.valueOf(TRANSACTION_ID))
                .withExternalTransactionId(String.valueOf(TRANSACTION_ID))
                .withMessage(format("productId: %s", PRODUCT_ID), new DateTime())
                .withAmount(Currency.getInstance("USD"),
                            PACKAGE_PRICE)
                .withPaymentOption(CHIP_AWARD, new DateTime().toString())
                .withCreditCardNumber("none")
                .withCashierName("Amazon")
                .withStatus(ExternalTransactionStatus.REQUEST)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withPromotionId(PROMOTION_ID)
                .withPlatform(Platform.AMAZON)
                .build();

        verify(walletService).record(txn);
    }

    @Test
    public void initiatePurchaseShouldReturnAPurchaseWithCreatedOnSuccessAndInternalTransactionId(){
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        when(transactionIdGenerator.generateNumericTransactionId()).thenReturn(TRANSACTION_ID);
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID)).thenReturn(chipBundle);
        when(chipBundle.getCurrency()).thenReturn(Currency.getInstance("USD"));
        when(chipBundle.getPrice()).thenReturn(PACKAGE_PRICE);
        when(chipBundle.getChips()).thenReturn(CHIP_AWARD);

        final Purchase purchase = (Purchase) underTest.initiatePurchase(PLAYER_ID, PRODUCT_ID, PROMOTION_ID, GAME_TYPE, Platform.AMAZON);

        Purchase expected = new Purchase();
        expected.setPurchaseId(String.valueOf(TRANSACTION_ID));
        expected.setStatus(PurchaseStatus.CREATED);

        assertThat(expected, equalTo(purchase));
    }

    @Test
    public void initiatePurchaseShouldReturnPurchaseWithStatusFailed() throws WalletServiceException {
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        when(transactionIdGenerator.generateNumericTransactionId()).thenReturn(TRANSACTION_ID);
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID)).thenReturn(chipBundle);
        when(chipBundle.getCurrency()).thenReturn(Currency.getInstance("USD"));
        when(chipBundle.getPrice()).thenReturn(PACKAGE_PRICE);
        when(chipBundle.getChips()).thenReturn(CHIP_AWARD);
        when(walletService.record(any(ExternalTransaction.class))).thenThrow(new WalletServiceException("Wallet Exception"));

        final Purchase purchase = (Purchase) underTest.initiatePurchase(PLAYER_ID, PRODUCT_ID, PROMOTION_ID, GAME_TYPE, Platform.AMAZON);

        Purchase expected = new Purchase();
        expected.setPurchaseId(String.valueOf(TRANSACTION_ID));
        expected.setStatus(PurchaseStatus.FAILED);

        assertThat(expected, equalTo(purchase));
    }

    @Test
    public void initiatePurchaseShouldReturnAFailedTransactionIfProductIdIsNotFound(){
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        when(transactionIdGenerator.generateNumericTransactionId()).thenReturn(TRANSACTION_ID);
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID)).thenReturn(null);

        final Purchase purchase = (Purchase) underTest.initiatePurchase(PLAYER_ID, PRODUCT_ID, PROMOTION_ID, GAME_TYPE, Platform.AMAZON);

        Purchase expected = new Purchase();
        expected.setStatus(PurchaseStatus.FAILED);
        expected.setErrorMessage(format("Unknown product id: %s", PRODUCT_ID));

        assertThat(expected, equalTo(purchase));
    }
    
}
