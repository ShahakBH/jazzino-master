package com.yazino.platform.payment;

import com.google.common.base.Optional;
import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.account.PaymentSettlement;
import com.yazino.platform.payment.dispute.PaymentDisputeProcessor;
import com.yazino.platform.payment.settlement.PaymentSettlementProcessor;
import com.yazino.platform.persistence.account.JDBCPaymentSettlementDAO;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileStatus;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Currency;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.account.TransactionContext.transactionContext;
import static com.yazino.platform.model.account.PaymentSettlement.newSettlement;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DatabasePaymentServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(-1000);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(-100);
    private static final BigDecimal PRICE = BigDecimal.valueOf(200);
    private static final BigDecimal CHIPS = BigDecimal.valueOf(2100);
    private static final Long PROMOTION_ID = 3141592L;
    private static final DateTime BASE_DATE = new DateTime(2013, 2, 5, 10, 15);

    @Mock
    private JDBCPaymentSettlementDAO paymentSettlementDao;
    @Mock
    private JDBCPaymentDisputeDAO paymentDisputeDao;
    @Mock
    private PlayerProfileDao playerProfileDao;
    @Mock
    private WalletService walletService;
    @Mock
    private PaymentSettlementProcessor settlementProcessor;
    @Mock
    private PaymentDisputeProcessor disputeProcessor;

    private DatabasePaymentService underTest;

    @BeforeClass
    public static void stopTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(BASE_DATE.getMillis());
    }

    @AfterClass
    public static void startTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Before
    public void setUp() throws Exception {
        underTest = new DatabasePaymentService(paymentSettlementDao, paymentDisputeDao, playerProfileDao, walletService);
        underTest.setPaymentSettlementProcessors(Collections.singletonMap("worldpay", settlementProcessor));
        underTest.setPaymentDisputeProcessors(Collections.singletonMap("testcashier", disputeProcessor));

        when(settlementProcessor.cancel(aPaymentSettlement(1))).thenReturn(aCancelledExternalTransaction(1));
        when(settlementProcessor.cancel(aPaymentSettlement(2))).thenReturn(aCancelledExternalTransaction(2));
        when(settlementProcessor.cancel(aPaymentSettlement(3))).thenReturn(anErrorExternalTransaction(3));

        when(paymentDisputeDao.findByInternalTransactionId("internal-tx-1")).thenReturn(Optional.of(anOpenDispute(1)));

        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile());
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullPaymentSettlementDAO() {
        new DatabasePaymentService(null, paymentDisputeDao, playerProfileDao, walletService);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullPaymentDisputeDAO() {
        new DatabasePaymentService(paymentSettlementDao, null, playerProfileDao, walletService);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullPlayerProfileDAO() {
        new DatabasePaymentService(paymentSettlementDao, paymentDisputeDao, null, walletService);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullWalletServiceDAO() {
        new DatabasePaymentService(paymentSettlementDao, paymentDisputeDao, playerProfileDao, null);
    }

    @Test
    public void findingAuthorisedPaymentsDelegatesToTheDAO() {
        final PagedData<PendingSettlement> expectedResult = new PagedData<>(
                10, 20, 100, asList(aPendingSettlement(1), aPendingSettlement(2)));
        when(paymentSettlementDao.findSummarisedPendingSettlements(10, 20)).thenReturn(expectedResult);

        final PagedData<PendingSettlement> authorised = underTest.findAuthorised(10, 20);

        assertThat(authorised, is(equalTo(expectedResult)));
    }

    @Test
    public void cancellingAllSettlementsForAPlayerBlocksAPlayerEvenIfThereAreNoValidSettlements() {
        when(paymentSettlementDao.findByPlayerId(PLAYER_ID)).thenReturn(Collections.<PaymentSettlement>emptySet());

        underTest.cancelAllSettlementsForPlayer(PLAYER_ID);

        verify(playerProfileDao).updateStatus(PLAYER_ID, PlayerProfileStatus.BLOCKED, "system", "Payment cancelled before settlement");
    }

    @Test
    public void cancellingAllSettlementsForAPlayerBlocksAPlayer() {
        when(paymentSettlementDao.findByPlayerId(PLAYER_ID)).thenReturn(newHashSet(aPaymentSettlement(1), aPaymentSettlement(2)));

        underTest.cancelAllSettlementsForPlayer(PLAYER_ID);

        verify(playerProfileDao).updateStatus(PLAYER_ID, PlayerProfileStatus.BLOCKED, "system", "Payment cancelled before settlement");
    }

    @Test
    public void cancellingAllSettlementsSkipsPaymentsWithNoRegisteredProcessor() {
        when(paymentSettlementDao.findByPlayerId(PLAYER_ID)).thenReturn(newHashSet(aPaymentSettlementWithCashier(1, "missingCashier"), aPaymentSettlement(2)));

        underTest.cancelAllSettlementsForPlayer(PLAYER_ID);

        verify(settlementProcessor, times(0)).cancel(aPaymentSettlement(1));
        verify(paymentSettlementDao, times(0)).deleteById("internal-tx-1");
        verify(settlementProcessor).cancel(aPaymentSettlement(2));
    }

    @Test
    public void cancellingAllSettlementsForAPlayerCancelsEachSettlement() {
        when(paymentSettlementDao.findByPlayerId(PLAYER_ID)).thenReturn(newHashSet(aPaymentSettlement(1), aPaymentSettlement(2)));

        underTest.cancelAllSettlementsForPlayer(PLAYER_ID);

        verify(settlementProcessor).cancel(aPaymentSettlement(1));
        verify(settlementProcessor).cancel(aPaymentSettlement(2));
    }

    @Test
    public void cancellingAllSettlementsForAPlayerRecordsAnExternalTransactionForEachPayment() throws WalletServiceException {
        when(paymentSettlementDao.findByPlayerId(PLAYER_ID)).thenReturn(newHashSet(aPaymentSettlement(1), aPaymentSettlement(2), aPaymentSettlement(3)));

        underTest.cancelAllSettlementsForPlayer(PLAYER_ID);

        verify(walletService).record(aCancelledExternalTransaction(1));
        verify(walletService).record(aCancelledExternalTransaction(2));
    }

    @Test
    public void cancellingAllSettlementsForAPlayerDoesNotBreakIfOneCancellationCausesAnException() throws WalletServiceException {
        when(paymentSettlementDao.findByPlayerId(PLAYER_ID)).thenReturn(newHashSet(aPaymentSettlement(1), aPaymentSettlement(2), aPaymentSettlement(3)));
        when(settlementProcessor.cancel(aPaymentSettlement(1))).thenThrow(new RuntimeException("aTestException"));

        underTest.cancelAllSettlementsForPlayer(PLAYER_ID);

        verify(walletService, times(0)).record(aCancelledExternalTransaction(1));
        verify(walletService).record(aCancelledExternalTransaction(2));
    }

    @Test
    public void cancellingAllSettlementsForAPlayerDeletesOnlySuccessfulCancellations() throws WalletServiceException {
        when(paymentSettlementDao.findByPlayerId(PLAYER_ID)).thenReturn(newHashSet(aPaymentSettlement(1), aPaymentSettlement(2), aPaymentSettlement(3)));

        underTest.cancelAllSettlementsForPlayer(PLAYER_ID);

        verify(paymentSettlementDao).deleteById("internal-tx-1");
        verify(paymentSettlementDao).deleteById("internal-tx-2");
        verify(paymentSettlementDao, times(0)).deleteById("internal-tx-3");
    }

    @Test
    public void cancellingAllSettlementsForAPlayerReturnsTheNumberOfTransactionSuccessfullyCancelled() throws WalletServiceException {
        when(paymentSettlementDao.findByPlayerId(PLAYER_ID)).thenReturn(newHashSet(aPaymentSettlement(1), aPaymentSettlement(2), aPaymentSettlement(3)));

        final int numberCancelled = underTest.cancelAllSettlementsForPlayer(PLAYER_ID);

        assertThat(numberCancelled, is(equalTo(2)));
    }

    @Test
    public void disputePaymentSkipsPaymentsWithNoRegisteredProcessor() {
        underTest.setPaymentDisputeProcessors(Collections.<String, PaymentDisputeProcessor>emptyMap());

        underTest.disputePayment(anOpenDispute(1));

        verify(disputeProcessor, times(0)).raise(any(PaymentDispute.class));
        verify(paymentDisputeDao, times(0)).save(any(PaymentDispute.class));
    }

    @Test
    public void disputePaymentRaisesADisputeWithTheAppropriateProcessor() {
        underTest.disputePayment(anOpenDispute(1));

        verify(disputeProcessor).raise(anOpenDispute(1));
    }

    @Test
    public void disputePaymentSavesTheDisputeIfTheRaiseWasSuccessful() {
        underTest.disputePayment(anOpenDispute(1));

        verify(paymentDisputeDao).save(anOpenDispute(1));
    }

    @Test
    public void disputePaymentDoesNotSaveTheDisputeIfTheRaiseWasUnsuccessful() {
        doThrow(new IllegalStateException("aTestException")).when(disputeProcessor).raise(any(PaymentDispute.class));

        underTest.disputePayment(anOpenDispute(1));

        verify(paymentDisputeDao, times(0)).save(any(PaymentDispute.class));
    }

    @Test
    public void resolveDisputeSkipsPaymentsWithNoRegisteredProcessor() {
        underTest.setPaymentDisputeProcessors(Collections.<String, PaymentDisputeProcessor>emptyMap());

        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUNDED_FRAUD, "aUser", "aNote");

        verify(disputeProcessor, times(0)).resolve(any(PaymentDispute.class));
        verify(paymentDisputeDao, times(0)).save(any(PaymentDispute.class));
        verifyZeroInteractions(walletService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveDisputeThrowsAnIllegalArgumentExceptionIfTheTransactionDoesNotExist() {
        reset(paymentDisputeDao);
        when(paymentDisputeDao.findByInternalTransactionId("internal-tx-1")).thenReturn(Optional.<PaymentDispute>absent());

        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUNDED_FRAUD, "aUser", "aNote");
    }

    @Test
    public void resolveDisputeResolvesTheDisputeWithTheAppropriateProcessor() {
        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUNDED_FRAUD, "aUser", "aNote");

        verify(disputeProcessor).resolve(aClosedDispute(1, DisputeResolution.REFUNDED_FRAUD));
    }

    @Test
    public void resolveDisputeSavesTheDisputeIfTheResolveWasSuccessful() {
        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUNDED_FRAUD, "aUser", "aNote");

        verify(paymentDisputeDao).save(aClosedDispute(1, DisputeResolution.REFUNDED_FRAUD));
    }

    @Test
    public void resolveDisputeDoesNotSaveTheDisputeIfTheResolveWasUnsuccessful() {
        doThrow(new IllegalStateException("aTestException")).when(disputeProcessor).resolve(any(PaymentDispute.class));

        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUNDED_FRAUD, "aUser", "aNote");

        verify(paymentDisputeDao, times(0)).save(anOpenDispute(1));
    }

    @Test
    public void resolveDisputeDoesNotCallTheWalletServiceIfTheResolveWasUnsuccessful() {
        doThrow(new IllegalStateException("aTestException")).when(disputeProcessor).resolve(any(PaymentDispute.class));

        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUNDED_FRAUD, "aUser", "aNote");

        verifyZeroInteractions(walletService);
    }

    @Test
    public void resolveDisputeIgnoresInabilityToRemoveChipsForRefundedResolutions() throws WalletServiceException {
        when(walletService.postTransaction(any(BigDecimal.class), any(BigDecimal.class), anyString(), anyString(), any(TransactionContext.class)))
                .thenThrow(new WalletServiceException("aTestException"));

        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUNDED_FRAUD, "aUser", "aNote");

        verify(walletService).postTransaction(ACCOUNT_ID, BigDecimal.ZERO.subtract(CHIPS), "testCashier Deposit",
                "Refund of transaction internal-tx-1", transactionContext().withPlayerId(PLAYER_ID).build());
        verifyNoMoreInteractions(walletService);
    }

    @Test
    public void resolveDisputeRemovesCreditedChipsForThePlayerForRefundedFraudResolution() throws WalletServiceException {
        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUNDED_FRAUD, "aUser", "aNote");

        verify(walletService).postTransaction(ACCOUNT_ID, BigDecimal.ZERO.subtract(CHIPS), "testCashier Deposit",
                "Refund of transaction internal-tx-1", transactionContext().withPlayerId(PLAYER_ID).build());
        verifyNoMoreInteractions(walletService);
    }

    @Test
    public void resolveDisputeRemovesCreditedChipsForThePlayerForRefundedPlayerErrorResolution() throws WalletServiceException {
        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUNDED_PLAYER_ERROR, "aUser", "aNote");

        verify(walletService).postTransaction(ACCOUNT_ID, BigDecimal.ZERO.subtract(CHIPS), "testCashier Deposit",
                "Refund of transaction internal-tx-1", transactionContext().withPlayerId(PLAYER_ID).build());
        verifyNoMoreInteractions(walletService);
    }

    @Test
    public void resolveDisputeRemovesCreditedChipsForThePlayerForRefundedOtherResolution() throws WalletServiceException {
        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUNDED_OTHER, "aUser", "aNote");

        verify(walletService).postTransaction(ACCOUNT_ID, BigDecimal.ZERO.subtract(CHIPS), "testCashier Deposit",
                "Refund of transaction internal-tx-1", transactionContext().withPlayerId(PLAYER_ID).build());
        verifyNoMoreInteractions(walletService);
    }

    @Test
    public void resolveDisputeCreditsChipsForThePlayerForChipsCreditedResolution() throws WalletServiceException {
        underTest.resolveDispute("internal-tx-1", DisputeResolution.CHIPS_CREDITED, "aUser", "aNote");

        verify(walletService).postTransaction(ACCOUNT_ID, CHIPS, "testCashier Deposit",
                "Re-credit of transaction internal-tx-1", transactionContext().withPlayerId(PLAYER_ID).build());
        verifyNoMoreInteractions(walletService);
    }

    @Test
    public void resolveDisputeDoesNotCreditChipsForThePlayerForRefusedResolution() throws WalletServiceException {
        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUSED, "aUser", "aNote");

        verifyZeroInteractions(walletService);
    }

    @Test
    public void resolveDisputeDoesNotCreditChipsForThePlayerForRefusedBannedResolution() throws WalletServiceException {
        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUSED_BANNED, "aUser", "aNote");

        verifyZeroInteractions(walletService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveDisputeThrowsAnIllegalArgumentExceptionIfWeRequiredThePlayerStatusAndThePlayerDoesNotExist() throws WalletServiceException {
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(null);

        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUSED, "aUser", "aNote");
    }

    @Test
    public void resolveDisputeTranslatesRefusedToRefusedBannedIfThePlayerIsBlocked() throws WalletServiceException {
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfileWithStatus(PlayerProfileStatus.BLOCKED));

        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUSED, "aUser", "aNote");

        verify(paymentDisputeDao).save(aClosedDispute(1, DisputeResolution.REFUSED_BANNED));
    }

    @Test
    public void resolveDisputeTranslatesRefusedToRefusedBannedIfThePlayerIsClosed() throws WalletServiceException {
        when(playerProfileDao.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfileWithStatus(PlayerProfileStatus.CLOSED));

        underTest.resolveDispute("internal-tx-1", DisputeResolution.REFUSED, "aUser", "aNote");

        verify(paymentDisputeDao).save(aClosedDispute(1, DisputeResolution.REFUSED_BANNED));
    }

    @Test
    public void queryingOpenDisputesDelegatesToTheDao() {
        final PagedData<DisputeSummary> expectedSummaries = new PagedData<>(3, 1, 2000, asList(summaryOf(anOpenDispute(1))));
        when(paymentDisputeDao.findOpenDisputes(2, 3)).thenReturn(expectedSummaries);

        final PagedData<DisputeSummary> openDisputes = underTest.findOpenDisputes(2, 3);

        assertThat(openDisputes, is(equalTo(expectedSummaries)));
    }

    private PaymentDispute aClosedDispute(final int i,
                                          final DisputeResolution resolution) {
        return PaymentDispute.copy(anOpenDispute(i))
                .withResolution(resolution, BASE_DATE, "aNote", "aUser")
                .build();
    }

    private DisputeSummary summaryOf(final PaymentDispute dispute) {
        return new DisputeSummary(dispute.getInternalTransactionId(),
                dispute.getCashierName(),
                dispute.getExternalTransactionId(),
                dispute.getStatus(),
                dispute.getDisputeTimestamp(),
                dispute.getPlayerId(),
                null,
                null,
                dispute.getCurrency(),
                dispute.getPrice(),
                dispute.getChips(),
                dispute.getDescription());
    }

    private PaymentDispute anOpenDispute(final int i) {
        return PaymentDispute.newDispute("internal-tx-" + i,
                "testCashier",
                "external-tx-" + i,
                PLAYER_ID,
                ACCOUNT_ID,
                BASE_DATE.minusHours(i),
                PRICE,
                Currency.getInstance("GBP"),
                CHIPS,
                ExternalTransactionType.DEPOSIT,
                "aDisputeReason")
                .withGameType("aGameType")
                .withPlatform(Platform.WEB)
                .withPaymentOptionId("aPaymentOptionId")
                .withPromotionId(PROMOTION_ID)
                .build();
    }

    private PendingSettlement aPendingSettlement(final int i) {
        return new PendingSettlement(new DateTime(100000000L),
                "internal-tx-" + i,
                "external-tx-" + i,
                PLAYER_ID,
                "aPlayer",
                "aPlayerCountry",
                "WorldPay",
                Currency.getInstance("GBP"),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(20000),
                Currency.getInstance("USD"),
                BigDecimal.valueOf(5));
    }

    private PaymentSettlement aPaymentSettlement(final int i) {
        return aPaymentSettlementWithCashier(i, "WorldPay");
    }

    private PaymentSettlement aPaymentSettlementWithCashier(final int i,
                                                            final String cashierName) {
        return newSettlement("internal-tx-" + i,
                "external-tx-" + i,
                PLAYER_ID,
                BigDecimal.valueOf(-20000),
                cashierName,
                new DateTime(100000000L),
                "anAccountNumber",
                BigDecimal.valueOf(1000),
                Currency.getInstance("GBP"),
                BigDecimal.valueOf(20000),
                ExternalTransactionType.DEPOSIT)
                .build();
    }

    private ExternalTransaction anErrorExternalTransaction(final int i) {
        return anExternalTransactionBuilderFor(i)
                .withMessage("aMessage", new DateTime())
                .withStatus(ExternalTransactionStatus.ERROR)
                .build();
    }

    private ExternalTransaction aCancelledExternalTransaction(final int i) {
        return anExternalTransactionBuilderFor(i)
                .withMessage("aMessage", new DateTime())
                .withStatus(ExternalTransactionStatus.CANCELLED)
                .build();
    }

    private ExternalTransactionBuilder anExternalTransactionBuilderFor(final int i) {
        return ExternalTransaction.newExternalTransaction(BigDecimal.valueOf(-20000))
                .withAmount(Currency.getInstance("GBP"), BigDecimal.valueOf(1000))
                .withCashierName("WorldPay")
                .withCreditCardNumber("anAccountNumber")
                .withExternalTransactionId("external-tx-" + i)
                .withInternalTransactionId("internal-tx-" + i)
                .withPaymentOption(BigDecimal.valueOf(-10000), null)
                .withPlayerId(PLAYER_ID)
                .withType(ExternalTransactionType.DEPOSIT);
    }

    private PlayerProfile aPlayerProfile() {
        return aPlayerProfileWithStatus(PlayerProfileStatus.ACTIVE);
    }

    private PlayerProfile aPlayerProfileWithStatus(final PlayerProfileStatus status) {
        final PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setPlayerId(PLAYER_ID);
        playerProfile.setStatus(status);
        return playerProfile;
    }

}
