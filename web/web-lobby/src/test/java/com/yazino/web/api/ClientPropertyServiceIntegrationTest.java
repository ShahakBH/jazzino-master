package com.yazino.web.api;

import com.yazino.configuration.YazinoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)

@ContextConfiguration("classpath:META-INF/yazino-properties.xml")
public class ClientPropertyServiceIntegrationTest {

    @Autowired
    private YazinoConfiguration configuration;

    @Test
    public void configurationShouldProvidePropertiesUsedByService() throws IOException {
        assertNotNull(configuration.getString("senet.web.application-content"));
        assertNotNull(configuration.getString("senet.web.content"));
        assertNotNull(configuration.getString("senet.web.permanent-content"));
        assertNotNull(configuration.getString("strata.rabbitmq.port"));
        assertNotNull(configuration.getString("strata.rabbitmq.virtualhost"));
        assertNotNull(configuration.getString("senet.web.command"));
        assertNotNull(configuration.getString("terms-of-service.url"));
        assertNotNull(configuration.getString("lightstreamer.protocol"));
        assertNotNull(configuration.getString("lightstreamer.server"));
        assertNotNull(configuration.getString("lightstreamer.port"));
        assertNotNull(configuration.getString("lightstreamer.adapter-set"));
        assertNotNull(configuration.getString("guest-play.enabled"));
    }
}
