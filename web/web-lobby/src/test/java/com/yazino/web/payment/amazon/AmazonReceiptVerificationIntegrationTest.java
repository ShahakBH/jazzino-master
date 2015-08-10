package com.yazino.web.payment.amazon;

import com.yazino.configuration.YazinoConfiguration;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.stub;

/**
 * Based on
 * Receipt Validation Service Docs - https://developer.amazon.com/sdk/in-app-purchasing/documentation/rvs.html
 * Testing In-App Purchases - https://developer.amazon.com/sdk/in-app-purchasing/documentation/testing-iap.html
 */
public class AmazonReceiptVerificationIntegrationTest {

    private static final int PORT = 8123;
    private static final String validToken = "eyJ0eXBlIjoiQ09OU1VNQUJMRSIsInNrdSI6Im15X3NwZWxsIn0";
    private static final String invalidToken = "eyJ0eXBlIjoiQ09OU1VNQUJMRSIsInNrdSI6Im15X3NwZW";
    private static Server server;
    private AmazonReceiptVerificationService underTest;

    @BeforeClass
    public static void startServer() throws Exception {
        server = new Server(PORT);
        final WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(AmazonReceiptVerificationIntegrationTest.class.getResource("/RVSSandbox.war").toString());
        server.setHandler(webapp);
        server.setStopAtShutdown(true);
        server.start();
    }

    @Before
    public void setUp() {
        YazinoConfiguration configuration = Mockito.mock(YazinoConfiguration.class);
        stub(configuration.getBoolean("amazon.rvs.enabled")).toReturn(true);
        stub(configuration.getString("amazon.rvs.host")).toReturn("http://localhost:8123");
        stub(configuration.getString("amazon.rvs.developerSecret")).toReturn("yazinoDeveloperSecret");
        underTest = new AmazonReceiptVerificationService(configuration, HttpClientBuilder.create().build());
    }

    @Test
    public void testValidToken() throws IOException {
        assertEquals(VerificationResult.VALID, underTest.verify("userId", validToken));
    }

    @Test
    public void testInvalidToken() throws IOException {
        assertEquals(VerificationResult.INVALID_TOKEN, underTest.verify("userId", invalidToken));
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stop();
    }

}
