package com.yazino.platform.service.account;

import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.AccountStatement;
import com.yazino.platform.model.account.ExternalTransactionPersistenceRequest;
import com.yazino.platform.persistence.account.JDBCAccountStatementDAO;
import com.yazino.platform.repository.account.AccountRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.service.account.transactional.TransactionalWalletService;
import com.yazino.platform.service.community.CashierInformation;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.RemotingService;
import org.openspaces.remoting.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.util.List;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingWalletService implements WalletService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingWalletService.class);

    private final AccountRepository localAccountRepository;
    private final GigaSpace gigaSpace;
    private final TransactionalWalletService transactionalWalletService;
    private final JDBCAccountStatementDAO accountStatementDAO;
    private final CashierInformation cashierInformation;
    private final PlayerRepository playerRepository;

    @Autowired(required = true)
    public GigaspaceRemotingWalletService(
            final AccountRepository localAccountRepository,
            @Qualifier("gigaSpace") final GigaSpace gigaSpace,
            final TransactionalWalletService transactionalWalletService,
            final JDBCAccountStatementDAO accountStatementDAO,
            final InternalWalletService internalWalletService,
            final CashierInformation cashierInformation,
            final PlayerRepository playerRepository) {
        notNull(localAccountRepository, "localAccountRepository may not be null");
        notNull(gigaSpace, "accountGigaSpace may not be null");
        notNull(transactionalWalletService, "transactionalWalletService may not be null");
        notNull(accountStatementDAO, "accountStatementDao may not be null");
        notNull(internalWalletService, "internalWalletService may not be null");
        notNull(cashierInformation, "cashierInformation may not be null");
        notNull(playerRepository, "playerRepository may not be null");

        this.localAccountRepository = localAccountRepository;
        this.gigaSpace = gigaSpace;
        this.transactionalWalletService = transactionalWalletService;
        this.accountStatementDAO = accountStatementDAO;
        this.cashierInformation = cashierInformation;
        this.playerRepository = playerRepository;
    }

    @Override
    public BigDecimal postTransaction(@Routing final BigDecimal accountId,
                                      final BigDecimal amount,
                                      final String transactionType,
                                      final String reference,
                                      final TransactionContext transactionContext)
            throws WalletServiceException {
        return transactionalWalletService.postTransaction(accountId, amount, transactionType, reference, transactionContext);
    }

    @Override
    public BigDecimal getBalance(@Routing final BigDecimal accountId)
            throws WalletServiceException {
        notNull(accountId, "accountId may not be null");

        final Account account = localAccountRepository.findById(accountId);
        if (account == null) {
            throw new WalletServiceException("Account does not exist: " + accountId);
        }

        return account.availableBalance();
    }

    @Override
    public BigDecimal record(@Routing("getAccountId") final ExternalTransaction externalTransaction)
            throws WalletServiceException {
        notNull(externalTransaction, "externalTransaction may not be null");

        BigDecimal updatedBalance = null;
        if (externalTransaction.getStatus().isPostRequired()) {
            LOG.debug("Posting transaction: {}", externalTransaction);

            updatedBalance = postTransaction(externalTransaction.getAccountId(),
                    externalTransaction.getAmountChips(),
                    externalTransaction.getTransactionLogType(),
                    externalTransaction.getInternalTransactionId(),
                    transactionContext()
                            .withSessionId(externalTransaction.getSessionId())
                            .build());

            if (cashierInformation.isPurchase(externalTransaction.getCashierName())) {
                LOG.debug("Player {} will be tagged as buyer after transaction with cashier {}",
                        externalTransaction.getPlayerId(), externalTransaction.getCashierName());
                playerRepository.addTag(externalTransaction.getPlayerId(), "buyer");
            }
        }

        LOG.debug("Logging transaction: {}", externalTransaction);
        gigaSpace.write(new ExternalTransactionPersistenceRequest(externalTransaction));

        return updatedBalance;
    }

    @Override
    public BigDecimal getValueOfTodaysEarnedChips(final BigDecimal accountId, final String cashier) {
        final List<AccountStatement> accountStatements = accountStatementDAO.findBy(accountId, cashier);
        BigDecimal total = BigDecimal.ZERO;

        for (AccountStatement accountStatement : accountStatements) {
            total = total.add(accountStatement.getChipsAmount());
        }
        return total;
    }

}
