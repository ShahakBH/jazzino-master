package com.yazino.web.controller.gameserver;

import org.junit.Before;
import org.junit.Test;
import com.yazino.web.data.PictureRepository;
import com.yazino.web.data.LevelRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.math.BigDecimal;

import static org.mockito.Mockito.*;

public class PlayerPictureAndLevelControllerTest {

    private static final String GAME_TYPE = "GAME_TYPE";
    private PlayerPictureAndLevelController underTest;
    private LevelRepository leveRepository;
    private PictureRepository pictureRepository;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private PrintWriter writer;

    @Before
    public void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);
        leveRepository = mock(LevelRepository.class);
        pictureRepository = mock(PictureRepository.class);
        underTest = new PlayerPictureAndLevelController();
        underTest.setLevelRepository(leveRepository);
        underTest.setPictureRepository(pictureRepository);

    }

    @Test
    public void shouldNotAllowEmptyGameType() throws Exception {
        when(request.getParameter("playerIds")).thenReturn("1,2");
        when(request.getParameter("gameType")).thenReturn("");
        underTest.handlePicture(request, response);
        verify(response).sendError(500, "gameType is required");
        verifyNoMoreInteractions(response);
    }

    @Test
    public void shouldNotAllowEmptyPlayerIds() throws Exception {
        when(request.getParameter("playerIds")).thenReturn("  ");
        when(request.getParameter("gameType")).thenReturn(GAME_TYPE);
        underTest.handlePicture(request, response);
        verify(response).sendError(500, "playerIds is required");
        verifyNoMoreInteractions(response);
    }

    @Test
    public void shouldNotAllowInvalidPlayerIds() throws Exception {
        when(request.getParameter("playerIds")).thenReturn("1,2,a");
        when(request.getParameter("gameType")).thenReturn(GAME_TYPE);
        underTest.handlePicture(request, response);
        verify(response).sendError(500, "invalid playerId: a");
    }

    @Test
    public void shouldSupportSpacesInPlayerIds() throws Exception {
        when(request.getParameter("playerIds")).thenReturn("1, 2");
        when(request.getParameter("gameType")).thenReturn(GAME_TYPE);
        underTest.handlePicture(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    public void shouldFetchPictureAndLevelForTwoPlayers() throws Exception {
        when(request.getParameter("playerIds")).thenReturn("1, 2");
        when(request.getParameter("gameType")).thenReturn(GAME_TYPE);
        when(leveRepository.getLevel(new BigDecimal("1"), GAME_TYPE)).thenReturn(11);
        when(pictureRepository.getPicture(new BigDecimal("1"))).thenReturn("pic1");
        when(leveRepository.getLevel(new BigDecimal("2"), GAME_TYPE)).thenReturn(12);
        when(pictureRepository.getPicture(new BigDecimal("2"))).thenReturn("pic2");
        underTest.handlePicture(request, response);
        verify(writer).write("[{\"picture\":\"pic1\",\"level\":11},{\"picture\":\"pic2\",\"level\":12}]");
    }
}
