package com.yazino.platform.repository.community;

import com.gigaspaces.client.ReadModifiers;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.community.TableInvite;
import com.yazino.platform.model.community.TableInvitePersistenceRequest;
import com.yazino.platform.persistence.SequenceGenerator;
import org.joda.time.DateTime;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("tableInviteRepository")
public class GigaspaceTableInviteRepository implements TableInviteRepository {
    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final Routing routing;
    private final SequenceGenerator sequenceGenerator;

    @Autowired
    public GigaspaceTableInviteRepository(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                                          @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                          final Routing routing,
                                          final SequenceGenerator sequenceGenerator) {
        notNull(localGigaSpace, "localGigaSpace may not be null");
        notNull(globalGigaSpace, "globalGigaSpace may not be null");
        notNull(sequenceGenerator, "sequenceGenerator may not be null");
        notNull(routing, "routing may not be null");

        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
        this.routing = routing;
        this.sequenceGenerator = sequenceGenerator;
    }

    @Override
    public List<TableInvite> findInvitationsByPlayerId(final BigDecimal playerId) {
        final TableInvite template = new TableInvite();
        template.setPlayerId(playerId);
        template.setOpen(true);

        final List<TableInvite> result = Arrays.asList(spaceFor(playerId).readMultiple(
                template, Integer.MAX_VALUE, ReadModifiers.DIRTY_READ));
        Collections.sort(result);
        return result;
    }

    @Override
    public List<TableInvite> removeInvitationsByTableId(final BigDecimal tableId) {
        final TableInvite template = new TableInvite();
        template.setTableId(tableId);

        final TableInvite[] tableInvites = globalGigaSpace.takeMultiple(template, Integer.MAX_VALUE);
        for (TableInvite tableInvite : tableInvites) {
            if (tableInvite.isOpen()) {
                tableInvite.setOpen(false);
                final TableInvitePersistenceRequest request = new TableInvitePersistenceRequest(tableInvite);
                spaceFor(request.getSpaceId()).write(request);
            }
        }
        return Arrays.asList(tableInvites);
    }

    @Override
    public void invitePlayerToTable(final BigDecimal playerId,
                                    final BigDecimal tableId) {
        final TableInvite tableInvite = buildTableInvite(playerId, tableId);
        spaceFor(tableInvite.getPlayerId()).write(tableInvite);

        final TableInvitePersistenceRequest request = new TableInvitePersistenceRequest(tableInvite);
        spaceFor(request.getSpaceId()).write(request);
    }

    @Override
    public TableInvite findByTableAndPlayerId(final BigDecimal tableId,
                                              final BigDecimal playerId) {
        final TableInvite template = new TableInvite();
        template.setPlayerId(playerId);
        template.setTableId(tableId);
        template.setOpen(true);
        return spaceFor(playerId).read(template, 0, ReadModifiers.DIRTY_READ);
    }

    private GigaSpace spaceFor(final Object spaceId) {
        if (routing.isRoutedToCurrentPartition(spaceId)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }

    /**
     * Builds an open TableInvite for for player and table. If an invitation exists, its invitation
     * time is updated and is set to open.
     */
    private TableInvite buildTableInvite(final BigDecimal playerId,
                                         final BigDecimal tableId) {
        final TableInvite template = new TableInvite();
        template.setPlayerId(playerId);
        template.setTableId(tableId);

        TableInvite tableInvite = spaceFor(playerId).readIfExists(template);
        if (tableInvite == null) {
            tableInvite = new TableInvite(playerId, tableId, new DateTime());
            tableInvite.setId(sequenceGenerator.next());
        } else {
            tableInvite.setInviteTime(new DateTime());
            tableInvite.setOpen(Boolean.TRUE);
        }
        return tableInvite;
    }

}
