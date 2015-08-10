package com.yazino.test.wallet;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.test.InMemoryWalletService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Extension of InMemoryWalletService which provides tracking of the total amount that was paid into / taken out
 * across the accounts.
 */
public class IntegrationWalletService extends InMemoryWalletService {

    private HashMap<BigDecimal, BigDecimal> accountStakePaid;
    private HashMap<BigDecimal, BigDecimal> accountPayoutReceived;

    private BigDecimal totalPayout;
    private BigDecimal totalStake;
    private long stakeCount;
    private long payoutCount;
    private HashMap<String, BigDecimal> totalAmounts;


    public IntegrationWalletService(final boolean failOnInsufficientBalance) {
        super(failOnInsufficientBalance);

        accountStakePaid = new HashMap<BigDecimal, BigDecimal>();
        accountPayoutReceived = new HashMap<BigDecimal, BigDecimal>();
        totalAmounts = new HashMap<String, BigDecimal>();
        stakeCount = 0;
        payoutCount = 0;
    }

    public BigDecimal totalPayout() {
        if (totalPayout == null) {
            totalPayout = BigDecimal.ZERO;
        }
        return totalPayout;
    }

    private void setTotalPayout(final BigDecimal payout) {
        totalPayout = payout;
    }

    public BigDecimal totalStake() {
        if (totalStake == null) {
            totalStake = BigDecimal.ZERO;
        }
        return totalStake;
    }

    private void setTotalStake(final BigDecimal stake) {
        totalStake = stake;
    }


    @Override
    public BigDecimal postTransaction(final BigDecimal accountId,
                                      final BigDecimal amountOfChips,
                                      final String transactionType,
                                      final String reference,
                                      final TransactionContext transactionContext) throws WalletServiceException {
        notNull(accountId, "accountId may not be null");
        notNull(amountOfChips, "amountOfChips may not be null");
        notNull(transactionType, "transactionType may not be null");
        notNull(transactionContext, "transactionContext may not be null");

        if (amountOfChips.compareTo(BigDecimal.ZERO) > 0) {
            incrementPayoutReceived(accountId, amountOfChips);
        } else if (amountOfChips.compareTo(BigDecimal.ZERO) < 0) {
            incrementStakePaid(accountId, amountOfChips);
        }
        final BigDecimal result = super.postTransaction(accountId,
                amountOfChips,
                transactionType,
                reference,
                transactionContext);
        final String key = transactionType + ":" + reference;
        BigDecimal totalAmount = totalAmounts.get(key);
        if (totalAmount == null) {
            totalAmount = amountOfChips;
        } else {
            totalAmount = totalAmount.add(amountOfChips);
        }
        totalAmounts.put(key, totalAmount);
        return result;
    }

    private void incrementStakePaid(final BigDecimal accountId, final BigDecimal amountOfChips) {
        BigDecimal currentAmount = accountStakePaid.get(accountId);
        if (currentAmount == null) {
            currentAmount = BigDecimal.ZERO;
        }
        currentAmount = currentAmount.add(amountOfChips);
        accountStakePaid.put(accountId, currentAmount);

        setTotalStake(totalStake().subtract(amountOfChips));
        stakeCount++;
    }

    private void incrementPayoutReceived(final BigDecimal accountId, final BigDecimal amountOfChips) {
        BigDecimal currentAmount = accountPayoutReceived.get(accountId);
        if (currentAmount == null) {
            currentAmount = BigDecimal.ZERO;
        }
        currentAmount = currentAmount.add(amountOfChips);
        accountPayoutReceived.put(accountId, currentAmount);

        setTotalPayout(totalPayout().add(amountOfChips));
        payoutCount++;
    }

    public long totalStakeCount() {
        return stakeCount;
    }

    public long totalPayoutCount() {
        return payoutCount;
    }


    public Map<String, BigDecimal> getTotalAmounts() {
        return totalAmounts;
    }
}
