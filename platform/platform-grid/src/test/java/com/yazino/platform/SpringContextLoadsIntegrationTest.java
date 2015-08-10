package com.yazino.platform;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class SpringContextLoadsIntegrationTest {

    @Test
    public void testSpringContextLoadsCorrectly() {
        new ClassPathXmlApplicationContext("classpath:/META-INF/spring/pu.xml");
    }
}
