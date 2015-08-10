package com.yazino.web.service;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Platform;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.util.Environment;
import com.yazino.web.util.MessagingHostResolver;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClientPropertyServiceTest {

    private static final Platform PLATFORM_ANDROID = Platform.ANDROID;
    private static final Platform PLATFORM_WEB = Platform.WEB;
    private static final Platform PLATFORM_FACEBOOK_CANVAS = Platform.FACEBOOK_CANVAS;
    private static final String GAME_TYPE = "SLOTS";
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String CONTENT_URL = "sample content url";
    private static final String COMMAND_URL = "sample/command/url";
    private static final String CLIENT_URL = "sample client url";
    private static final String SERVER_URL = "sample/server/url/with/no/slash";
    private static final String PERMANENT_CONTENT_URL = "sample permanent content url";
    private static final String EXPECTED_CONTENT_URL = CONTENT_URL + "/";
    private static final String EXPECTED_COMMAND_URL = COMMAND_URL + "/";
    private static final String EXPECTED_CLIENT_URL = CLIENT_URL + "/";
    private static final String EXPECTED_PERMANENT_CONTENT_URL = PERMANENT_CONTENT_URL + "/";
    private static final String EXPECTED_SERVER_URL = SERVER_URL + "/";
    private static final String EXPECTED_TERMS_OF_SERVICE_URL = "some tos url";
    private static final String AMQP_HOST = "sample ampq host";
    private static final String AMQP_VIRTUAL_HOST = "sample amqp virtual host";
    private static final String AMQP_PORT = "8977";
    private static final String LIGHTSTREAMER_PROTOCOL = "sample ls protocol";
    private static final String LIGHTSTREAMER_SERVER = "sample ls server";
    private static final String LIGHTSTREAMER_PORT = "7868";
    private static final String LIGHTSTREAMER_ADAPTER_SET = "sample adapter set";
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(37);
    private static final String PLAYER_NAME = "ron harris";
    private static final String EXPECTED_GCM_PROJECT = "gcm-123";
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);
    private static final LobbySession LOBBY_SESSION = new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, null, null, null, null, null, true, null, AuthProvider.YAZINO);
    private static final String GUEST_PLAY_ENABLED = "false";
    private static final String ENABLED_GIFT_TYPES = "TYPE_1,TYPE_2";
    private static final String GIFT_COLLECTION_ROLLOVER_TIME = "05:00:00";

    private YazinoConfiguration yazinoConfiguration = mock(YazinoConfiguration.class);
    private MessagingHostResolver messagingHostResolver = mock(MessagingHostResolver.class);
    private GameAvailabilityService gameAvailabilityService = mock(GameAvailabilityService.class);
    private PlayerProfileService playerProfileService = mock(PlayerProfileService.class);
    private Environment environment = mock(Environment.class);

    private ClientPropertyService underTest = new ClientPropertyService(yazinoConfiguration, messagingHostResolver, gameAvailabilityService, playerProfileService, environment);

    @Before
    public void setUpCommonProperties() {
        when(environment.isDevelopment()).thenReturn(true);

        when(yazinoConfiguration.getString("terms-of-service.url")).thenReturn(EXPECTED_TERMS_OF_SERVICE_URL);
        when(yazinoConfiguration.getString("senet.web.command")).thenReturn(EXPECTED_COMMAND_URL);
        when(yazinoConfiguration.getString("senet.web.content")).thenReturn(EXPECTED_CONTENT_URL);
        when(yazinoConfiguration.getString("senet.web.permanent-content")).thenReturn(EXPECTED_PERMANENT_CONTENT_URL);
        when(yazinoConfiguration.getString("senet.web.application-content")).thenReturn(EXPECTED_CLIENT_URL);
        when(yazinoConfiguration.getString("senet.web.host")).thenReturn(EXPECTED_SERVER_URL);
        when(yazinoConfiguration.getString("google-cloud-messaging.project-id")).thenReturn(EXPECTED_GCM_PROJECT);
        when(yazinoConfiguration.getString("lightstreamer.protocol")).thenReturn(LIGHTSTREAMER_PROTOCOL);
        when(yazinoConfiguration.getString("lightstreamer.server")).thenReturn(LIGHTSTREAMER_SERVER);
        when(yazinoConfiguration.getString("lightstreamer.port")).thenReturn(LIGHTSTREAMER_PORT);
        when(yazinoConfiguration.getString("lightstreamer.adapter-set")).thenReturn(LIGHTSTREAMER_ADAPTER_SET);
        when(yazinoConfiguration.getString("guest-play.enabled")).thenReturn(GUEST_PLAY_ENABLED);
        when(yazinoConfiguration.getString("canvas")).thenReturn("true");
        when(yazinoConfiguration.getString("facebookCanvasActionsAllowed")).thenReturn("true");
        when(yazinoConfiguration.getString("gifting.enabled-types")).thenReturn(ENABLED_GIFT_TYPES);
        when(yazinoConfiguration.getString("gifting.collection-rollover-time")).thenReturn(GIFT_COLLECTION_ROLLOVER_TIME);
        when(yazinoConfiguration.getString("canvas")).thenReturn("true");
        when(yazinoConfiguration.getString("facebookCanvasActionsAllowed")).thenReturn("true");
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(new PlayerProfile());
    }

    @Test
    public void getPropertiesShouldSetLightstreamerPort() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("lightstreamer-port"), equalTo(LIGHTSTREAMER_PORT));
    }
    @Test
    public void getPropertiesShouldSetLightstreamerServer() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("lightstreamer-server"), equalTo(LIGHTSTREAMER_SERVER));
    }
    @Test
    public void getPropertiesShouldSetLightstreamerAdapterSet() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("lightstreamer-adapter-set"), equalTo(LIGHTSTREAMER_ADAPTER_SET));
    }
    @Test
    public void getPropertiesShouldSetLightstreamerProtocol() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("lightstreamer-protocol"), equalTo(LIGHTSTREAMER_PROTOCOL));
    }

    @Test
    public void getPropertiesShouldSetTermsOfServiceUrl() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("terms-of-service-url"), equalTo(EXPECTED_TERMS_OF_SERVICE_URL));
    }

    @Test
    public void getPropertiesShouldSeServerUrl() {
        when(yazinoConfiguration.getString("senet.web.host")).thenReturn(SERVER_URL);

        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("server-url"), equalTo(EXPECTED_SERVER_URL));
    }

    @Test
    public void getPropertiesShouldSetServerUrlWithEndingSlash() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("server-url"), equalTo(EXPECTED_SERVER_URL));
    }

    @Test
    public void getPropertiesShouldSetCommandUrl() {
        when(yazinoConfiguration.getString("senet.web.command")).thenReturn(COMMAND_URL);

        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("command-url"), equalTo(EXPECTED_COMMAND_URL));
    }

    @Test
    public void getPropertiesShouldSetCommandUrlWithEndingSlash() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("command-url"), equalTo(EXPECTED_COMMAND_URL));
    }

    @Test
    public void getPropertiesShouldSetClientUrl() {
        when(yazinoConfiguration.getString("senet.web.application-content")).thenReturn(CLIENT_URL);

        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("client-url"), equalTo(EXPECTED_CLIENT_URL));
    }

    @Test
    public void getPropertiesShouldSetClientUrlWithEndingSlash() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("client-url"), equalTo(EXPECTED_CLIENT_URL));
    }

    @Test
    public void getPropertiesShouldSetContentUrl() {
        when(yazinoConfiguration.getString("senet.web.content")).thenReturn(CONTENT_URL);

        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("content-url"), equalTo(EXPECTED_CONTENT_URL));
    }

    @Test
    public void getPropertiesShouldSetContentUrlWithEndingSlash() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("content-url"), equalTo(EXPECTED_CONTENT_URL));
    }

    @Test
    public void getPropertiesShouldSetPermanentContentUrl() {
        when(yazinoConfiguration.getString("senet.web.permanent-content")).thenReturn(PERMANENT_CONTENT_URL);

        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("permanent-content-url"), equalTo(EXPECTED_PERMANENT_CONTENT_URL));
    }

    @Test
    public void getPropertiesShouldSetPermanentContentUrlWithEndingSlash() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("permanent-content-url"), equalTo(EXPECTED_PERMANENT_CONTENT_URL));
    }

    @Test
    public void getPropertiesForAndroidShouldReturnGCMProjectId(){
        Map<String,Object> properties = underTest.getBasePropertiesFor(Platform.ANDROID);
        assertThat((String)properties.get("gcm-project-id"), equalTo(EXPECTED_GCM_PROJECT));
    }

    @Test
    public void getPropertiesForWebShouldNotReturnGCMProjectId(){
        Map<String,Object> properties = underTest.getBasePropertiesFor(Platform.WEB);
        assertThat((String)properties.get("gcm-project-id"), equalTo(null));
    }

    @Test(expected = NullPointerException.class)
    public void getSessionPropertiesShouldThrowWhenSessionIsNull() {
        underTest.getSessionPropertiesFor(null);
    }

    @Test
    public void getSessionPropertiesShouldSetAmqpHostIfSessionExists() {
        when(messagingHostResolver.resolveMessagingHostForPlayer(PLAYER_ID)).thenReturn(AMQP_HOST);

        Map<String,Object> properties = underTest.getSessionPropertiesFor(LOBBY_SESSION);

        assertThat((String)properties.get("amqp-host"), equalTo(AMQP_HOST));
    }

    @Test
    public void getSessionPropertiesShouldSetAmqpVirtualHostIfSessionExists() {
        when(yazinoConfiguration.getString("strata.rabbitmq.virtualhost")).thenReturn(AMQP_VIRTUAL_HOST);

        Map<String,Object> properties = underTest.getSessionPropertiesFor(LOBBY_SESSION);

        assertThat((String)properties.get("amqp-virtual-host"), equalTo(AMQP_VIRTUAL_HOST));
    }

    @Test
    public void getSessionPropertiesShouldSetAmqpPortIfSessionExists() {
        when(yazinoConfiguration.getString("strata.rabbitmq.port")).thenReturn(AMQP_PORT);

        Map<String,Object> properties = underTest.getSessionPropertiesFor(LOBBY_SESSION);

        assertThat((String)properties.get("amqp-port"), equalTo(AMQP_PORT));
    }

    @Test
    public void getSessionPropertiesShouldSetPlayerIdIfSessionExists() {
        Map<String,Object> properties = underTest.getSessionPropertiesFor(LOBBY_SESSION);

        assertThat((String)properties.get("player-id"), equalTo(PLAYER_ID.toPlainString()));
    }

    @Test
    public void getSessionPropertiesShouldSetPlayerNameIfSessionExists() {
        Map<String,Object> properties = underTest.getSessionPropertiesFor(LOBBY_SESSION);

        assertThat((String)properties.get("player-name"), equalTo(PLAYER_NAME));
    }

    @Test
    public void getSessionPropertiesShouldSetGuestFlag() {
        PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setGuestStatus(GuestStatus.GUEST);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        Map<String,Object> properties = underTest.getSessionPropertiesFor(LOBBY_SESSION);

        assertThat((String)properties.get("guest"), equalTo("true"));
    }

    @Test
    public void getPropertiesShouldSetFacebookCanvasFlag() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(Platform.ANDROID);

        assertThat((String)properties.get("facebook-canvas"), equalTo("true"));
    }

    @Test(expected = IllegalStateException.class)
    public void getVersionsForShouldThrowAnExceptionForNonDevelopmentEnvironmentsWhereThePropertyIsNotConfigured() {
        when(environment.isDevelopment()).thenReturn(false);

        underTest.getVersionsFor(PLATFORM_ANDROID, GAME_TYPE, CLIENT_ID);
    }

    @Test
    public void getVersionsForShouldNotThrowAnExceptionForADevelopmentEnvironmentsWhereThePropertyIsNotConfigured() {
        underTest.getVersionsFor(PLATFORM_ANDROID, GAME_TYPE, CLIENT_ID);
    }

    @Test
    public void getVersionsForShouldSetMinimumVersionIfConfigured() {
        String expectedMinimumVersion = "1";
        configureVersionProperty("minimum", expectedMinimumVersion, PLATFORM_ANDROID);
        Map<String,Object> properties = underTest.getVersionsFor(PLATFORM_ANDROID, GAME_TYPE, CLIENT_ID);

        assertThat((String)properties.get("minimum-version"), equalTo(expectedMinimumVersion));
    }

    @Test
    public void getVersionsForShouldNotSetMinimumVersionIfNull() {
        configureVersionProperty("minimum", null, PLATFORM_ANDROID);
        Map<String,Object> properties = underTest.getVersionsFor(PLATFORM_ANDROID, GAME_TYPE, CLIENT_ID);

        assertThat((String)properties.get("minimum-version"), equalTo(null));
    }

    @Test
    public void getVersionsForShouldNotSetMinimumVersionIfBlank() {
        configureVersionProperty("minimum", " ", PLATFORM_ANDROID);
        Map<String,Object> properties = underTest.getVersionsFor(PLATFORM_ANDROID, GAME_TYPE, CLIENT_ID);

        assertThat((String)properties.get("minimum-version"), equalTo(null));
    }

    @Test
    public void getVersionsForShouldSetLatestVersionIfConfigured() {
        String expectedLatestVersion = "1";
        configureVersionProperty("latest", expectedLatestVersion, PLATFORM_ANDROID);
        Map<String,Object> properties = underTest.getVersionsFor(PLATFORM_ANDROID, GAME_TYPE, CLIENT_ID);

        assertThat((String)properties.get("latest-version"), equalTo(expectedLatestVersion));
    }

    @Test
    public void getVersionsForShouldNotSetLatestVersionIfNull() {
        configureVersionProperty("latest", null, PLATFORM_ANDROID);
        Map<String,Object> properties = underTest.getVersionsFor(PLATFORM_ANDROID, GAME_TYPE, CLIENT_ID);

        assertThat((String)properties.get("latest-version"), equalTo(null));
    }

    @Test
    public void getVersionsForShouldNotSetLatestVersionIfBlank() {
        configureVersionProperty("latest", " ", PLATFORM_ANDROID);
        Map<String,Object> properties = underTest.getVersionsFor(PLATFORM_ANDROID, GAME_TYPE, CLIENT_ID);

        assertThat((String)properties.get("latest-version"), equalTo(null));
    }

    @Test
    public void getVersionsForShouldIgnoreMinimumVersionForWEB() {
        configureVersionProperty("minimum", "won't be loaded", PLATFORM_WEB);
        Map<String,Object> properties = underTest.getVersionsFor(PLATFORM_WEB, GAME_TYPE, CLIENT_ID);

        assertThat((String)properties.get("minimum-version"), equalTo(null));
    }

    @Test
    public void getVersionsForShouldIgnoreLatestVersionForWEB() {
        configureVersionProperty("latest", "won't be loaded", PLATFORM_WEB);
        Map<String,Object> properties = underTest.getVersionsFor(PLATFORM_WEB, GAME_TYPE, CLIENT_ID);

        assertThat((String)properties.get("latest-version"), equalTo(null));
    }

    @Test
    public void getVersionsForShouldIgnoreMinimumVersionForFACEBOOK_CANVAS() {
        configureVersionProperty("minimum", "won't be loaded", PLATFORM_FACEBOOK_CANVAS);
        Map<String,Object> properties = underTest.getVersionsFor(PLATFORM_FACEBOOK_CANVAS, GAME_TYPE, CLIENT_ID);

        assertThat((String)properties.get("minimum-version"), equalTo(null));
    }

    @Test
    public void getVersionsForShouldIgnoreLatestVersionForFACEBOOK_CANVAS() {
        configureVersionProperty("latest", "won't be loaded", PLATFORM_FACEBOOK_CANVAS);
        Map<String,Object> properties = underTest.getVersionsFor(PLATFORM_FACEBOOK_CANVAS, GAME_TYPE, CLIENT_ID);

        assertThat((String)properties.get("latest-version"), equalTo(null));
    }

    @Test
    public void getAvailabilityOfGameTypeShouldSetAvailability() {
        GameAvailability value = new GameAvailability(GameAvailabilityService.Availability.AVAILABLE);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);

        Map<String,Object> properties = underTest.getAvailabilityOfGameType(GAME_TYPE);

        assertThat((String)properties.get("availability"), equalTo("AVAILABLE"));
    }

    @Test
    public void getAvailabilityOfGameTypeShouldSetCountdownIfMaintenanceScheduledForSpecifiedGame() {
        Long expectedCountdown = 10L;
        GameAvailability value = new GameAvailability(GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED, expectedCountdown);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);

        Map<String,Object> properties = underTest.getAvailabilityOfGameType(GAME_TYPE);

        assertThat((String)properties.get("maintenance-starts-at-millis"), equalTo(Long.toString(expectedCountdown)));
    }

    @Test
    public void getAvailabilityOfGameTypeShouldNotSetCountdownIfMaintenanceNotScheduledForSpecifiedGame() {
        GameAvailability value = new GameAvailability(GameAvailabilityService.Availability.AVAILABLE);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);

        Map<String,Object> properties = underTest.getAvailabilityOfGameType(GAME_TYPE);

        assertThat((String)properties.get("maintenance-starts-at-millis"), equalTo(null));
    }

    @Test
    public void getPropertiesShouldSetGuestPlayEnabled() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("guest-play-enabled"), equalTo(GUEST_PLAY_ENABLED));
    }

    @Test
    public void getPropertiesShouldSetEnabledGiftTypes() {
        Map<String,Object> properties = underTest.getBasePropertiesFor(PLATFORM_ANDROID);

        assertThat((String)properties.get("enabled-gift-types"), equalTo(ENABLED_GIFT_TYPES));
    }

    private void configureVersionProperty(String property, String expectedMinimumVersion, Platform platform) {
        when(yazinoConfiguration.getString("client." + platform + "." + GAME_TYPE + "." + CLIENT_ID + ".version." + property)).thenReturn(expectedMinimumVersion);
    }
}
