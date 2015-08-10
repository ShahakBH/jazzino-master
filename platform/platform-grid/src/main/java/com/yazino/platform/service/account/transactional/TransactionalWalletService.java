package com.yazino.platform.service.account.transactional;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.error.account.InsufficientFundsException;
import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.AccountTransaction;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.account.AccountRepository;
import org.openspaces.remoting.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Delegate class to handle inability to make GS service as @Transactional.
 */
@Service
public class TransactionalWalletService {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionalWalletService.class);

    private final AccountRepository accountRepository;
    private final SequenceGenerator sequenceGenerator;

    @SuppressWarnings("UnusedDeclaration")
    TransactionalWalletService() {
        // CGLib constructor

        this.accountRepository = null;
        this.sequenceGenerator = null;
    }

    @Autowired(required = true)
    public TransactionalWalletService(final AccountRepository accountRepository,
                                      final SequenceGenerator sequenceGenerator) {
        notNull(accountRepository, "accountRepository may not be null");
        notNull(sequenceGenerator, "sequenceGenerator may not be null");

        this.accountRepository = accountRepository;
        this.sequenceGenerator = sequenceGenerator;
    }

    private void verifyInitialisation() {
        if (accountRepository == null
                || sequenceGenerator == null) {
            throw new IllegalStateException(
                    "Class was created with the CGLib constructor and is invalid for direct use");
        }
    }

    @Transactional("spaceTransactionManager")
    public BigDecimal postTransaction(@Routing final BigDecimal accountId,
                                      final BigDecimal amount,
                                      final String transactionType,
                                      final String reference,
                                      final TransactionContext transactionContext)
            throws WalletServiceException {
        verifyInitialisation();

        notNull(accountId, "accountId may not be null");
        notNull(amount, "amount may not be null");
        notNull(transactionType, "transactionType may not be null");
        notNull(transactionContext, "transactionContext may not be null");

        Account account = accountRepository.findById(accountId);
        if (account == null) {
            LOG.error("Post attempted to non-existent account {}: amount={}; txType={}; reference={}; context={}",
                    accountId, amount, transactionType, reference, transactionContext);
            throw new IllegalArgumentException("Account " + accountId + " does not exist");
        }

        account = accountRepository.lock(accountId);

        try {
            final AccountTransaction transaction = new AccountTransaction(
                    account.getAccountId(), amount, transactionType, reference, transactionContext);
            account.postTransaction(transaction);

        } catch (InsufficientFundsException iex) {
            throw new WalletServiceException(iex.getParameterisedMessage(), false);
        }

        accountRepository.save(account);

        return account.getBalance();
    }

}
