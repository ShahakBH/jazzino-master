package com.yazino.platform.repository.community;

import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.community.TableInvite;
import com.yazino.platform.model.community.TableInvitePersistenceRequest;
import com.yazino.platform.persistence.SequenceGenerator;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static com.gigaspaces.internal.utils.Assert.notNull;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaspaceTableInviteRepositoryIntegrationTest {

    @Autowired
    private GigaSpace gigaSpace;
    @Autowired
    private Routing routing;

    private GigaspaceTableInviteRepository tableInviteRepository;

    private final SequenceGenerator sequenceGenerator = mock(SequenceGenerator.class);

    BigDecimal playerA = new BigDecimal(1234);
    BigDecimal playerB = new BigDecimal(4321);
    BigDecimal playerC = new BigDecimal(999);

    BigDecimal tableA = new BigDecimal(5678);
    BigDecimal tableB = new BigDecimal(8765);

    BigDecimal invite1Id = BigDecimal.valueOf(1);
    BigDecimal invite2Id = BigDecimal.valueOf(2);
    BigDecimal invite3Id = BigDecimal.valueOf(3);

    DateTime inviteTime = new DateTime().minus(1000);

    TableInvite invite1 = tableInvite(invite1Id, playerA, tableA, inviteTime, true);
    TableInvite invite2 = tableInvite(invite2Id, playerA, tableB, inviteTime, true);
    TableInvite invite3 = tableInvite(invite3Id, playerB, tableA, inviteTime, true);

    @Before
    public void setUp() {
        tableInviteRepository = new GigaspaceTableInviteRepository(gigaSpace, gigaSpace, routing, sequenceGenerator);

        gigaSpace.clear(null);
        gigaSpace.write(invite1);
        gigaSpace.write(invite2);
        gigaSpace.write(invite3);
    }

    @Test
    public void findInvitationsByPlayerIdShouldReturnCorrectInvitations() {
        TableInvite table4 = tableInvite(BigDecimal.TEN, playerA, tableA, inviteTime, false);
        gigaSpace.write(table4);
        List<TableInvite> expectedInvites = Arrays.asList(invite1, invite2);
        List<TableInvite> tableInvites = tableInviteRepository.findInvitationsByPlayerId(playerA);

        assertEquals(2, tableInvites.size());
        assertTrue(expectedInvites.containsAll(tableInvites));
    }

    @Test
    public void removeInvitationsByTableId_shouldEmptyList() {
        List<TableInvite> actualTableInvitesRemoved = tableInviteRepository.removeInvitationsByTableId(BigDecimal.ONE);

        notNull(actualTableInvitesRemoved);
        assertTrue(actualTableInvitesRemoved.isEmpty());
    }

    @Test
    public void removeInvitationsByTableId_shouldReturnCorrectInvitationsAndWritePersistenceRequest() {
        TableInvite table1Closed = tableInvite(invite1Id, invite1.getPlayerId(), invite1.getTableId(), invite1.getInviteTime(), false);
        TableInvite table3Closed = tableInvite(invite3Id, invite3.getPlayerId(), invite3.getTableId(), invite3.getInviteTime(), false);

        List<TableInvite> expectedTableInvites = Arrays.asList(table1Closed, table3Closed);

        List<TableInvite> actualTableInvites = tableInviteRepository.removeInvitationsByTableId(tableA);
        TableInvitePersistenceRequest[] actualRequests = gigaSpace.readMultiple(new TableInvitePersistenceRequest(), Integer.MAX_VALUE);

        assertEquals(2, actualTableInvites.size());
        assertTrue(expectedTableInvites.containsAll(actualTableInvites));
        TableInvite template = new TableInvite();
        template.setTableId(tableA);
        assertEquals(0, gigaSpace.count(template));
        assertEquals(2, actualRequests.length);
    }

    @Test
    public void invitePlayerToTableShouldWriteInvitationToGigaspacesAndRequestPersistence() {
        TableInvite template = new TableInvite();
        template.setPlayerId(playerC);
        template.setTableId(tableA);

        when(sequenceGenerator.next()).thenReturn(BigDecimal.ONE);

        tableInviteRepository.invitePlayerToTable(playerC, tableA);

        TableInvite result = gigaSpace.read(template);
        assertNotNull(result);

        DateTime now = new DateTime();

        assert (now.getMillis() - result.getInviteTime().getMillis() < 1000);

        TableInvitePersistenceRequest request = gigaSpace.readIfExists(new TableInvitePersistenceRequest());

        assertNotNull(request);

        assertEquals(result, request.getTableInvite());
    }

    @Test
    public void invitePlayerToTable_shouldUpdateExistingGigaspaceInvitationAndWritePersistenceRequest() {
        TableInvite existingInvite = invite1;
        tableInviteRepository.invitePlayerToTable(existingInvite.getPlayerId(), existingInvite.getTableId());

        TableInvite template = new TableInvite();
        template.setPlayerId(existingInvite.getPlayerId());
        template.setTableId(existingInvite.getTableId());
        TableInvite actualTableInvites[] = gigaSpace.readMultiple(template, Integer.MAX_VALUE);

        assertNotNull(actualTableInvites);
        assertEquals(1, actualTableInvites.length);
        TableInvite updatedInvite = actualTableInvites[0];
        assertEquals(existingInvite.getId(), updatedInvite.getId());
        assertEquals(existingInvite.getPlayerId(), updatedInvite.getPlayerId());
        assertEquals(existingInvite.getTableId(), updatedInvite.getTableId());
        assertTrue(updatedInvite.getInviteTime().isAfter(existingInvite.getInviteTime()));
        assertTrue(updatedInvite.isOpen());

        TableInvitePersistenceRequest request = gigaSpace.readIfExists(new TableInvitePersistenceRequest());

        assertNotNull(request);
    }

    private TableInvite tableInvite(BigDecimal id, BigDecimal playerId, BigDecimal tableId, DateTime inviteTime, Boolean open) {
        TableInvite invite = new TableInvite(playerId, tableId, inviteTime, open);
        invite.setId(id);
        return invite;
    }

}
