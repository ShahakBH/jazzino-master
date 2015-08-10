package com.yazino.bi.operations;

import com.yazino.engagement.ChannelType;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import com.yazino.bi.operations.engagement.GameTypeValidator;
import static org.junit.Assert.assertNotNull;
import java.util.Map;

public class SpringContextLoadsIntegrationTest {
    @Test
    @DirtiesContext
    public void testSpringContextLoadsCorrectly() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:/META-INF/spring/springmvc-spring.xml");

        assertThatGameTypeValidatorForFacebookAppToUserNotificationIsNotNull(context);
    }

    @SuppressWarnings("unchecked")
    private void assertThatGameTypeValidatorForFacebookAppToUserNotificationIsNotNull(ClassPathXmlApplicationContext context) {
        Map<ChannelType, GameTypeValidator> gameTypeValidatorMap = (Map<ChannelType, GameTypeValidator>)context.getBean("gameTypeValidatorMap");

        assertNotNull(gameTypeValidatorMap.get(ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION));
    }
}
