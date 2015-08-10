package com.yazino.web.controller.gameserver;

import org.junit.Before;
import org.junit.Test;
import com.yazino.web.domain.DefaultPicture;
import com.yazino.web.data.PictureRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

import static org.mockito.Mockito.*;

public class PlayerPictureControllerTest {

    private static final String FALLBACK_URL = "http://content/avatars/public/avatar0.png";
    private static final String SOME_PICTURE_URL = "picUrl1";
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private PlayerPictureController underTest;
    private PictureRepository pictureRepository;
    private HttpServletResponse response;
    private HttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        underTest = new PlayerPictureController();
        pictureRepository = mock(PictureRepository.class);
        response = mock(HttpServletResponse.class);
        request = mock(HttpServletRequest.class);
        underTest.setPictureRepository(pictureRepository);
        underTest.setDefaultPicture(new DefaultPicture("http://content/avatars/public/avatar0.png", null));
    }

    @Test
    public void getPictureUrlFromRepositoryUsingPlayerId() throws Exception {
        when(pictureRepository.getPicture(PLAYER_ID)).thenReturn(SOME_PICTURE_URL);
        when(request.getParameter("playerid")).thenReturn(PLAYER_ID.toString());
        underTest.handlePicture(request, response);
        verify(response).sendRedirect(SOME_PICTURE_URL);
    }

    @Test
    public void getPictureUrlFromRepositoryUsingAccountId() throws Exception {
        when(pictureRepository.getPicture(PLAYER_ID)).thenReturn(SOME_PICTURE_URL);
        when(request.getParameter("accountid")).thenReturn(PLAYER_ID.toString());
        underTest.handlePicture(request, response);
        verify(response).sendRedirect(SOME_PICTURE_URL);
    }

    @Test
    public void returnFallbackIfParameterNotProvided() throws Exception {
        underTest.handlePicture(request, response);
        verify(response).sendRedirect(FALLBACK_URL);
        verifyZeroInteractions(pictureRepository);
    }

    @Test
    public void returnFallbackIfParameterIsNotNumberic() throws Exception {
        when(request.getParameter("playerid")).thenReturn("null");

        underTest.handlePicture(request, response);

        verify(response).sendRedirect(FALLBACK_URL);
        verifyZeroInteractions(pictureRepository);
    }
}
