package com.yazino.platform.processor.community;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.model.community.LocationChangeNotification;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.TableInviteRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.session.transactional.TransactionalSessionService;
import com.yazino.platform.session.Location;
import com.yazino.platform.session.LocationChange;
import com.yazino.platform.session.LocationChangeType;
import com.yazino.platform.table.TableType;
import com.yazino.platform.util.JsonHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.table.TableType.PUBLIC;
import static com.yazino.platform.table.TableType.TOURNAMENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LocationChangeNotificationProcessorTest {

    private static final BigDecimal PLAYER_ID = new BigDecimal("13.1");
    private static final BigDecimal SESSION_ID = new BigDecimal("14.1");
    private static final BigDecimal ONLINE_FRIEND_ID = new BigDecimal("13.2");
    private static final BigDecimal OFFLINE_FRIEND_ID = new BigDecimal("13.5");
    private static final BigDecimal BLOCKED_ID = new BigDecimal("13.6");
    private static final BigDecimal ACCOUNT_BALANCE = BigDecimal.TEN;
    private static final String PUBLIC_LOCATION_ID = "100";
    private static final String TOURNAMENT_LOCATION_ID = "666";

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private DocumentDispatcher documentDispatcher;
    @Mock
    private PlayerSessionRepository playerSessionRepository;
    @Mock
    private InternalWalletService walletService;
    @Mock
    private TransactionalSessionService transactionalSessionService;

    private LocationChangeNotificationProcessor underTest;

    @Before
    public void wireMocks() throws WalletServiceException {
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player());
        when(playerSessionRepository.isOnline(PLAYER_ID)).thenReturn(true);
        when(playerSessionRepository.findOnlinePlayers(isA(Set.class))).thenReturn(newHashSet(ONLINE_FRIEND_ID));
        when(walletService.getBalance(player().getAccountId())).thenReturn(ACCOUNT_BALANCE);

        underTest = new LocationChangeNotificationProcessor(
                documentDispatcher, playerRepository, walletService,
                mock(TableInviteRepository.class), playerSessionRepository, transactionalSessionService);
    }

    @Test
    public void onlineFriendsGetNotifiedOfLocationChange() throws WalletServiceException {
        when(transactionalSessionService.updateSession(any(LocationChange.class), any(BigDecimal.class))).thenReturn(new PlayerSessionsSummary("nickname",
                                                                                                                                               "picture",
                                                                                                                                               BigDecimal.TEN,
                                                                                                                                               new HashSet<Location>()));
        underTest.processLocationChange(locationChangeNotification(PUBLIC));

        verify(documentDispatcher).dispatch(isA(Document.class), eq(ONLINE_FRIEND_ID));
        verify(transactionalSessionService).updateSession(locationChange(PUBLIC), ACCOUNT_BALANCE);
    }

    @Test
    public void tournamentLocationsAreFiltered() throws WalletServiceException {
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        when(transactionalSessionService.updateSession(any(LocationChange.class), any(BigDecimal.class)))
                .thenReturn(new PlayerSessionsSummary("nickname", "picture", BigDecimal.TEN,
                                                      newHashSet(location(PUBLIC_LOCATION_ID, PUBLIC), location(TOURNAMENT_LOCATION_ID, TOURNAMENT))));

        underTest.processLocationChange(locationChangeNotification(TOURNAMENT));

        verify(documentDispatcher).dispatch(captor.capture(), eq(ONLINE_FRIEND_ID));
        Map docBody = new JsonHelper().deserialize(HashMap.class, captor.getValue().getBody());
        List<Map> locations = (List) ((Map) docBody.get(PLAYER_ID.toString())).get("locations");
        assertThat(locations, allOf(hasItem(hasEntry("locationId", PUBLIC_LOCATION_ID)), not(hasItem(hasEntry("locationId", TOURNAMENT_LOCATION_ID)))));
    }

    @Test
    public void whenALocationChangeNotificationisReceivedThePlayerSessionIsUpdated() {
        underTest.processLocationChange(locationChangeNotification(PUBLIC));

        verify(transactionalSessionService).updateSession(locationChange(PUBLIC), ACCOUNT_BALANCE);
    }

    @Test
    public void whenALocationChangeNotificationisReceivedForAnOnlineUserTheGlobalPlayerListIsUpdated() {
        when(playerSessionRepository.isOnline(PLAYER_ID)).thenReturn(true);

        underTest.processLocationChange(locationChangeNotification(PUBLIC));

        verify(playerSessionRepository).updateGlobalPlayerList(PLAYER_ID);
    }

    @Test
    public void whenALocationChangeNotificationisReceivedForAnOfflineUserTheGlobalPlayerListIsUpdated() {
        when(playerSessionRepository.isOnline(PLAYER_ID)).thenReturn(false);

        underTest.processLocationChange(locationChangeNotification(PUBLIC));

        verify(playerSessionRepository).updateGlobalPlayerList(PLAYER_ID);
    }

    private Player player() {
        Player p = new Player(PLAYER_ID);
        p.setRelationship(ONLINE_FRIEND_ID, new Relationship("m", RelationshipType.FRIEND));
        p.setRelationship(OFFLINE_FRIEND_ID, new Relationship("m2", RelationshipType.FRIEND));
        p.setRelationship(BLOCKED_ID, new Relationship("m3", RelationshipType.IGNORED));
        return p;
    }

    private LocationChangeNotification locationChangeNotification(TableType tableType) {
        return new LocationChangeNotification(PLAYER_ID,
                                              SESSION_ID, LocationChangeType.ADD, new Location("182", "Atlantic Blackjack", "BLACKJACK", null, tableType));
    }

    private LocationChange locationChange(TableType tableType) {
        return new LocationChange(PLAYER_ID,
                                  SESSION_ID, LocationChangeType.ADD, new Location("182", "Atlantic Blackjack", "BLACKJACK", null, tableType));
    }

    private Location location(String locationId, TableType tableType) {
        return new Location(locationId, "loc name", "BLACKJACK", null, tableType);
    }
}
