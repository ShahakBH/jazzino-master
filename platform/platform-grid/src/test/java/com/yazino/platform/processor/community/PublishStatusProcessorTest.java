package com.yazino.platform.processor.community;

import com.yazino.platform.gifting.PlayerCollectionStatus;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.model.community.*;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.TableInviteRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.util.JsonHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PublishStatusProcessorTest {

    public static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private DocumentDispatcher documentDispatcher = mock(DocumentDispatcher.class);
    private PlayerRepository playerRepository = mock(PlayerRepository.class);
    private PlayerSessionRepository playerSessionRepository = mock(PlayerSessionRepository.class);
    private InternalWalletService internalWalletService = mock(InternalWalletService.class);
    private GigaSpace communityGigaSpace = mock(GigaSpace.class);
    private TableInviteRepository tableInviteRepository = mock(TableInviteRepository.class);
    private Player player = mock(Player.class);
    private PublishStatusProcessor underTest;

    @Before
    public void setUp() {
        underTest = new PublishStatusProcessor(documentDispatcher, playerRepository, playerSessionRepository, internalWalletService, communityGigaSpace, tableInviteRepository);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);
        when(player.getPlayerId()).thenReturn(PLAYER_ID);
        when(playerSessionRepository.isOnline(PLAYER_ID)).thenReturn(true);
    }

    @Test
    public void processRequestShouldWhenRequestTypeIsGIFTING_PLAYER_COLLECTION_STATUS() {
        Map<String, Object> arguments = new HashMap<>();
        final PlayerCollectionStatus expectedPlayerCollectionStatus = new PlayerCollectionStatus(20, 10);
        arguments.put(PublishStatusRequestArgument.PLAYER_COLLECTION_STATUS.name(), expectedPlayerCollectionStatus);
        final PublishStatusRequest request = new PublishStatusRequestWithArguments(PLAYER_ID, PublishStatusRequestType.GIFTING_PLAYER_COLLECTION_STATUS, arguments);

        underTest.processRequest(request);

        ArgumentCaptor<Document> document = ArgumentCaptor.forClass(Document.class);
        verify(documentDispatcher).dispatch(document.capture(), eq(PLAYER_ID));
        assertThat(document.getValue().getType(), equalTo(DocumentType.GIFTING_PLAYER_COLLECTION_STATUS.getName()));

        Map map = new JsonHelper().deserialize(Map.class, document.getValue().getBody());

        assertNotNull(map.get("PLAYER_COLLECTION_STATUS"));
        assertThat((Integer) ((Map) map.get("PLAYER_COLLECTION_STATUS")).get("collectionsRemainingForCurrentPeriod"), equalTo(expectedPlayerCollectionStatus.getCollectionsRemainingForCurrentPeriod()));
        assertThat((Integer) ((Map) map.get("PLAYER_COLLECTION_STATUS")).get("giftsWaitingToBeCollected"), equalTo(expectedPlayerCollectionStatus.getGiftsWaitingToBeCollected()));
    }
}
