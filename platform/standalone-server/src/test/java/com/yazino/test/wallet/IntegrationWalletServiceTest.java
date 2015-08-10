package com.yazino.test.wallet;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IntegrationWalletServiceTest {
    private IntegrationWalletService underTest;

    @Before
    public void setUp() {
        underTest = new IntegrationWalletService(true);
    }

    @Test
    public void getTotalAmountsShouldReturnEmptyMapWhenNoTransactionPosted() {
        Map<String, BigDecimal> result = underTest.getTotalAmounts();

        assertTrue(result.isEmpty());
    }

    @Test
    public void getTotalAmountsShouldReturnAMapWithOneKeyWhenOneTransactionPosted() throws WalletServiceException {
        underTest.postTransaction(BigDecimal.ONE, BigDecimal.TEN, "Return", "BonusPayout", TransactionContext.EMPTY);

        final Map<String, BigDecimal> result = underTest.getTotalAmounts();

        final Map<String, BigDecimal> expected = new HashMap<String, BigDecimal>();
        expected.put("Return:BonusPayout", BigDecimal.TEN);
        assertEquals(expected, result);
    }

    @Test
    public void getTotalAmountsShouldReturnAMapWithOneKeyAndSumOfAmountsWhenTwoTransactionsPosted() throws WalletServiceException {
        underTest.postTransaction(BigDecimal.ONE, BigDecimal.TEN, "Return", "BonusPayout", TransactionContext.EMPTY);
        underTest.postTransaction(BigDecimal.ONE, BigDecimal.valueOf(12), "Return", "BonusPayout", TransactionContext.EMPTY);

        final Map<String, BigDecimal> result = underTest.getTotalAmounts();

        final Map<String, BigDecimal> expected = new HashMap<String, BigDecimal>();
        expected.put("Return:BonusPayout", BigDecimal.valueOf(22));
        assertEquals(expected, result);
    }

    @Test
    public void getTotalAmountsShouldReturnTotalAmountForTransactionsWithNullReference() throws WalletServiceException {
        underTest.postTransaction(BigDecimal.ONE, BigDecimal.TEN, "Stake", null, TransactionContext.EMPTY);

        final Map<String, BigDecimal> result = underTest.getTotalAmounts();

        final Map<String, BigDecimal> expected = new HashMap<String, BigDecimal>();
        expected.put("Stake:null", BigDecimal.TEN);
        assertEquals(expected, result);
    }
}
