package com.yazino.platform;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

public class SpringContextLoadsIntegrationTest {

    @Test
    @DirtiesContext
    public void testSpringContextLoadsCorrectly() {
        new ClassPathXmlApplicationContext("classpath:/META-INF/spring/context.xml");
    }

}
