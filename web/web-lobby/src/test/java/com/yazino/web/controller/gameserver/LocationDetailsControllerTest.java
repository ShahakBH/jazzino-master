package com.yazino.web.controller.gameserver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import com.yazino.web.domain.LocationDetails;
import com.yazino.web.data.LocationDetailsRepository;
import com.yazino.web.util.JsonHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocationDetailsControllerTest {

    private LocationDetailsRepository locationDetailsRepository;
    private LocationDetailsController underTest;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private JsonHelper json = new JsonHelper();

    @Before
    public void setUp(){
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        locationDetailsRepository = mock(LocationDetailsRepository.class);
        underTest = new LocationDetailsController(locationDetailsRepository);
    }

    @Test
    public void shouldNotAllowInvalidLocationId() throws IOException {
        underTest.locationDetails(request, response);
        verify(response).sendError(eq(500), anyString());
    }

    @Test
    public void shouldReturnLocationDetails() throws IOException {
        LocationDetails details = new LocationDetails(BigDecimal.TEN, "aLocation", "TEXAS_HOLDEM");
        when(request.getParameter("locationId")).thenReturn("10");
        when(locationDetailsRepository.getLocationDetails(BigDecimal.TEN)).thenReturn(details);
        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);
        underTest.locationDetails(request,response);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer).write(captor.capture());
        Assert.assertEquals(json.serialize(details), captor.getValue());
    }
}
