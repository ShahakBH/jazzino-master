package com.yazino.web.api;

import com.yazino.game.api.GameType;
import com.yazino.platform.Platform;
import com.yazino.platform.invitation.InvitationService;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.data.GameTypeRepository;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.SpringErrorResponseFormatter;
import com.yazino.web.util.WebApiResponses;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.web.api.InvitationSentRecord.toInvitationSentRecord;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InvitationRecordingControllerTest {
    private static final HashMap<String, Object> FORMATTED_JSON = new HashMap<>();

    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private InvitationService invitationService;
    @Mock
    private GameTypeRepository gameTypeRepository;
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private SpringErrorResponseFormatter springErrorResponseFormatter;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private final DateTime invitationTime = new DateTime(2012, 12, 9, 9, 8, 10, 10);
    private final BigDecimal playerId = BigDecimal.TEN;
    private final BindingResult bindingResult = new MapBindingResult(new HashMap<>(), "record");
    private final Map<String, GameTypeInformation> gameTypes = new HashMap<>();

    private InvitationRecordingController underTest;

    @Before
    public void setup() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(invitationTime.getMillis());

        underTest = new InvitationRecordingController(
                lobbySessionCache, invitationService, gameTypeRepository, webApiResponses, springErrorResponseFormatter);

        when(gameTypeRepository.getGameTypes()).thenReturn(gameTypes);
        LobbySession session = mock(LobbySession.class);
        when(session.getPlayerId()).thenReturn(playerId);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);
        when(springErrorResponseFormatter.toJson(bindingResult)).thenReturn(FORMATTED_JSON);
    }

    @After
    public void resetJoda() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    private void addGameToRepository(String gameType) {
        gameTypes.put(gameType, new GameTypeInformation(new GameType(gameType, gameType, Collections.<String>emptySet()), true));
    }

    @Test
    public void shouldReturn404WhenInvalidSource() throws Exception {
        addGameToRepository("SLOTS");
        underTest.recordSentInvites(request, response, "Foo", toInvitationSentRecord("SLOTS", Platform.IOS.name(), "abc@123.com"), bindingResult);
        verify(response).setStatus(404);
    }

    @Test
    public void shouldReturn400WhenInvalidGame() throws Exception {
        underTest.recordSentInvites(request, response, InvitationSource.EMAIL.name(), toInvitationSentRecord("BAR", Platform.IOS.name(), "abc@123.com"), bindingResult);
        verify(webApiResponses).write(eq(response), eq(400), any(Map.class));
    }

    @Test
    public void shouldReturnFieldErrorWhenInvalidGame() throws Exception {
        underTest.recordSentInvites(request, response, InvitationSource.EMAIL.name(), toInvitationSentRecord("BAR", Platform.IOS.name(), "abc@123.com"), bindingResult);
        FieldError fieldError = bindingResult.getFieldError("gameType");
        assertEquals("gameType is not supported", fieldError.getDefaultMessage());
        assertEquals("unsupported", fieldError.getCode());
    }

    @Test
    public void shouldReturnErrorViewWhenInvalidGame() throws Exception {
        addGameToRepository("SLOTS");
        underTest.recordSentInvites(request, response, InvitationSource.EMAIL.name(), toInvitationSentRecord("BAR", Platform.IOS.name(), "abc@123.com"), bindingResult);

        verify(webApiResponses).write(eq(response), anyInt(), same(FORMATTED_JSON));
    }

    @Test
    public void shouldReturn400WhenInvalidPlatform() throws Exception {
        addGameToRepository("SLOTS");
        underTest.recordSentInvites(request, response, InvitationSource.EMAIL.name(), toInvitationSentRecord("SLOTS", "PSN", "abc@def.com"), bindingResult);
        verify(webApiResponses).write(eq(response), eq(400), any(Map.class));
    }

    @Test
    public void shouldReturnModelWithErrorWhenInvalidPlatform() throws Exception {
        addGameToRepository("SLOTS");
        underTest.recordSentInvites(request, response, InvitationSource.EMAIL.name(), toInvitationSentRecord("SLOTS", "PSN", "abc@def.com"), bindingResult);
        FieldError fieldError = bindingResult.getFieldError("platform");
        assertEquals("platform is not supported", fieldError.getDefaultMessage());
        assertEquals("unsupported", fieldError.getCode());
    }

    @Test
    public void shouldReturnErrorViewWhenInvalidPlatform() throws Exception {
        addGameToRepository("SLOTS");
        underTest.recordSentInvites(request, response, InvitationSource.EMAIL.name(), toInvitationSentRecord("SLOTS", "PSN", "abc@def.com"), bindingResult);
        verify(webApiResponses).write(eq(response), anyInt(), same(FORMATTED_JSON));
    }

    @Test
    public void shouldReturn400WhenEmptySourceIds() throws Exception {
        addGameToRepository("SLOTS");
        underTest.recordSentInvites(request, response, InvitationSource.EMAIL.name(), toInvitationSentRecord("SLOTS", Platform.IOS.name(), ""), bindingResult);
        verify(webApiResponses).write(eq(response), eq(400), any(Map.class));
    }

    @Test
    public void shouldReturn400WhenASourceIdIsBlank() throws Exception {
        addGameToRepository("SLOTS");
        underTest.recordSentInvites(request, response, InvitationSource.FACEBOOK.name(), toInvitationSentRecord("SLOTS", Platform.IOS.name(), "1000000099,,1002345"), bindingResult);
        verify(webApiResponses).write(eq(response), eq(400), any(Map.class));
    }

    @Test
    public void shouldReturnModelWithErrorWhenEmptySourceIds() throws Exception {
        addGameToRepository("SLOTS");
        underTest.recordSentInvites(request, response, InvitationSource.EMAIL.name(), toInvitationSentRecord("SLOTS", Platform.IOS.name(), ""), bindingResult);
        FieldError fieldError = bindingResult.getFieldError("sourceIds");
        assertEquals("sourceIds must be present", fieldError.getDefaultMessage());
        assertEquals("empty", fieldError.getCode());
    }

    @Test
    public void shouldReturnErrorViewWhenEmailInviteAndEmptyEmailAddresses() throws Exception {
        addGameToRepository("SLOTS");
        underTest.recordSentInvites(request, response, InvitationSource.EMAIL.name(), toInvitationSentRecord("SLOTS", Platform.IOS.name(), "   "), bindingResult);
        verify(webApiResponses).write(eq(response), anyInt(), same(FORMATTED_JSON));
    }

    @Test
    public void shouldReturn202WhenInvitationsHaveBeenRecorded() throws Exception {
        addGameToRepository("SLOTS");
        underTest.recordSentInvites(request, response, InvitationSource.EMAIL.name(), toInvitationSentRecord("SLOTS", Platform.IOS.name(), "a@b.com"), bindingResult);
        webApiResponses.writeNoContent(response, 202);
    }

    @Test
    public void shouldUseInvitationServiceToSendInvitations() throws Exception {
        addGameToRepository("SLOTS");
        underTest.recordSentInvites(request, response, InvitationSource.EMAIL.name(), toInvitationSentRecord("SLOTS", Platform.IOS.name(), "a@b.com,b@c.com"), bindingResult);
        verify(invitationService).invitationSent(eq(playerId), eq("a@b.com"), eq(InvitationSource.EMAIL), eq(invitationTime), eq("SLOTS"), eq(Platform.IOS.name()));
        verify(invitationService).invitationSent(eq(playerId), eq("b@c.com"), eq(InvitationSource.EMAIL), eq(invitationTime), eq("SLOTS"), eq(Platform.IOS.name()));
    }

    @Test
    public void shouldReturnSuccessViewWhenInvitationsHaveBeenSent() throws Exception {
        addGameToRepository("SLOTS");
        underTest.recordSentInvites(request, response, InvitationSource.EMAIL.name(), toInvitationSentRecord("SLOTS", Platform.IOS.name(), "a@b.com"), bindingResult);
        webApiResponses.writeNoContent(response, 202);
    }

}
