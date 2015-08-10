package com.yazino.platform.processor.account;

import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.CloseAccountRequest;
import com.yazino.platform.repository.account.AccountRepository;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class CloseAccountProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(CloseAccountProcessor.class);
    private AccountRepository accountRepository;

    @Autowired
    public void setAccountRepository(final AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @EventTemplate
    public CloseAccountRequest getTemplate() {
        return new CloseAccountRequest();
    }

    @SpaceDataEvent
    @Transactional
    public void process(final CloseAccountRequest request) {
        notNull(request, "Request is required.");

        final BigDecimal accountId = request.getAccountId();
        notNull(accountId, "Account id is required.");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Closing account: " + accountId);
        }

        Account account = accountRepository.findById(accountId);
        if (account == null) {
            LOG.warn("Cannot close account " + accountId + ": not present in space");
            return;
        }

        account = accountRepository.lock(accountId);
        account.setOpen(false);

        accountRepository.save(account);
    }
}
