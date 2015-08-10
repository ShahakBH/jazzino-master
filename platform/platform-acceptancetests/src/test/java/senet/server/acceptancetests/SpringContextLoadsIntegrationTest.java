package senet.server.acceptancetests;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.Assert.assertNotNull;


public class SpringContextLoadsIntegrationTest {

    @Test
    @DirtiesContext
    public void checkThatSpringContextLoads() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:spring.xml");
        assertNotNull(ctx);
    }
}
