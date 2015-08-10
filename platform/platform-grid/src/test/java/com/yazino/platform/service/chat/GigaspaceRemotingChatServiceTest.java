package com.yazino.platform.service.chat;

import com.yazino.platform.chat.ChatRequestArgument;
import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.model.chat.ChatParticipant;
import com.yazino.platform.model.chat.GigaspaceChatRequest;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.repository.chat.ChatRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceRemotingChatServiceTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);
    private static final BigDecimal OTHER_PLAYER_ID = BigDecimal.valueOf(200);
    private static final String CHANNEL_ID = "aChannelId";
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(132);

    @Mock
    private ChatRepository chatGlobalRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private Player player;

    private GigaspaceRemotingChatService underTest;

    @Before
    public void setUp() {
        underTest = new GigaspaceRemotingChatService(chatGlobalRepository, playerRepository);

        when(player.getPlayerId()).thenReturn(PLAYER_ID);
        when(player.getName()).thenReturn("aPlayer");
        when(player.getPictureUrl()).thenReturn("aPictureUrl");
        when(player.getAccountId()).thenReturn(ACCOUNT_ID);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);
    }

    @Test(expected = NullPointerException.class)
    public void processingACommandWithANullPlayerIdThrowsAnIllegalArgumentException() {
        underTest.processCommand(null, "aCommand");
    }

    @Test(expected = NullPointerException.class)
    public void processingACommandWithANullCommandThrowsAnIllegalArgumentException() {
        underTest.processCommand(PLAYER_ID, (String[]) null);
    }

    @Test
    public void processingALeaveChannelCommandDelegatesItToTheChatRepository() {
        underTest.processCommand(PLAYER_ID, ChatRequestType.LEAVE_CHANNEL.toString(), "aChannelId", PLAYER_ID.toPlainString());

        verify(chatGlobalRepository).request(aChatRequestOfType(ChatRequestType.LEAVE_CHANNEL));
    }

    @Test
    public void processingAPublishChannelCommandDelegatesItToTheChatRepository() {
        underTest.processCommand(PLAYER_ID, ChatRequestType.PUBLISH_CHANNEL.toString(), "aChannelId", PLAYER_ID.toPlainString());

        verify(chatGlobalRepository).request(aChatRequestOfType(ChatRequestType.PUBLISH_CHANNEL));
    }

    @Test
    public void processingAnAddParticipantCommandDelegatesItToTheChatRepository() {
        when(player.getRelationshipTo(OTHER_PLAYER_ID)).thenReturn(new Relationship("aRelationship", RelationshipType.FRIEND));

        underTest.processCommand(PLAYER_ID, ChatRequestType.ADD_PARTICIPANT.toString(), "aChannelId", OTHER_PLAYER_ID.toPlainString());

        verify(chatGlobalRepository).request(aChatRequestOfType(ChatRequestType.ADD_PARTICIPANT));
    }

    @Test(expected = RuntimeException.class)
    public void processingAnAddParticipantCommandWhereNoRelationshipExistsThrowsARuntimeException() {
        underTest.processCommand(PLAYER_ID, ChatRequestType.ADD_PARTICIPANT.toString(), "aChannelId", OTHER_PLAYER_ID.toPlainString());
    }

    @Test(expected = RuntimeException.class)
    public void processingAnAddParticipantCommandWhereTheOtherParticipantIsNotAFriendThrowsARuntimeException() {
        when(player.getRelationshipTo(OTHER_PLAYER_ID)).thenReturn(new Relationship("aRelationship", RelationshipType.IGNORED));

        underTest.processCommand(PLAYER_ID, ChatRequestType.ADD_PARTICIPANT.toString(), "aChannelId", OTHER_PLAYER_ID.toPlainString());
    }

    @Test
    public void processingASendMessageCommandDelegatesItToTheChatRepository() {
        underTest.processCommand(PLAYER_ID, ChatRequestType.SEND_MESSAGE.toString(), "aChannelId", "aMessage");

        verify(chatGlobalRepository).request(aChatRequestOfType(ChatRequestType.SEND_MESSAGE));
    }

    @Test
    public void processingAPublishChannelsCommandDelegatesItToTheChatRepositoryForEachParticipant() {
        when(chatGlobalRepository.readParticipantsForSession(PLAYER_ID)).thenReturn(new ChatParticipant[]{participant(1), participant(2)});

        underTest.processCommand(PLAYER_ID, ChatRequestType.PUBLISH_CHANNELS.toString(), "aChannelId", PLAYER_ID.toPlainString());

        verify(chatGlobalRepository).request(aChatRequestForPlayerOfType(BigDecimal.valueOf(1), ChatRequestType.PUBLISH_CHANNEL));
        verify(chatGlobalRepository).request(aChatRequestForPlayerOfType(BigDecimal.valueOf(2), ChatRequestType.PUBLISH_CHANNEL));
    }

    @Test
    public void processingALeaveAllCommandDelegatesItToTheChatRepositoryForEachParticipant() {
        when(chatGlobalRepository.readParticipantsForSession(PLAYER_ID)).thenReturn(new ChatParticipant[]{participant(1), participant(2)});

        underTest.processCommand(PLAYER_ID, ChatRequestType.LEAVE_ALL.toString(), "aChannelId", PLAYER_ID.toPlainString());

        verify(chatGlobalRepository).request(aChatRequestForPlayerOfType(BigDecimal.valueOf(1), ChatRequestType.LEAVE_CHANNEL));
        verify(chatGlobalRepository).request(aChatRequestForPlayerOfType(BigDecimal.valueOf(2), ChatRequestType.LEAVE_CHANNEL));
    }

    @Test(expected = IllegalArgumentException.class)
    public void processingAnInvalidCommandThrowsAnIllegalArgumentException() {
        underTest.processCommand(PLAYER_ID, ChatRequestType.SEND_MESSAGE.toString());
    }

    private ChatParticipant participant(final int id) {
        return new ChatParticipant(BigDecimal.valueOf(id), CHANNEL_ID);
    }

    private GigaspaceChatRequest aChatRequestOfType(final ChatRequestType requestType) {
        return aChatRequestForPlayerOfType(PLAYER_ID, requestType);
    }

    private GigaspaceChatRequest aChatRequestForPlayerOfType(final BigDecimal playerId,
                                                             final ChatRequestType requestType) {
        final Map<ChatRequestArgument, String> requestParams = new HashMap<ChatRequestArgument, String>();

        switch (requestType) {
            case SEND_MESSAGE:
                requestParams.put(ChatRequestArgument.MESSAGE, "aMessage");
                break;
            case ADD_PARTICIPANT:
                requestParams.put(ChatRequestArgument.PLAYER_ID, OTHER_PLAYER_ID.toPlainString());
                requestParams.put(ChatRequestArgument.NICKNAME, "aRelationship");
                break;
            default:
                requestParams.put(ChatRequestArgument.PLAYER_ID, PLAYER_ID.toPlainString());
                break;
        }

        return new GigaspaceChatRequest(requestType, playerId, CHANNEL_ID, null, requestParams);
    }

}
