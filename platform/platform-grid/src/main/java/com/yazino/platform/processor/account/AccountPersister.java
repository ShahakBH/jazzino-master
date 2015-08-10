package com.yazino.platform.processor.account;

import com.yazino.platform.event.message.AccountEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.AccountPersistenceRequest;
import com.yazino.platform.persistence.account.AccountDAO;
import com.yazino.platform.processor.PersistenceRequest;
import com.yazino.platform.processor.Persister;
import com.yazino.platform.repository.account.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Service("accountPersister")
@Qualifier("persister")
public class AccountPersister implements Persister<BigDecimal> {
    private static final Logger LOG = LoggerFactory.getLogger(AccountPersister.class);

    private final AccountDAO accountDAO;
    private final AccountRepository accountRepository;
    private final QueuePublishingService<AccountEvent> accountEventService;

    @Autowired
    public AccountPersister(final AccountDAO accountDAO,
                            final AccountRepository accountRepository,
                            @Qualifier("accountEventQueuePublishingService") final QueuePublishingService<AccountEvent> accountEventService) {
        notNull(accountDAO, "accountDAO may not be null");
        notNull(accountRepository, "accountRepository may not be null");
        notNull(accountEventService, "accountEventService may not be null");

        this.accountDAO = accountDAO;
        this.accountRepository = accountRepository;
        this.accountEventService = accountEventService;
    }

    @Override
    public Class<? extends PersistenceRequest<BigDecimal>> getPersistenceRequestClass() {
        return AccountPersistenceRequest.class;
    }

    @Override
    public PersistenceRequest<BigDecimal> persist(final PersistenceRequest<BigDecimal> request) {
        LOG.debug("Attempting to save balance for Account {}", request);

        PersistenceRequest<BigDecimal> afterProcessing = null;
        try {
            final Account account = accountRepository.findById(request.getObjectId());
            if (account == null) {
                LOG.error("Account not found! {}", request.getObjectId());
                return null;
            }
            accountDAO.saveAccount(account);

            accountEventService.send(new AccountEvent(account.getAccountId(), account.getBalance()));

            if (!account.isOpen()) {
                LOG.debug("Account {} closed, removing from space", request.getObjectId());
                accountRepository.remove(account);
            }

        } catch (Throwable t) {
            request.setStatus(AccountPersistenceRequest.Status.ERROR);
            afterProcessing = request;
            LOG.error("Exception saving account see stack trace: ", t);
        }

        return afterProcessing;
    }

}

