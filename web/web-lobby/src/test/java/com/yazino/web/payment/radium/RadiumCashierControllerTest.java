package com.yazino.web.payment.radium;


import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

public class RadiumCashierControllerTest {


    private RadiumCashierController underTest;
    private RadiumService radiumService= mock(RadiumService.class);
    private HttpServletResponse response=mock(HttpServletResponse.class);
    private HttpServletRequest request=mock(HttpServletRequest.class);
    private PrintWriter writer = mock(PrintWriter.class);

    @Before
    public void setUp() throws Exception {

        underTest=new RadiumCashierController(radiumService);
    }

    @Test
    public void successfulResultShouldSendOne() throws ServletException, IOException {

        when(response.getWriter()).thenReturn(writer);
        when(request.getRemoteAddr()).thenReturn("ip");
        when(radiumService.payoutChipsAndNotifyPlayer("chipAmount", "appId", "hash", "trackId", "userId", "pid", "ip"))
                .thenReturn(true);

        underTest.callback("chipAmount", "appId", "hash", "trackId", "userId", "pid", request,response);

        verify(response).setContentType("text/plain");
        verify(writer).write("1");
    }

    @Test
    public void failureResultShouldSendOne() throws ServletException, IOException {

        when(response.getWriter()).thenReturn(writer);
        when(request.getRemoteAddr()).thenReturn("ip");
        when(radiumService.payoutChipsAndNotifyPlayer("chipAmount", "appId", "hash", "trackId", "userId", "pid", "ip"))
                .thenReturn(false);

        underTest.callback("chipAmount", "appId", "hash", "trackId", "userId", "pid", request,response);

        verify(response).setContentType("text/plain");
        verify(writer).write("0");
    }
}
