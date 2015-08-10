package com.yazino.platform.processor.table;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.table.PostTransactionAtTable;
import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.model.table.TableTransactionRequest;
import com.yazino.platform.model.table.TransactionResultWrapper;
import com.yazino.platform.service.account.InternalWalletService;
import org.openspaces.core.GigaSpace;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.TransactionResult;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 5, maxConcurrentConsumers = 30)
public class PostTransactionAtTableProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PostTransactionAtTableProcessor.class);
    private static final TableTransactionRequest TEMPLATE = new TableTransactionRequest();

    private final InternalWalletService internalWalletService;
    private final GigaSpace gigaSpace;

    /**
     * CGLib constructor.
     */
    PostTransactionAtTableProcessor() {
        internalWalletService = null;
        gigaSpace = null;
    }

    @Autowired(required = true)
    public PostTransactionAtTableProcessor(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                           final InternalWalletService internalWalletService) {
        notNull(gigaSpace, "gigaSpace is null");
        notNull(internalWalletService, "internalWalletService is null");

        this.gigaSpace = gigaSpace;
        this.internalWalletService = internalWalletService;
    }

    @EventTemplate
    public TableTransactionRequest template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void processTransactionRequest(final TableTransactionRequest request) {
        notNull(internalWalletService, "Class is not initialised or was created via"
                + " CGLib constructor and is invalid for direct use");
        notNull(gigaSpace, "Class is not initialised or was created via"
                + " CGLib constructor and is invalid for direct use");
        notNull(request, "Request Wrapper may not be null");

        LOG.debug("Processing TableTransactionRequest, RequestWrapper: {}", request);

        processAllRequestedTransactions(request);
    }

    private void processAllRequestedTransactions(final TableTransactionRequest request) {
        for (PostTransactionAtTable transaction : request.getTransactions()) {
            processTransaction(request, transaction);
        }
    }

    private void processTransaction(final TableTransactionRequest request,
                                    final PostTransactionAtTable transaction) {
        TransactionResult result = null;
        try {
            LOG.debug("Using wallet service to process postTransactions processTransactionRequest: [{}]", transaction);
            final BigDecimal balance = internalWalletService.postTransaction(transaction.getAccountId(),
                    transaction.getAmount(), transaction.getTransactionType(),
                    transaction.getTransactionReference(), transaction.getTransactionContext());
            result = new TransactionResult(transaction.getUniqueId(), true, null, transaction.getAccountId(), balance, transaction.getPlayerId());
        } catch (Throwable t) {
            logWalletError(transaction, t);
            result = new TransactionResult(transaction.getUniqueId(), false, t.getMessage(),
                    transaction.getAccountId(), null, transaction.getPlayerId());
        } finally {
            writeResultToGigaSpace(request, transaction, result);
        }
    }

    private void logWalletError(final PostTransactionAtTable transaction,
                                final Throwable cause) {
        if (isExpectedWalletError(cause)) {
            LOG.info("Unsuccessful postTransactions processTransactionRequest [{}]: {}", transaction, cause.getMessage());
        } else {
            LOG.error("Error processing postTransactions processTransactionRequest [{}]", transaction, cause);
        }
    }

    private void writeResultToGigaSpace(final TableTransactionRequest request,
                                        final PostTransactionAtTable transaction,
                                        final TransactionResult result) {
        final TransactionResultWrapper wrapper = wrap(request, transaction, result);

        if (LOG.isDebugEnabled()) {
            final String resultString;
            if (result.isSuccessful()) {
                resultString = "";
            } else {
                resultString = "(failure)";
            }
            LOG.debug("Writing postTransactions wrapper {} to space:[{}]", resultString, wrapper);
        }

        gigaSpace.write(new TableRequestWrapper(wrapper));
    }

    private TransactionResultWrapper wrap(final TableTransactionRequest request,
                                          final PostTransactionAtTable transaction,
                                          final TransactionResult result) {
        final TransactionResultWrapper wrapper = new TransactionResultWrapper(
                transaction.getTransactionContext().getTableId(), transaction.getTransactionContext().getGameId(), result, request.getAuditLabel());
        wrapper.setTimestamp(request.getTimestamp());
        return wrapper;
    }

    private boolean isExpectedWalletError(final Throwable t) {
        return t instanceof WalletServiceException
                && !((WalletServiceException) t).isUnexpected();
    }
}
