package com.yazino.web.payment.creditcard.paypalwpp;


import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import org.openspaces.remoting.Routing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class StubWalletService implements WalletService {

    private final List<ExternalTransaction> loggedExternalTransactions = new ArrayList<ExternalTransaction>();
    private final List<ExternalTransaction> postedExternalTransactions = new ArrayList<ExternalTransaction>();

    @Override
    public BigDecimal record(@Routing("getAccountId") final ExternalTransaction externalTransaction) {
        if (externalTransaction != null) {
            loggedExternalTransactions.add(externalTransaction);
            if (externalTransaction.getStatus().isPostRequired()) {
                postedExternalTransactions.add(externalTransaction);
            }
        }
        return null;
    }

    @Override
    public BigDecimal getValueOfTodaysEarnedChips(@Routing BigDecimal accountId, String cashier) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    public List<ExternalTransaction> getLoggedExternalTransactions() {
        return loggedExternalTransactions;
    }

    public List<ExternalTransaction> getPostedExternalTransactions() {
        return postedExternalTransactions;
    }

    @Override
    public BigDecimal getBalance(@Routing final BigDecimal accountId) throws WalletServiceException {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public BigDecimal postTransaction(@Routing final BigDecimal accountId,
                                      final BigDecimal amountOfChips,
                                      final String transactionType,
                                      final String reference,
                                      final TransactionContext transactionContext) throws WalletServiceException {
        throw new UnsupportedOperationException("Unimplemented");

    }
}
