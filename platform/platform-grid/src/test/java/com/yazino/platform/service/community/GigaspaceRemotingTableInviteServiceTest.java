package com.yazino.platform.service.community;

import com.yazino.platform.community.TableInviteSummary;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocumentDispatcher;
import com.yazino.platform.messaging.host.TableInviteHostDocument;
import com.yazino.platform.model.community.TableInvite;
import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.repository.community.TableInviteRepository;
import com.yazino.platform.repository.session.InboxMessageRepository;
import com.yazino.platform.table.TableInvitationException;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.table.TableSummary;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.GameType;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceRemotingTableInviteServiceTest {

    private static final String PLAYER_NAME = "John Doe";
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final BigDecimal TABLE_ID = BigDecimal.TEN;
    private static final GameType GAME_TYPE = new GameType("BLACKJACK", "Blackjack", Collections.<String>emptySet());

    @Mock
    private InboxMessageRepository inboxMessageGlobalRepository;
    @Mock
    private TableInviteRepository tableInviteRepository;
    @Mock
    private HostDocumentDispatcher hostDocumentDispatcher;
    @Mock
    private DestinationFactory destinationFactory;
    @Mock
    private Destination destination;

    private GigaspaceRemotingTableInviteService underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(1000000);

        when(destinationFactory.player(PLAYER_ID)).thenReturn(destination);

        underTest = new GigaspaceRemotingTableInviteService(inboxMessageGlobalRepository, tableInviteRepository,
                hostDocumentDispatcher, destinationFactory);
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldSendInboxMessageEmailAndInvitePlayer() throws TableInvitationException {
        underTest.invitePlayerToTable(PLAYER_ID, PLAYER_NAME, TABLE_ID, GAME_TYPE);

        verify(inboxMessageGlobalRepository).send(new InboxMessage(PLAYER_ID,
                new NewsEvent.Builder(PLAYER_ID, new ParameterisedMessage("10"))
                        .setType(NewsEventType.TABLE_INVITE)
                        .setShortDescription(new ParameterisedMessage("You are invited to %s's %s table", PLAYER_NAME, "Blackjack"))
                        .setImage("").setDelay(0).setGameType(GAME_TYPE.getId()).build(), new DateTime()));
        verify(tableInviteRepository).invitePlayerToTable(PLAYER_ID, TABLE_ID);
    }

    @Test
    public void shouldSendInvitesToPlayer() throws TableInvitationException {
        underTest.sendInvitations(PLAYER_ID, asList(anInvite(1), anInvite(2)));

        verify(hostDocumentDispatcher).send(
                new TableInviteHostDocument(asList(anInvite(1), anInvite(2)), destination));
    }

    private TableInviteSummary anInvite(final int id) {
        return new TableInviteSummary(new TableSummary(BigDecimal.valueOf(id), "aName", TableStatus.open, "aGameType",
                new GameType("aGameType", "aGameName", new HashSet<String>()),
                BigDecimal.valueOf(id * 2), "aClientId", "aClientFile", "aTemplateName",
                "aMonitoringMessage", new HashSet<BigDecimal>(), Collections.<String>emptySet()), "anOwner" + id, "anOwnerPicture" + id);
    }

    @Test
    public void findTableInvitesByPlayerIdShouldDelegateToRepository() {
        final List<TableInvite> invites = new ArrayList<TableInvite>();
        invites.add(new TableInvite(new BigDecimal(123), new BigDecimal(1231), new DateTime()));
        invites.add(new TableInvite(new BigDecimal(321), new BigDecimal(3211), new DateTime()));
        when(tableInviteRepository.findInvitationsByPlayerId(PLAYER_ID)).thenReturn(invites);

        final List<BigDecimal> expectedTableIds = asList(new BigDecimal(1231), new BigDecimal(3211));

        assertEquals(expectedTableIds, underTest.findTableInvitesByPlayerId(PLAYER_ID));
    }

    @Test
    public void tableClosed_shouldDelegateToRepository() {
        underTest.tableClosed(TABLE_ID);

        verify(tableInviteRepository).removeInvitationsByTableId(TABLE_ID);
    }
}
