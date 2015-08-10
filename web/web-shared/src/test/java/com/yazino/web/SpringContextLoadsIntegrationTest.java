package com.yazino.web;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class SpringContextLoadsIntegrationTest {
    @Test
    public void testSpringContextLoadsCorrectly() {
        new ClassPathXmlApplicationContext("classpath:/com/yazino/web/SpringContextLoadsIntegrationTest-context.xml");
    }

}
