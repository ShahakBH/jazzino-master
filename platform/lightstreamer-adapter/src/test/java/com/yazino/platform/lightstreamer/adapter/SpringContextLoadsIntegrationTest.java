package com.yazino.platform.lightstreamer.adapter;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertNotNull;

public class SpringContextLoadsIntegrationTest {

    @Test
    public void testSpringContextLoadsCorrectly() {
        ApplicationContext ctx=new ClassPathXmlApplicationContext("classpath:ls-spring.xml");
        assertNotNull(ctx);
    }

}
