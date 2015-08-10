package strata.server.performance.rabbit;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Ensures spring loads correctly.
 */
public class SpringContextLoadsIntegrationTest {

    @Test
    public void springContextShouldLoadOk() {
        new ClassPathXmlApplicationContext("classpath:rabbit-replayer-spring.xml");
    }
}
