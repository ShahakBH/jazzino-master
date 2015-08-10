package com.yazino.platform.service.community;

import com.j_spaces.core.LeaseContext;
import com.yazino.platform.chat.ChatService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.model.community.LocationChangeNotification;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.RelationshipActionRequest;
import com.yazino.platform.model.community.UpdatePlayerRequest;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.SystemMessageRepository;
import com.yazino.platform.session.LocationChangeType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"UnusedDeclaration"})
public class GigaspaceRemotingCommunityServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(248);
    private static final BigDecimal PLAYER_2_ID = BigDecimal.valueOf(1000);
    private static final String GAME_TYPE = "BLACKJACK";
    private static final String REQUEST_ID = "dfdsfsdfdfs";
    private static final long TIMEOUT = 5000;

    @Mock
    private GigaSpace communitySpace;
    @Mock
    private GigaSpace globalCommunitySpace;
    @Mock
    private ChatService chatService;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private SystemMessageRepository systemMessageRepository;
    @Mock
    private LeaseContext leaseContext;

    private GigaspaceRemotingCommunityService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new GigaspaceRemotingCommunityService(communitySpace, globalCommunitySpace,
                playerRepository, systemMessageRepository);
    }

    @Test
    public void locationChangeWrittenToSpaceWhenUserLogsOn() {
        underTest.sendPlayerLoggedOn(PLAYER_ID, SESSION_ID);

        verify(communitySpace).write(new LocationChangeNotification(PLAYER_ID, SESSION_ID, LocationChangeType.LOG_ON, null));
    }

    @Test
    public void locationChangesAreWrittenToTheSpaceWhenUserLogsOn() {
        underTest.sendPlayerLoggedOn(PLAYER_ID, SESSION_ID);

        verify(communitySpace).write(new LocationChangeNotification(PLAYER_ID, SESSION_ID, LocationChangeType.LOG_ON, null));
    }

    @Test
    public void shouldWriteUpdatePlayerRequestIntoCommunitySpace() {
        final PaymentPreferences paymentPreferences = new PaymentPreferences(Currency.EUR);
        UpdatePlayerRequest request = new UpdatePlayerRequest(PLAYER_ID, "Display name", "picture location", paymentPreferences);
        underTest.updatePlayer(request.getPlayerId(), request.getDisplayName(), request.getPictureLocation(), paymentPreferences);
        verify(communitySpace).write(request);
    }

    @Test
    public void requestRelationshipChange_posts_two_inverse_requests_to_both_players() {
        when(playerRepository.findById(PLAYER_ID)).thenReturn(aPlayer(PLAYER_ID));
        when(playerRepository.findById(PLAYER_2_ID)).thenReturn(aPlayer(PLAYER_2_ID));

        underTest.requestRelationshipChange(PLAYER_ID, PLAYER_2_ID, RelationshipAction.IGNORE);

        verify(communitySpace).write(new RelationshipActionRequest(PLAYER_ID, PLAYER_2_ID,
                "nameOf" + PLAYER_2_ID, RelationshipAction.IGNORE, false));
        verify(globalCommunitySpace).write(new RelationshipActionRequest(PLAYER_2_ID, PLAYER_ID,
                "nameOf" + PLAYER_ID, RelationshipAction.IGNORE, true));
    }

    private Player aPlayer(final BigDecimal playerId) {
        return new Player(playerId, "nameOf" + playerId,
                playerId.add(BigDecimal.valueOf(10000)), "aPicture", null, null, null);
    }
}
