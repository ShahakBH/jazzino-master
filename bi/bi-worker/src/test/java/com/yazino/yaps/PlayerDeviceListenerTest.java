package com.yazino.yaps;

import com.yazino.engagement.mobile.MobileDeviceService;
import com.yazino.mobile.yaps.message.PlayerDevice;
import com.yazino.platform.Platform;
import org.junit.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

public class PlayerDeviceListenerTest {

    private final MobileDeviceService mobileDeviceDao = mock(MobileDeviceService.class);
    private final PlayerDeviceListener listener = new PlayerDeviceListener(mobileDeviceDao);

    @Test
    public void shouldPersistThePlayerDeviceIfItIsValid() throws Exception {
        PlayerDevice playerDevice = new PlayerDevice("SLOTS", BigDecimal.ONE, "XXX-666-EEE", "com.yazino.YazinoApp");
        listener.handle(playerDevice);
        verify(mobileDeviceDao).register(BigDecimal.ONE, "SLOTS", Platform.IOS, "com.yazino.YazinoApp", null, "XXX-666-EEE");
    }

    @Test
    public void shouldNotAllowExceptionsToProprogate() throws Exception {
        PlayerDevice playerDevice = new PlayerDevice("SLOTS", BigDecimal.ONE, "XXX-666-EEE", "com.yazino.YazinoApp");
        doThrow(new RuntimeException("oops")).when(mobileDeviceDao).register(BigDecimal.ONE, "SLOTS", Platform.IOS, "com.yazino.YazinoApp", null, "XXX-666-EEE");
        listener.handle(playerDevice);
    }

}
