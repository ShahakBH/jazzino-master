package com.yazino.web.parature.service;

import com.yazino.platform.player.PlayerProfile;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ParatureSupportUserServiceExternalIntegrationTest {

    private static final String PARATURE_URL = "https://s5.parature.com/api/v1/15164/15201/Customer";

    // This token will expire, and you'll start getting 401s back from the server. To regenerate, see http://wiki.london.yazino.com/display/DEV/Parature
    private static final String PARATURE_TOKEN = "XxBl0lBCBsV/7fwjsU/F4gsscdOKZ4X5@7j7VB30@xQoOLGaRcnpf/yNY/5IcrPA7BGyWPvQxY8tDmlOoePrLA==";

    private CloseableHttpClient httpClient;

    private ParatureSupportUserService paratureSupportUserService;
    private BigDecimal playerId;

    @Before
    public void setup() {
        httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(5000).build())
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(5000).build())
                .build();

        playerId = new BigDecimal(System.currentTimeMillis());

        paratureSupportUserService = new ParatureSupportUserService(httpClient, PARATURE_URL, PARATURE_TOKEN);
    }

    @After
    public void deleteTestUser() throws IOException, SAXException, ParserConfigurationException {
        if (playerId != null) {
            final String paratureId = getParatureIdForPlayerId(playerId);
            if (paratureId != null) {
                deleteParatureUser(paratureId);
            }
        }
    }

    @Test
    public void aUserCanBeRegistered() throws IOException, SupportUserServiceException {
        final PlayerProfile playerProfile = aPlayerProfileWithId(playerId);

        paratureSupportUserService.createSupportUser(playerProfile.getPlayerId(), playerProfile);
    }

    @Test
    public void aUsersRegistrationStatusCanBeDetected() throws IOException, SupportUserServiceException {
        final PlayerProfile playerProfile = aPlayerProfileWithId(playerId);

        assertThat(paratureSupportUserService.hasUserRegistered(playerProfile.getPlayerId()), is(false));

        paratureSupportUserService.createSupportUser(playerProfile.getPlayerId(), playerProfile);

        assertThat(paratureSupportUserService.hasUserRegistered(playerProfile.getPlayerId()), is(true));
    }

    private PlayerProfile aPlayerProfileWithId(final BigDecimal playerId) {
        PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setFirstName("aFirstName");
        playerProfile.setLastName("aLastName");
        playerProfile.setEmailAddress("anEmail@yazino.com");
        playerProfile.setDisplayName("aDisplayName");
        playerProfile.setPlayerId(playerId);
        return playerProfile;
    }

    private void deleteParatureUser(final String paratureId) throws IOException {
        final HttpResponse deleteResponse = httpClient.execute(new HttpDelete(String.format("%s/%s?_token_=%s", PARATURE_URL, paratureId, PARATURE_TOKEN)));
        EntityUtils.consume(deleteResponse.getEntity());

        if (deleteResponse.getStatusLine().getStatusCode() == 204) {
            httpClient = HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(1000).build())
                    .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(1000).build())
                    .build();

            try {
                final HttpResponse purgeResponse = httpClient.execute(new HttpDelete(String.format("%s/%s?_purge_=true&_token_=%s", PARATURE_URL, paratureId, PARATURE_TOKEN)));
                EntityUtils.consume(purgeResponse.getEntity());
            } catch (SocketTimeoutException e) {
                // Parature seem to return a malformed header for the purge request, such as ï»¿<?xml version="1.0" encoding="utf-8"?>HTTP/1.1 204 No Content
                // As such, HttpClient correctly ignored it and waits for a proper response, hence timing out.
            }
        }
    }

    private String getParatureIdForPlayerId(final BigDecimal playerId) throws IOException, ParserConfigurationException, SAXException {
        final HttpGet query = new HttpGet(String.format("%s?User_Name=%s&_token_=%s", PARATURE_URL, playerId.toPlainString(), PARATURE_TOKEN));
        final HttpResponse queryResult = httpClient.execute(query);
        if (queryResult.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK) {
            EntityUtils.consume(queryResult.getEntity());
            throw new RuntimeException("Failed to cleanup test data; search failed with code " + queryResult.getStatusLine().getStatusCode());
        }

        InputStream responseStream = null;
        try {
            responseStream = queryResult.getEntity().getContent();
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document doc = db.parse(new InputSource(responseStream));

            final String total = doc.getDocumentElement().getAttribute("total");
            if (Integer.parseInt(total) > 0) {
                return doc.getDocumentElement().getElementsByTagName("Customer").item(0).getAttributes().getNamedItem("id").getNodeValue();
            }

        } finally {
            IOUtils.closeQuietly(responseStream);
        }

        return null;
    }

}
