package strata.server.worker.event;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

public class SpringContextLoadsIntegrationTest {

	@Test
	@DirtiesContext
	public void testSpringContextLoadsCorrectly() {
		new ClassPathXmlApplicationContext("classpath:/META-INF/spring/context.xml");
	}

    /* used to run the event consumers locally. Requires local copy of /etc/senet/aggregator.environment.properties */
    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("classpath:/META-INF/spring/context.xml");
    }
}
