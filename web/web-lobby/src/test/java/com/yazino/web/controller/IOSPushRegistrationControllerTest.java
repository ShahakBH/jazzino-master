package com.yazino.web.controller;


import com.yazino.mobile.yaps.config.TypedMapBean;
import com.yazino.mobile.yaps.message.PlayerDevice;
import com.yazino.platform.messaging.publisher.SafeQueuePublishingEventService;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static java.math.BigDecimal.TEN;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class IOSPushRegistrationControllerTest {

    @Mock
    private SafeQueuePublishingEventService<PlayerDevice> playerDeviceQueuePublishingService;
    @Mock
    private HttpServletResponse response;
    private Map<String, String> bundleMappings = new HashMap<String, String>();
    private IOSPushRegistrationController controller;

    @Before
    public void setup() {

        bundleMappings.put("com.foo.Foo", "FOO");
        bundleMappings.put("com.yazino.YazinoApp", "SLOTS");
        bundleMappings.put("com.yazino.Blackjack", "BLACKJACK");
        bundleMappings.put("yazino.WheelDeal", "SLOTS");
        bundleMappings.put("yazino.Blackjack", "BLACKJACK");
        bundleMappings.put("yazino.HighStakes", "HIGH_STAKES");

        controller = new IOSPushRegistrationController(playerDeviceQueuePublishingService, new TypedMapBean<String, String>(bundleMappings));
    }

    @Test
    public void registerDeviceShouldCleanDeviceToken(){
        String brokenToken= "<2c43e410 cda4889f 6199f057 9204593a 676c2acc 988f6f3c da71676e 1eb62cce>";
        String cleanToken="2c43e410cda4889f6199f0579204593a676c2acc988f6f3cda71676e1eb62cce";
        controller.registerDevice("yazino.WheelDeal", TEN,brokenToken,response);
        verify(playerDeviceQueuePublishingService).send(new PlayerDevice("SLOTS", TEN, cleanToken, "yazino.WheelDeal"));
    }

    @Test
    public void shouldRegisterDeviceWithAGLBundleWhenUsingLegacyURLAndSlots() throws Exception {
        String gameType = "SLOTS";
        BigDecimal playerId = TEN;
        String deviceToken = "122344554454";
        controller.registerDeviceForPush(gameType, playerId, deviceToken, response);
        verify(playerDeviceQueuePublishingService).send(new PlayerDevice(gameType, playerId, deviceToken, "com.yazino.YazinoApp"));
    }

    @Test
    public void shouldNotRegisterDeviceWithAGLBundleWhenUsingLegacyURLAndBlackjack() throws Exception {
        String gameType = "BLACKJACK";
        BigDecimal playerId = TEN;
        String deviceToken = "122344554454";
        controller.registerDeviceForPush(gameType, playerId, deviceToken, response);
        verifyZeroInteractions(playerDeviceQueuePublishingService);
    }

    @Test
    public void shouldRegisterDeviceWithYazinoBundleWhenUsingLegacyURLAndHighstakes() throws Exception {
        String gameType = "HIGH_STAKES";
        BigDecimal playerId = TEN;
        String deviceToken = "122344554454";
        controller.registerDeviceForPush(gameType, playerId, deviceToken, response);
        verify(playerDeviceQueuePublishingService).send(new PlayerDevice(gameType, playerId, deviceToken, "yazino.HighStakes"));
    }

    @Test
    public void shouldRegisterDeviceWhenAGLWheelDealBundle() throws Exception {
        BigDecimal playerId = TEN;
        String bundle = "com.yazino.YazinoApp";
        String deviceToken = "122334566544";
        controller.registerDevice(bundle, playerId, deviceToken, response);
        verify(playerDeviceQueuePublishingService).send(new PlayerDevice("SLOTS", playerId, deviceToken, bundle));
    }

    @Test
    public void shouldRegisterDeviceWhenYazinoWheelDealBundle() throws Exception {
        BigDecimal playerId = TEN;
        String bundle = "yazino.WheelDeal";
        String deviceToken = "122334566544";
        controller.registerDevice(bundle, playerId, deviceToken, response);
        verify(playerDeviceQueuePublishingService).send(new PlayerDevice("SLOTS", playerId, deviceToken, bundle));
    }

    @Test
    public void shouldNotRegisterDeviceWhenAGLBlackjackBundle() throws Exception {
        BigDecimal playerId = TEN;
        String bundle = "com.yazino.Blackjack";
        String deviceToken = "122334566544";
        controller.registerDevice(bundle, playerId, deviceToken, response);
        verifyZeroInteractions(playerDeviceQueuePublishingService);
    }

    @Test
    public void shouldRegisterDeviceWhenYazinoBlackjackBundle() throws Exception {
        BigDecimal playerId = TEN;
        String bundle = "yazino.Blackjack";
        String deviceToken = "122334566544";
        controller.registerDevice(bundle, playerId, deviceToken, response);
        verify(playerDeviceQueuePublishingService).send(new PlayerDevice("BLACKJACK", playerId, deviceToken, bundle));
    }

    @Test
    public void shouldRegisterDeviceWhenHighStakesBundle() throws Exception {
        BigDecimal playerId = TEN;
        String bundle = "yazino.HighStakes";
        String deviceToken = "122334566544";
        controller.registerDevice(bundle, playerId, deviceToken, response);
        verify(playerDeviceQueuePublishingService).send(new PlayerDevice("HIGH_STAKES", playerId, deviceToken, bundle));
    }

}
