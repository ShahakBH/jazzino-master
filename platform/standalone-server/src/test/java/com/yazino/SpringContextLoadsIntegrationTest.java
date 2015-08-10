package com.yazino;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;


public class SpringContextLoadsIntegrationTest {
    @Test
    @DirtiesContext
    public void testSpringContextLoadsCorrectly() {
        new ClassPathXmlApplicationContext("classpath:standalone-server-springmvc.xml");
    }
}
