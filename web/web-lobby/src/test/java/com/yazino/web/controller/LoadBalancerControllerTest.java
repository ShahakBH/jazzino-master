package com.yazino.web.controller;

import com.yazino.platform.session.SessionService;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LoadBalancerControllerTest {

    @Mock
    private SessionService sessionService;
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private HttpServletResponse response;

    private LoadBalancerController underTest;

    @Before
    public void setUp() throws IOException {
        when(sessionService.countSessions(false)).thenReturn(120);

        if (loadBalancerFile().exists() && !loadBalancerFile().delete()) {
            throw new IllegalStateException("Could not delete old test file: " + loadBalancerFile());
        }

        underTest = new LoadBalancerController(sessionService, webApiResponses, loadBalancerFile());
    }

    @Test
    public void theStatusIsReturnedAsOkayWhenEverythingIsGood() throws IOException {
        underTest.checkStatus(response);

        verify(webApiResponses).writeOk(response, singletonMap("status", "okay"));
    }

    @Test
    public void theStatusIsReturnedAsSuspendedWhenTheSuspensionFileExists() throws IOException {
        if (!loadBalancerFile().createNewFile()) {
            throw new IllegalStateException("Couldn't create test file: " + loadBalancerFile());
        }
        loadBalancerFile().deleteOnExit();

        underTest.checkStatus(response);

        verify(webApiResponses).writeOk(response, singletonMap("status", "suspended"));
    }

    @Test
    public void theStatusIsReturnedAsGridErrorWhenTheSpaceTestQueryFails() throws IOException {
        reset(sessionService);
        when(sessionService.countSessions(false)).thenThrow(new RuntimeException("aTestException"));

        underTest.checkStatus(response);

        verify(webApiResponses).writeOk(response, singletonMap("status", "grid-error"));
    }

    private File loadBalancerFile() {
        return new File(System.getProperty("java.io.tmpdir") + "/suspendFromLoadBalancer");
    }

}
