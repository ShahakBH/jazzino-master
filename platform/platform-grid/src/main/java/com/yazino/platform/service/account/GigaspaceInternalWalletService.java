package com.yazino.platform.service.account;

import com.gigaspaces.async.AsyncResult;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.grid.Executor;
import com.yazino.platform.grid.ExecutorException;
import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.CloseAccountRequest;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.account.AccountRepository;
import com.yazino.platform.service.account.transactional.TransactionalWalletService;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.Task;
import org.openspaces.remoting.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.persistence.account.AccountLoadType.NULL_IF_ABSENT;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class GigaspaceInternalWalletService implements InternalWalletService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceInternalWalletService.class);

    private final Executor executor;
    private final AccountRepository accountGlobalRepository;
    private final SequenceGenerator sequenceGenerator;
    private final GigaSpace globalGigaSpace;

    @Autowired
    public GigaspaceInternalWalletService(@Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                          final AccountRepository accountGlobalRepository,
                                          final SequenceGenerator sequenceGenerator,
                                          final Executor executor) {
        notNull(globalGigaSpace, "globalGigaSpace may not be null");
        notNull(accountGlobalRepository, "accountGlobalRepository may not be null");
        notNull(sequenceGenerator, "sequenceGenerator may not be null");
        notNull(executor, "executor may not be null");

        this.globalGigaSpace = globalGigaSpace;
        this.accountGlobalRepository = accountGlobalRepository;
        this.sequenceGenerator = sequenceGenerator;
        this.executor = executor;
    }

    @Override
    public BigDecimal createAccount(final String accountName)
            throws WalletServiceException {
        notNull(accountName, "accountName may not be null");

        final Account account = new Account(sequenceGenerator.next(), accountName, BigDecimal.ZERO, BigDecimal.ZERO);
        return executor.remoteExecute(new CreateAccountTask(account), account.getAccountId());
    }

    @Override
    public void closeAccount(@Routing final BigDecimal accountId) {
        notNull(accountId, "accountId is required");

        globalGigaSpace.write(new CloseAccountRequest(accountId));
    }

    @Override
    public Map<BigDecimal, BigDecimal> getBalances(final Collection<BigDecimal> accountIds)
            throws WalletServiceException {
        if (accountIds == null || accountIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return executor.mapReduce(new AccountBalanceTask(accountIds));
    }

    @Override
    public BigDecimal getBalance(final BigDecimal accountId) throws WalletServiceException {
        notNull(accountId, "accountId may not be null");

        final Account account = accountGlobalRepository.findById(accountId);
        if (account == null) {
            throw new WalletServiceException("Account does not exist: " + accountId);
        }

        return account.availableBalance();
    }

    @Override
    public BigDecimal postTransaction(final BigDecimal accountId,
                                      final BigDecimal amount,
                                      final String transactionType,
                                      final String transactionReference,
                                      final TransactionContext transactionContext)
            throws WalletServiceException {
        notNull(accountId, "accountId may not be null");
        notNull(amount, "amount may not be null");
        notNull(transactionType, "transactionType may not be null");
        notNull(transactionContext, "transactionContext may not be null");

        try {
            return executor.remoteExecute(
                    new PostTransactionTask(accountId, amount, transactionType, transactionReference, transactionContext),
                    accountId);

        } catch (ExecutorException e) {
            if (e.getCause() != null && e.getCause() instanceof WalletServiceException) {
                throw (WalletServiceException) e.getCause();
            }
            LOG.error("Unexpected exception from remote execution", e);
            throw e;
        }
    }

    @AutowireTask
    public static class CreateAccountTask implements Task<BigDecimal> {
        private static final long serialVersionUID = -6373102844395247831L;

        @Resource
        private transient AccountRepository accountRepository;

        private final Account account;

        public CreateAccountTask(final Account account) {
            this.account = account;
        }

        @Override
        public BigDecimal execute() throws Exception {
            accountRepository.save(account);
            return account.getAccountId();
        }
    }

    @AutowireTask
    public static class PostTransactionTask implements Task<BigDecimal> {
        private static final long serialVersionUID = -6373102844395247831L;

        @Resource(name = "transactionalWalletService")
        private transient TransactionalWalletService transactionalWalletService;

        private final BigDecimal accountId;
        private final BigDecimal amount;
        private final String transactionType;
        private final String reference;
        private final TransactionContext transactionContext;

        public PostTransactionTask(final BigDecimal accountId,
                                   final BigDecimal amount,
                                   final String transactionType,
                                   final String reference,
                                   final TransactionContext transactionContext) {
            this.accountId = accountId;
            this.amount = amount;
            this.transactionType = transactionType;
            this.reference = reference;
            this.transactionContext = transactionContext;
        }

        @Override
        public BigDecimal execute() throws Exception {
            try {
                return transactionalWalletService.postTransaction(
                        accountId, amount, transactionType, reference, transactionContext);

            } catch (ConcurrentModificationException e) {
                throw new WalletServiceException(e.getMessage(), e);
            }
        }
    }

    @AutowireTask
    public static class AccountBalanceTask implements DistributedTask<HashMap<BigDecimal, BigDecimal>, HashMap<BigDecimal, BigDecimal>> {
        private static final long serialVersionUID = -1482878486569787066L;

        @Resource
        private transient AccountRepository accountRepository;

        private final Collection<BigDecimal> accountIds;

        public AccountBalanceTask(final Collection<BigDecimal> accountIds) {
            this.accountIds = accountIds;
        }

        @Override
        public HashMap<BigDecimal, BigDecimal> execute() throws Exception {
            if (accountIds == null || accountIds.size() == 0) {
                return new HashMap<BigDecimal, BigDecimal>();
            }

            LOG.debug("Getting balances for accounts: {}", accountIds);

            final HashMap<BigDecimal, BigDecimal> results = new HashMap<BigDecimal, BigDecimal>();
            for (final BigDecimal accountId : accountIds) {
                final Account account = accountRepository.findById(accountId, NULL_IF_ABSENT);
                if (account != null) {
                    results.put(accountId, calculateBalance(account));
                }
            }
            return results;
        }

        private BigDecimal calculateBalance(final Account account) {
            BigDecimal balance = account.getBalance();
            if (balance == null) {
                balance = BigDecimal.ZERO;
            }

            if (account.getCreditLimit() != null) {
                balance = balance.add(account.getCreditLimit());
            }

            return balance;
        }

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        @Override
        public HashMap<BigDecimal, BigDecimal> reduce(final List<AsyncResult<HashMap<BigDecimal, BigDecimal>>> results) throws Exception {
            final HashMap<BigDecimal, BigDecimal> mergedResults = new HashMap<BigDecimal, BigDecimal>();

            if (results != null) {
                for (final AsyncResult<HashMap<BigDecimal, BigDecimal>> result : results) {
                    if (result.getException() != null) {
                        LOG.error("Remote invocation failed", result.getException());
                        continue;
                    }

                    final Map<BigDecimal, BigDecimal> partitionResult = result.getResult();
                    if (partitionResult != null) {
                        mergedResults.putAll(partitionResult);
                    }
                }
            }

            return mergedResults;
        }
    }

}
