package strata.server.lobby.promotion;

import org.junit.Test;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import strata.server.lobby.api.promotion.Promotion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class SpringContextLoadsIntegrationTest {

    @Test
    @DirtiesContext
    public void testSpringContextLoadsCorrectly() {
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("/SpringContextLoadsTest-context.xml");
        Promotion defaultDailyPromotion = (Promotion) classPathXmlApplicationContext.getBean("defaultDailyPromotion");
        assertNotNull(defaultDailyPromotion);
        assertNotNull(classPathXmlApplicationContext.getBean("playerPlayedEventConsumer"));
        assertFalse(classPathXmlApplicationContext.getBeansOfType(SimpleMessageListenerContainer.class).isEmpty());
    }
}
