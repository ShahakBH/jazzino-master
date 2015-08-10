package com.yazino.yaps;

import com.yazino.engagement.mobile.MobileDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlayerDeviceWorkerConfiguration {

    @Autowired
    private MobileDeviceService mobileDeviceDao;

    @Bean(name = "playerDeviceListener")
    public PlayerDeviceListener playerDeviceListener() {
        return new PlayerDeviceListener(mobileDeviceDao);
    }
}
