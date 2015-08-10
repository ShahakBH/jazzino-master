package com.yazino.platform.service.community;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.yazino.game.api.GameType;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.community.TableInviteService;
import com.yazino.platform.community.TableInviteSummary;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocumentDispatcher;
import com.yazino.platform.messaging.host.TableInviteHostDocument;
import com.yazino.platform.model.community.TableInvite;
import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.repository.community.TableInviteRepository;
import com.yazino.platform.repository.session.InboxMessageRepository;
import org.joda.time.DateTime;
import org.openspaces.remoting.RemotingService;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingTableInviteService implements TableInviteService {
    private static final TableIdTransformer TABLE_ID_TRANSFORMER = new TableIdTransformer();
    private static final String NEWS = "You are invited to %s's %s table";
    private static final String NEWS_SIMPLIFIED = "You have been invited to play at a %s table";

    private final InboxMessageRepository inboxMessageRepository;
    private final TableInviteRepository tableInviteRepository;
    private final HostDocumentDispatcher hostDocumentDispatcher;
    private final DestinationFactory destinationFactory;

    @Autowired
    public GigaspaceRemotingTableInviteService(final InboxMessageRepository inboxMessageRepository,
                                               final TableInviteRepository tableInviteRepository,
                                               final HostDocumentDispatcher hostDocumentDispatcher,
                                               final DestinationFactory destinationFactory) {
        notNull(inboxMessageRepository, "inboxMessageRepository is null");
        notNull(tableInviteRepository, "tableInviteRepository is null");
        notNull(hostDocumentDispatcher, "hostDocumentDispatcher is null");
        notNull(destinationFactory, "destinationFactory is null");

        this.inboxMessageRepository = inboxMessageRepository;
        this.tableInviteRepository = tableInviteRepository;
        this.hostDocumentDispatcher = hostDocumentDispatcher;
        this.destinationFactory = destinationFactory;
    }

    public List<BigDecimal> findTableInvitesByPlayerId(@Routing final BigDecimal playerId) {
        return new ArrayList<>(Lists.transform(tableInviteRepository.findInvitationsByPlayerId(playerId),
                TABLE_ID_TRANSFORMER));
    }

    public void invitePlayerToTable(@Routing final BigDecimal playerId,
                                    final String optionalOwnerName,
                                    final BigDecimal tableId,
                                    final GameType gameType) {
        notNull(playerId, "playerId may not be null");
        notNull(gameType, "gameType may not be null");
        notNull(tableId, "tableId may not be null");

        final String gameTypeName = gameType.getName();

        final ParameterisedMessage news;
        if (optionalOwnerName != null) {
            news = new ParameterisedMessage(NEWS, optionalOwnerName, gameTypeName);
        } else {
            news = new ParameterisedMessage(NEWS_SIMPLIFIED, gameTypeName);
        }

        inboxMessageRepository.send(new InboxMessage(playerId,
                new NewsEvent.Builder(playerId, new ParameterisedMessage(tableId.toString()))
                        .setType(NewsEventType.TABLE_INVITE)
                        .setShortDescription(news)
                        .setGameType(gameType.getId())
                        .build(), new DateTime()));

        tableInviteRepository.invitePlayerToTable(playerId, tableId);
    }

    @Override
    public void tableClosed(final BigDecimal tableId) {
        notNull(tableId, "Table Id cannot be null");

        tableInviteRepository.removeInvitationsByTableId(tableId);
    }

    @Override
    public void sendInvitations(final BigDecimal playerId,
                                final List<TableInviteSummary> allInvites) {
        notNull(playerId, "playerId may not be null");
        notNull(allInvites, "allInvites may not be null");

        hostDocumentDispatcher.send(new TableInviteHostDocument(allInvites, destinationFactory.player(playerId)));
    }

    private static class TableIdTransformer implements Function<TableInvite, BigDecimal> {
        @Override
        public BigDecimal apply(final TableInvite input) {
            if (input == null) {
                return null;
            }
            return input.getTableId();
        }
    }
}
