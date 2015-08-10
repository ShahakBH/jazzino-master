package com.yazino.host.community;

import com.yazino.platform.processor.community.LocationChangeNotificationProcessor;
import com.yazino.platform.service.account.InternalWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.TableInviteRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.service.session.transactional.TransactionalSessionService;

import static org.mockito.Mockito.mock;

@Component
public class StandaloneLocationChangeNotificationProcessor extends LocationChangeNotificationProcessor {
    @Autowired
    public StandaloneLocationChangeNotificationProcessor(
            @Qualifier("standaloneDocumentDispatcher") final DocumentDispatcher documentDispatcher,
            final PlayerRepository playerRepository,
            final InternalWalletService walletService,
            final TableInviteRepository tableInviteRepository,
            final PlayerSessionRepository playerSessionRepository) {
        super(documentDispatcher, playerRepository, walletService, tableInviteRepository,
                playerSessionRepository, mock(TransactionalSessionService.class));
    }


}
