package com.yazino.platform;

import com.yazino.platform.invitation.InvitationQueryService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class InvitationQueryServiceIntegrationTest {

    private static final BigDecimal ISSUING_PLAYER_ID = new BigDecimal(-4201);

    @Autowired
    private InvitationQueryService invitationQueryService;

    private Server server;

    @Test
    public void serviceIsExposedForHttpRemoteInvocation() throws Exception {
        assertNotNull("Expected a set", invitationQueryService.findInvitationsByIssuingPlayer(ISSUING_PLAYER_ID));
    }

    @Before
    public void setUp() throws Exception {
        WebAppContext context = new WebAppContext();
        String webappSourceDirectory = bestGuessAtWebSourceDirectory();
        context.setResourceBase(webappSourceDirectory);
        context.setContextPath("/");
        context.setParentLoaderPriority(true);

        server = new Server(8080);
        server.setHandler(context);
        server.start();
        server.dump();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    private String bestGuessAtWebSourceDirectory() {
        URL knownResource = this.getClass().getResource("InvitationQueryServiceIntegrationTest-context.xml");
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
