package com.yazino.platform.service.community;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.gifting.PlayerCollectionStatus;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PlayerSessionDocumentProperties;
import com.yazino.platform.model.community.PublishStatusRequestArgument;
import com.yazino.platform.model.community.RelationshipDocumentProperties;
import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.repository.community.TableInviteRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.util.JsonHelper;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayerWorkerTest {
    private static final int ONLINE_FRIEND = 1;
    private static final int OFFLINE_FRIEND = 10;
    private static final int INVITATION_RECEIVED = 6;
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(INVITATION_RECEIVED);

    private BigDecimal playerId = new BigDecimal("11.11");

    private Player player;
    private Player mockPlayer;
    private InternalWalletService internalWalletService;

    private PlayerWorker worker;

    private TableInviteRepository tableInviteRepository;
    private PlayerSessionRepository playerSessionRepository;

    @Before
    public void setUp() {
        internalWalletService = mock(InternalWalletService.class);
        tableInviteRepository = mock(TableInviteRepository.class);
        playerSessionRepository = mock(PlayerSessionRepository.class);
        mockPlayer = mock(Player.class);
        player = new Player(playerId, "test", ACCOUNT_ID, "aPicture", null, null, null);
        worker = new PlayerWorker();
    }

    @Test
    public void getLocationDocumentProperties_retrieves_empty_object_when_offline() {
        Assert.assertEquals(PlayerSessionDocumentProperties.OFFLINE, worker.getLocationDocumentProperties(playerId, null, tableInviteRepository));
    }

    @Test
    public void buildRelationshipDocumentForInvitationReceived() {
        // create all types of player relationships for the player - include all relationship types

        Map<BigDecimal, Relationship> allPlayerRelationships = new HashMap<>();

        allPlayerRelationships.put(BigDecimal.valueOf(2), new Relationship("ignored1", RelationshipType.IGNORED));
        allPlayerRelationships.put(BigDecimal.valueOf(3), new Relationship("ignoredBy1", RelationshipType.IGNORED_BY));
        allPlayerRelationships.put(BigDecimal.valueOf(4), new Relationship("ignoredByFriend1", RelationshipType.IGNORED_BY_FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(5), new Relationship("ignoredFriend1", RelationshipType.IGNORED_FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(6), new Relationship("invitationRecievedFriend1", RelationshipType.INVITATION_RECEIVED));
        allPlayerRelationships.put(BigDecimal.valueOf(7), new Relationship("invitationSentFriend1", RelationshipType.INVITATION_SENT));
        allPlayerRelationships.put(BigDecimal.valueOf(8), new Relationship("noRelationship", RelationshipType.NO_RELATIONSHIP));
        allPlayerRelationships.put(BigDecimal.valueOf(9), new Relationship("notFriend", RelationshipType.NOT_FRIEND));

        when(mockPlayer.getRelationships()).thenReturn(allPlayerRelationships);

        // from all type of player relationships create a set of players that have session which indicates they are online across all relationship types

        final Map<BigDecimal, PlayerSessionsSummary> sessionsForOnlinePlayerRelationshipsOnly = new HashMap<>();

        when(playerSessionRepository.findOnlinePlayerSessions(allPlayerRelationships.keySet()))
                .thenReturn(sessionsForOnlinePlayerRelationshipsOnly);

        // execute code
        Document document = worker.buildRelationshipDocument(mockPlayer, playerSessionRepository, tableInviteRepository);

        JsonHelper jsonHelper = new JsonHelper();
        HashMap<BigDecimal, RelationshipDocumentProperties> deserialize
                = jsonHelper.deserialize(HashMap.class, document.getBody());
        Assert.assertEquals(1, deserialize.size());
        assertTrue(deserialize.containsKey(Integer.toString(INVITATION_RECEIVED)));
    }

    @Test
    public void buildRelationshipDocumentForOnLineFriends() {
        // create all types of player relationships for the player - include all relationship types

        Map<BigDecimal, Relationship> allPlayerRelationships = new HashMap<BigDecimal, Relationship>();
        allPlayerRelationships.put(BigDecimal.valueOf(ONLINE_FRIEND), new Relationship("aFriendWillBeOnLine", RelationshipType.FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(2), new Relationship("ignored1", RelationshipType.IGNORED));
        allPlayerRelationships.put(BigDecimal.valueOf(3), new Relationship("ignoredBy1", RelationshipType.IGNORED_BY));
        allPlayerRelationships.put(BigDecimal.valueOf(4), new Relationship("ignoredByFriend1", RelationshipType.IGNORED_BY_FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(5), new Relationship("ignoredFriend1", RelationshipType.IGNORED_FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(7), new Relationship("invitationSentFriend1", RelationshipType.INVITATION_SENT));
        allPlayerRelationships.put(BigDecimal.valueOf(8), new Relationship("noRelationship", RelationshipType.NO_RELATIONSHIP));
        allPlayerRelationships.put(BigDecimal.valueOf(9), new Relationship("notFriend", RelationshipType.NOT_FRIEND));

        when(mockPlayer.getRelationships()).thenReturn(allPlayerRelationships);

        // from all type of player relationships create a set of players that have session which indicates they are online across all relationship types

        final Map<BigDecimal, PlayerSessionsSummary> sessionsForOnlinePlayerRelationshipsOnly = new HashMap<>();
        sessionsForOnlinePlayerRelationshipsOnly.put(new BigDecimal(1), sessionFor(new BigDecimal(ONLINE_FRIEND)));
        sessionsForOnlinePlayerRelationshipsOnly.put(new BigDecimal(3), sessionFor(new BigDecimal(3)));


        when(playerSessionRepository.findOnlinePlayerSessions(allPlayerRelationships.keySet())).thenReturn(
                sessionsForOnlinePlayerRelationshipsOnly);

        // execute code
        Document document = worker.buildRelationshipDocument(mockPlayer, playerSessionRepository, tableInviteRepository);

        JsonHelper jsonHelper = new JsonHelper();
        HashMap<String, RelationshipDocumentProperties> deserialize = jsonHelper.deserialize(HashMap.class, document.getBody());

        Assert.assertEquals(1, deserialize.size());
        assertTrue(deserialize.containsKey(Integer.toString(ONLINE_FRIEND)));
    }

    @Test
    public void buildRelationshipDocumentOnlyIncludingPlayerRelationshipTypesOfInvitiationRecievedAndFriendsWhereFriendsMustBeOnline() {
        // create all types of player relationships for the player - include all relationship types

        Map<BigDecimal, Relationship> allPlayerRelationships = new HashMap<BigDecimal, Relationship>();
        allPlayerRelationships.put(BigDecimal.valueOf(ONLINE_FRIEND), new Relationship("aFriendWillBeOnLine", RelationshipType.FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(2), new Relationship("ignored1", RelationshipType.IGNORED));
        allPlayerRelationships.put(BigDecimal.valueOf(3), new Relationship("ignoredBy1", RelationshipType.IGNORED_BY));
        allPlayerRelationships.put(BigDecimal.valueOf(4), new Relationship("ignoredByFriend1", RelationshipType.IGNORED_BY_FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(5), new Relationship("ignoredFriend1", RelationshipType.IGNORED_FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(INVITATION_RECEIVED), new Relationship("invitationRecievedFriend1", RelationshipType.INVITATION_RECEIVED));
        allPlayerRelationships.put(BigDecimal.valueOf(7), new Relationship("invitationSentFriend1", RelationshipType.INVITATION_SENT));
        allPlayerRelationships.put(BigDecimal.valueOf(8), new Relationship("noRelationship", RelationshipType.NO_RELATIONSHIP));
        allPlayerRelationships.put(BigDecimal.valueOf(9), new Relationship("notFriend", RelationshipType.NOT_FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(OFFLINE_FRIEND), new Relationship("friend2", RelationshipType.FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(11), new Relationship("ignored2", RelationshipType.IGNORED));
        allPlayerRelationships.put(BigDecimal.valueOf(12), new Relationship("ignoredBy2", RelationshipType.IGNORED_BY));
        allPlayerRelationships.put(BigDecimal.valueOf(13), new Relationship("ignoredByFriend2", RelationshipType.IGNORED_BY_FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(14), new Relationship("ignoredFriend2", RelationshipType.IGNORED_FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(15), new Relationship("invitationReceived2", RelationshipType.INVITATION_RECEIVED));
        allPlayerRelationships.put(BigDecimal.valueOf(16), new Relationship("invitationSent2", RelationshipType.INVITATION_SENT));
        allPlayerRelationships.put(BigDecimal.valueOf(17), new Relationship("noRelationship2", RelationshipType.NO_RELATIONSHIP));
        allPlayerRelationships.put(BigDecimal.valueOf(18), new Relationship("notFriend2", RelationshipType.NOT_FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(19), new Relationship("friend2", RelationshipType.FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(20), new Relationship("offlineFriend1", RelationshipType.IGNORED));
        allPlayerRelationships.put(BigDecimal.valueOf(21), new Relationship("offlineFriend1", RelationshipType.IGNORED_BY));
        allPlayerRelationships.put(BigDecimal.valueOf(22), new Relationship("offlineFriend1", RelationshipType.IGNORED_BY_FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(23), new Relationship("offlineFriend1", RelationshipType.IGNORED_FRIEND));
        allPlayerRelationships.put(BigDecimal.valueOf(24), new Relationship("offlineFriend1", RelationshipType.INVITATION_RECEIVED));
        allPlayerRelationships.put(BigDecimal.valueOf(25), new Relationship("offlineFriend1", RelationshipType.INVITATION_SENT));
        allPlayerRelationships.put(BigDecimal.valueOf(26), new Relationship("offlineFriend1", RelationshipType.NO_RELATIONSHIP));
        allPlayerRelationships.put(BigDecimal.valueOf(27), new Relationship("offlineFriend1", RelationshipType.NOT_FRIEND));

        when(mockPlayer.getRelationships()).thenReturn(allPlayerRelationships);

        // from all type of player relationships create a set of players that have session which indicates they are online across all relationship types

        final Map<BigDecimal, PlayerSessionsSummary> sessionsForOnlinePlayerRelationshipsOnly = new HashMap<>();
        sessionsForOnlinePlayerRelationshipsOnly.put(new BigDecimal(1), sessionFor(new BigDecimal(ONLINE_FRIEND)));
        sessionsForOnlinePlayerRelationshipsOnly.put(new BigDecimal(INVITATION_RECEIVED), sessionFor(new BigDecimal(INVITATION_RECEIVED)));
        sessionsForOnlinePlayerRelationshipsOnly.put(new BigDecimal(3), sessionFor(new BigDecimal(3)));
        sessionsForOnlinePlayerRelationshipsOnly.put(new BigDecimal(4), sessionFor(new BigDecimal(4)));
        sessionsForOnlinePlayerRelationshipsOnly.put(new BigDecimal(6), sessionFor(new BigDecimal(6)));
        sessionsForOnlinePlayerRelationshipsOnly.put(new BigDecimal(11), sessionFor(new BigDecimal(11)));
        sessionsForOnlinePlayerRelationshipsOnly.put(new BigDecimal(19), sessionFor(new BigDecimal(12)));
        when(playerSessionRepository.findOnlinePlayerSessions(allPlayerRelationships.keySet())).thenReturn(sessionsForOnlinePlayerRelationshipsOnly);

        // execute code
        Document document = worker.buildRelationshipDocument(mockPlayer, playerSessionRepository, tableInviteRepository);

        JsonHelper jsonHelper = new JsonHelper();
        HashMap<String, RelationshipDocumentProperties> deserialize = jsonHelper.deserialize(HashMap.class, document.getBody());

        assertTrue(deserialize.containsKey(Integer.toString(ONLINE_FRIEND)));
        assertTrue(deserialize.containsKey(Integer.toString(INVITATION_RECEIVED)));
        assertTrue(deserialize.containsKey("15"));
        assertTrue(deserialize.containsKey("24"));
        assertTrue(deserialize.containsKey("19"));
        Assert.assertEquals(5, deserialize.size());
    }


    @Test
    public void playerBalanceDocumentContainsNoLocationsIfSessionNotPresent() throws WalletServiceException {
        final String expectedDocument = "{\"balance\":\"120\"}";

        when(internalWalletService.getBalance(ACCOUNT_ID)).thenReturn(BigDecimal.valueOf(120));

        final Document document = worker.buildPlayerBalanceDocument(player, internalWalletService);

        assertNotNull(document);
        Assert.assertEquals(expectedDocument, document.getBody());
    }

    @Test
    public void buildGiftReceivedDocumentShouldBuildDocumentForGiftReceived() {
        JsonHelper jsonHelper = new JsonHelper();
        final Document expected = new Document(DocumentType.GIFT_RECEIVED.getName(), jsonHelper.serialize(new HashMap<String, Object>()), new HashMap<String, String>());

        final Document actualDocument = worker.buildGiftReceivedDocument();
        Assert.assertThat(actualDocument.getType(), CoreMatchers.is(IsEqual.equalTo(expected.getType())));
        Assert.assertThat(actualDocument.getBody(), CoreMatchers.is(IsEqual.equalTo(expected.getBody())));
    }

    @Test
    public void buildGiftCollectionsRemainingDocumentShouldBuiltDocumentForGiftingPlayerCollectionStatus() {
        JsonHelper jsonHelper = new JsonHelper();
        HashMap<String, Object> arguments = new HashMap<>();
        arguments.put(PublishStatusRequestArgument.PLAYER_COLLECTION_STATUS.name(), new PlayerCollectionStatus(23, 10));
        final Document expectedDocument = new Document(DocumentType.GIFTING_PLAYER_COLLECTION_STATUS.getName(), jsonHelper.serialize(arguments), new HashMap<String, String>());
        Document actualDocument = worker.buildGiftingPlayerCollectionStatusDocument(arguments);

        Assert.assertThat(actualDocument, CoreMatchers.is(IsEqual.equalTo(expectedDocument)));
    }

    private PlayerSessionsSummary sessionFor(final BigDecimal balanceSnapshot) {
        return new PlayerSessionsSummary(
                "aNickname",
                "aPictureUrl",
                balanceSnapshot,
                null);
    }

}
