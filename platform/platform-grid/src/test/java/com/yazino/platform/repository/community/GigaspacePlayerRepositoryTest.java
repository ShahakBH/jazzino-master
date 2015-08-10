package com.yazino.platform.repository.community;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.community.RelatedPlayer;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.grid.Executor;
import com.yazino.platform.grid.ExecutorTestUtils;
import com.yazino.platform.grid.SpaceAccess;
import com.yazino.platform.model.community.*;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.persistence.community.PlayerDAO;
import com.yazino.platform.processor.community.PlayerLastPlayedPersistenceRequest;
import com.yazino.platform.processor.table.PlayerLastPlayedUpdateRequest;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import net.jini.core.lease.Lease;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaspacePlayerRepositoryTest {
    private static final BigDecimal LOCAL_ID = BigDecimal.valueOf(1);
    private static final BigDecimal REMOTE_ID = BigDecimal.valueOf(2);
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);

    @Mock
    private GigaSpace localSpace;
    @Mock
    private GigaSpace globalSpace;
    @Mock
    private PlayerDAO playerDao;
    @Mock
    private PlayerSessionRepository injectedPlayerSessionRepository;
    @Mock
    private PlayerRepository injectedPlayerRepository;
    @Mock
    private SpaceAccess space;

    private GigaspacePlayerRepository underTest;

    @Before
    public void setUp() {
        final Map<String, Object> injectedServices = newHashMap();
        injectedServices.put("playerRepository", injectedPlayerRepository);
        injectedServices.put("playerSessionRepository", injectedPlayerSessionRepository);
        final Executor executor = ExecutorTestUtils.mockExecutorWith(3, injectedServices, REMOTE_ID);

        when(space.isRoutedLocally(LOCAL_ID)).thenReturn(true);
        when(space.local()).thenReturn(localSpace);
        when(space.global()).thenReturn(globalSpace);
        when(space.forRouting(LOCAL_ID)).thenReturn(localSpace);
        when(space.forRouting(REMOTE_ID)).thenReturn(globalSpace);

        underTest = new GigaspacePlayerRepository(space, playerDao, executor);
    }

    @Test(expected = NullPointerException.class)
    public void repositoryCannotBeCreatedWithANullSpace() {
        new GigaspacePlayerRepository(null, playerDao, mock(Executor.class));
    }

    @Test(expected = NullPointerException.class)
    public void repositoryCannotBeCreatedWithANullPlayerDAO() {
        new GigaspacePlayerRepository(space, null, mock(Executor.class));
    }

    @Test(expected = NullPointerException.class)
    public void repositoryCannotBeCreatedWithANullExecutor() {
        new GigaspacePlayerRepository(space, playerDao, null);
    }

    @Test(expected = NullPointerException.class)
    public void findingANullPlayerIdThrowsANullPointerException() {
        underTest.findById(null);
    }

    @Test
    public void aPlayerIsReadFromTheLocalSpaceWhenTheIdRoutesLocallyAndThePlayerIsInTheGrid() {
        when(localSpace.readById(Player.class, LOCAL_ID, LOCAL_ID, 0, ReadModifiers.DIRTY_READ)).thenReturn(aPlayerWithId(LOCAL_ID));

        assertThat(underTest.findById(LOCAL_ID), is(equalTo(aPlayerWithId(LOCAL_ID))));
        verifyZeroInteractions(globalSpace);
    }

    @Test
    public void aPlayerIsReadFromTheDBWhenTheIdRoutesLocallyAndThePlayerIsNotInTheGrid() {
        when(playerDao.findById(LOCAL_ID)).thenReturn(aPlayerWithId(LOCAL_ID));

        assertThat(underTest.findById(LOCAL_ID), is(equalTo(aPlayerWithId(LOCAL_ID))));
        verifyZeroInteractions(globalSpace);
    }

    @Test
    public void aPlayerThatDoesNotExistInTheDBIsNotWrittenIntoTheSpace() {
        assertThat(underTest.findById(LOCAL_ID), is(nullValue()));
        verify(localSpace).readById(Player.class, LOCAL_ID, LOCAL_ID, 0, ReadModifiers.DIRTY_READ);
        verifyNoMoreInteractions(localSpace);
        verifyZeroInteractions(globalSpace);
    }

    @Test
    public void aPlayerIsWrittenBackToTheLocalSpaceWhenTheIdRoutesLocallyAndThePlayerIsNotInTheGrid() {
        when(playerDao.findById(LOCAL_ID)).thenReturn(aPlayerWithId(LOCAL_ID));

        assertThat(underTest.findById(LOCAL_ID), is(equalTo(aPlayerWithId(LOCAL_ID))));
        verify(localSpace).write(aPlayerWithId(LOCAL_ID), Lease.FOREVER, 5000, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void aPlayerIsReadFromTheGlobalSpaceWhenTheIdDoesNotRouteLocallyAndThePlayerIsInTheGrid() {
        when(globalSpace.readById(Player.class, REMOTE_ID, REMOTE_ID, 0, ReadModifiers.DIRTY_READ)).thenReturn(aPlayerWithId(REMOTE_ID));

        assertThat(underTest.findById(REMOTE_ID), is(equalTo(aPlayerWithId(REMOTE_ID))));
        verifyZeroInteractions(localSpace);
    }

    @Test
    public void aPlayerIsReadFromTheDBWhenTheIdDoesNotRouteLocallyAndThePlayerIsNotInTheGrid() {
        when(playerDao.findById(REMOTE_ID)).thenReturn(aPlayerWithId(REMOTE_ID));

        assertThat(underTest.findById(REMOTE_ID), is(equalTo(aPlayerWithId(REMOTE_ID))));
        verifyZeroInteractions(localSpace);
    }

    @Test
    public void aPlayerIsWrittenBackToTheGlobalSpaceWhenTheIdDoesNotRouteLocallyAndThePlayerIsNotInTheGrid() {
        when(playerDao.findById(REMOTE_ID)).thenReturn(aPlayerWithId(REMOTE_ID));

        assertThat(underTest.findById(REMOTE_ID), is(equalTo(aPlayerWithId(REMOTE_ID))));
        verify(globalSpace).write(aPlayerWithId(REMOTE_ID), Lease.FOREVER, 5000, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test(expected = NullPointerException.class)
    public void lockingANullPlayerIdThrowsANullPointerException() {
        underTest.lock(null);
    }

    @Test
    public void aPlayerFromTheLocalSpaceCanBeLocked() {
        when(localSpace.readById(Player.class, LOCAL_ID, LOCAL_ID, 5000, ReadModifiers.EXCLUSIVE_READ_LOCK)).thenReturn(aPlayerWithId(LOCAL_ID));

        assertThat(underTest.lock(LOCAL_ID), is(equalTo(aPlayerWithId(LOCAL_ID))));
        verifyZeroInteractions(globalSpace);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void lockingAPlayerThatDoesNotExistOrIsLockedInTheLocalSpaceCausesAConcurrentModificationException() {
        underTest.lock(LOCAL_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void lockingAPlayerThatIsNotRoutedLocallyCausesAnIllegalArgumentException() {
        underTest.lock(REMOTE_ID);
    }

    @Test(expected = NullPointerException.class)
    public void savingANullPlayerThrowsANullPointerException() {
        underTest.save(null);
    }

    @Test
    public void savingAPlayerRoutedToTheLocalSpaceWritesItAndAPersistenceRequestToTheLocalSpace() {
        underTest.save(aPlayerWithId(LOCAL_ID));

        verify(localSpace).writeMultiple(new Object[]{aPlayerWithId(LOCAL_ID), new PlayerPersistenceRequest(LOCAL_ID)},
                Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void savingAPlayerThatIsNotRoutedToTheLocalSpaceWritesItAndAPersistenceRequestToTheGlobalSpace() {
        underTest.save(aPlayerWithId(REMOTE_ID));

        verify(globalSpace).writeMultiple(new Object[]{aPlayerWithId(REMOTE_ID), new PlayerPersistenceRequest(REMOTE_ID)},
                Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test(expected = NullPointerException.class)
    public void savingANullPublishStatusRequestThrowsANullPointerException() {
        underTest.savePublishStatusRequest(null);
    }

    @Test
    public void savingAPublishStatusRequestRoutedToTheLocalSpaceWriteItToTheLocalSpaceWithAThirtySecondLease() {
        underTest.savePublishStatusRequest(new PublishStatusRequest(LOCAL_ID));

        verify(localSpace).write(new PublishStatusRequest(LOCAL_ID), 30000);
    }

    @Test
    public void savingAPublishStatusRequestThatIsNotRoutedToTheLocalSpaceWriteItToTheGlobalSpaceWithAThirtySecondLease() {
        underTest.savePublishStatusRequest(new PublishStatusRequest(REMOTE_ID));

        verify(globalSpace).write(new PublishStatusRequest(REMOTE_ID), 30000);
    }

    @Test
    public void aNonexistentPlayerReturnsANullSummary() {
        final PlayerSessionSummary summary = underTest.findSummaryByPlayerAndSession(REMOTE_ID, SESSION_ID);

        assertThat(summary, is(nullValue()));
    }

    @Test
    public void aPlayerWithNoSessionReturnsASummaryWithANullSessionKey() {
        when(injectedPlayerRepository.findById(REMOTE_ID)).thenReturn(aNamedPlayerWithId(REMOTE_ID));

        final PlayerSessionSummary summary = underTest.findSummaryByPlayerAndSession(REMOTE_ID, null);

        assertThat(summary, is(equalTo(aSummaryWithId(null))));
    }

    @Test
    public void aPlayerWithASessionReturnsASummaryWithASessionKey() {
        when(injectedPlayerRepository.findById(REMOTE_ID)).thenReturn(aNamedPlayerWithId(REMOTE_ID));
        when(injectedPlayerSessionRepository.findAllByPlayer(REMOTE_ID)).thenReturn(asList(aSession()));

        final PlayerSessionSummary summary = underTest.findSummaryByPlayerAndSession(REMOTE_ID, SESSION_ID);

        assertThat(summary, is(equalTo(aSummaryWithId(SESSION_ID))));
    }

    @Test
    public void aPlayerWithNoSpecifiedSessionReturnsASummaryWithTheFirstSession() {
        when(injectedPlayerRepository.findById(REMOTE_ID)).thenReturn(aNamedPlayerWithId(REMOTE_ID));
        when(injectedPlayerSessionRepository.findAllByPlayer(REMOTE_ID)).thenReturn(asList(aSession()));

        final PlayerSessionSummary summary = underTest.findSummaryByPlayerAndSession(REMOTE_ID, null);

        assertThat(summary, is(equalTo(aSummaryWithId(SESSION_ID))));
    }

    @Test
    public void savingAPlayerLastPlayedRoutedToTheLocalSpaceWritesItAndAPersistenceRequestToTheLocalSpace() {
        underTest.saveLastPlayed(aPlayerWithId(LOCAL_ID));

        verify(localSpace).writeMultiple(new Object[]{aPlayerWithId(LOCAL_ID), new PlayerLastPlayedPersistenceRequest(LOCAL_ID)},
                Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void savingAPlayerLastPlayedThatIsNotRoutedToTheLocalSpaceWritesItAndAPersistenceRequestToTheGlobalSpace() {
        underTest.saveLastPlayed(aPlayerWithId(REMOTE_ID));

        verify(globalSpace).writeMultiple(new Object[]{aPlayerWithId(REMOTE_ID), new PlayerLastPlayedPersistenceRequest(REMOTE_ID)},
                Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void requestingALastPlayedUpdateWithAnEmptyArrayDoesNotWriteAnythingToTheSpace() {
        underTest.requestLastPlayedUpdates(new PlayerLastPlayedUpdateRequest[0]);

        verifyZeroInteractions(localSpace, globalSpace);
    }

    @Test
    public void requestingALastPlayedUpdateWriteEachRequestToTheAppropriateSpace() {
        final PlayerLastPlayedUpdateRequest localUpdate = new PlayerLastPlayedUpdateRequest(LOCAL_ID, new DateTime());
        final PlayerLastPlayedUpdateRequest remoteUpdate = new PlayerLastPlayedUpdateRequest(REMOTE_ID, new DateTime());
        underTest.requestLastPlayedUpdates(new PlayerLastPlayedUpdateRequest[]{localUpdate, remoteUpdate});

        verify(localSpace).write(localUpdate);
        verify(globalSpace).write(remoteUpdate);
    }

    @Test(expected = NullPointerException.class)
    public void requestingRelationshipActionsThrowsANullPointerExceptionForANullSetOfActions() {
        underTest.requestRelationshipChanges(null);
    }

    @Test
    public void requestingRelationshipActionsTakesNoActionIfTheListOfActionsIsEmpty() {
        underTest.requestRelationshipChanges(Collections.<RelationshipActionRequest>emptySet());

        verifyZeroInteractions(localSpace, globalSpace);
    }

    @Test
    public void requestingRelationshipActionsWritesAllActionsToTheGlobalSpace() {
        final HashSet<RelationshipActionRequest> relationships = newHashSet(aRelationshipAction(1), aRelationshipAction(2));
        underTest.requestRelationshipChanges(relationships);

        verify(globalSpace).writeMultiple(relationships.toArray(new RelationshipActionRequest[relationships.size()]));
    }

    @Test(expected = NullPointerException.class)
    public void publishingAFriendsSummaryThrowsANullPointerExceptionForANullPlayerId() {
        underTest.publishFriendsSummary(null);
    }

    @Test
    public void publishingAFriendsSummaryForALocallyRoutedPlayerPublishesASummaryRequestToTheLocalSpace() {
        underTest.publishFriendsSummary(LOCAL_ID);

        verify(localSpace).write(new FriendsSummaryRequest(LOCAL_ID), Lease.FOREVER, 1000L, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void publishingAFriendsSummaryForARemotelyRoutedPlayerPublishesASummaryRequestToTheRemoteSpace() {
        underTest.publishFriendsSummary(REMOTE_ID);

        verify(globalSpace).write(new FriendsSummaryRequest(REMOTE_ID), Lease.FOREVER, 1000L, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test(expected = NullPointerException.class)
    public void aRequestForFriendRegistrationThrowsANullPointerExceptionForANullPlayerId() {
        final HashSet<BigDecimal> friendIds = newHashSet(BigDecimal.valueOf(10), BigDecimal.valueOf(20));
        underTest.requestFriendRegistration(null, friendIds);
    }

    @Test(expected = NullPointerException.class)
    public void aRequestForFriendRegistrationThrowsANullPointerExceptionForANullSetOfFriends() {
        underTest.requestFriendRegistration(LOCAL_ID, null);
    }

    @Test
    public void aRequestForFriendRegistrationIgnoresAnEmptySetOfFriends() {
        underTest.requestFriendRegistration(LOCAL_ID, Collections.<BigDecimal>emptySet());

        verifyZeroInteractions(localSpace, globalSpace);
    }

    @Test
    public void aRequestForFriendRegistrationForALocallyRoutedPlayerIsWrittenToTheLocalSpace() {
        final HashSet<BigDecimal> friendIds = newHashSet(BigDecimal.valueOf(10), BigDecimal.valueOf(20));
        underTest.requestFriendRegistration(LOCAL_ID, friendIds);

        verify(localSpace).write(new FriendRegistrationRequest(LOCAL_ID, friendIds));
    }

    @Test
    public void aRequestForFriendRegistrationForARemotelyRoutedPlayerIsWrittenToTheGlobalSpace() {
        final HashSet<BigDecimal> friendIds = newHashSet(BigDecimal.valueOf(10), BigDecimal.valueOf(20));
        underTest.requestFriendRegistration(REMOTE_ID, friendIds);

        verify(globalSpace).write(new FriendRegistrationRequest(REMOTE_ID, friendIds));
    }

    @Test
    public void addTagWritesALocalTaggingRequestToTheLocalSpace() {
        underTest.addTag(LOCAL_ID, "aTag");

        verify(localSpace).write(new PlayerTaggingRequest(LOCAL_ID, "aTag", PlayerTaggingRequest.Action.ADD));
    }

    @Test
    public void addTagWritesARemoteTaggingRequestToTheRemoteSpace() {
        underTest.addTag(REMOTE_ID, "aTag");

        verify(globalSpace).write(new PlayerTaggingRequest(REMOTE_ID, "aTag", PlayerTaggingRequest.Action.ADD));
    }

    @Test
    public void removeTagWritesALocalTaggingRequestToTheLocalSpace() {
        underTest.removeTag(LOCAL_ID, "aTag");

        verify(localSpace).write(new PlayerTaggingRequest(LOCAL_ID, "aTag", PlayerTaggingRequest.Action.REMOVE));
    }

    @Test
    public void removeTagWritesARemoteTaggingRequestToTheRemoteSpace() {
        underTest.removeTag(REMOTE_ID, "aTag");

        verify(globalSpace).write(new PlayerTaggingRequest(REMOTE_ID, "aTag", PlayerTaggingRequest.Action.REMOVE));
    }

    private RelationshipActionRequest aRelationshipAction(final int playerId) {
        return new RelationshipActionRequest(BigDecimal.valueOf(playerId),
                newHashSet(new RelatedPlayer(BigDecimal.valueOf(playerId + 1), "aPlayer", RelationshipAction.ACCEPT_FRIEND, false)));
    }

    private PlayerSession aSession() {
        final PlayerSession playerSession = new PlayerSession(REMOTE_ID);
        playerSession.setSessionId(SESSION_ID);
        return playerSession;
    }

    private Player aPlayerWithId(final BigDecimal id) {
        return new Player(id);
    }

    private Player aNamedPlayerWithId(final BigDecimal id) {
        return new Player(id, "aPlayerName", id.add(BigDecimal.ONE), "aPictureUrl", null, null, null);
    }

    private PlayerSessionSummary aSummaryWithId(final BigDecimal sessionId) {
        return new PlayerSessionSummary(REMOTE_ID, REMOTE_ID.add(BigDecimal.ONE), "aPlayerName", sessionId);
    }

}
