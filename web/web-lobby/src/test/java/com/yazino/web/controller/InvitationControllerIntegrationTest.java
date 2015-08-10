package com.yazino.web.controller;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class InvitationControllerIntegrationTest {

    @Value("${jetty.test.port}")
    private Integer port;

    private Server server;

    @Ignore
    @Test
    public void shouldRespondToInvitationStartUrl() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + port + "/invitations/start");

        HttpResponse response = client.execute(get);

        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
    }

    @Before
    public void setUp() throws Exception {
        ContextHandler context = new ContextHandler();
        context.setResourceBase(bestGuessAtWebSourceDirectory());
        context.setContextPath("/");

        server = new Server(port);
        server.setHandler(context);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    private String bestGuessAtWebSourceDirectory() {
        URL knownResource = this.getClass().getResource("InvitationControllerIntegrationTest-context.xml");
        String path = knownResource.getFile();
        String projectRoot = path.substring(0, path.lastIndexOf("target"));
        String webappSourceDirectory = projectRoot + "src/main/webapp";
        if (!new File(webappSourceDirectory, "WEB-INF/web.xml").exists()) {
            throw new RuntimeException("Unable to find webapp source directory.  Perhaps (1) the project structure " +
                    "has changed or (2) you are running this test outside of Maven?");
        }
        return webappSourceDirectory;
    }

}
