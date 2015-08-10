package com.yazino.mobile.ws;

import com.yazino.mobile.ws.config.FacebookConfig;
import com.yazino.mobile.ws.ios.IOSConfig;
import com.yazino.mobile.ws.ios.IOSController;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;


public class SpringContextLoadsIntegrationTest {
    @Test
    @DirtiesContext
    public void testSpringContextLoadsCorrectly() throws IOException {
        final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:META-INF/spring/mobile-ws.xml");

        final IOSConfig iosConfig = (IOSConfig) context.getBean("iOSConfig");
        assertThat(iosConfig.getIdentifiers().size(), is(greaterThan(0)));

        final ModelAndView wheeldeal = ((IOSController) context.getBean("IOSController")).handleBootstrapRequest("2.0.1", "wheeldeal");
        assertThat(((FacebookConfig) wheeldeal.getModel().get("facebook")).getApplicationIds().size(), is(greaterThan(0)));

    }
}

