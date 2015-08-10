package com.yazino.web;

import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.session.ReferrerSessionCache;
import com.yazino.web.util.PlayerFriendsCache;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

public class SpringContextLoadsIntegrationTest {

    @Test
    @DirtiesContext
    public void testLobbySpringContextLoadsCorrectly() {
        final ApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:/com/yazino/web/SpringContextLoadsIntegrationTest-context.xml");

        // session scoped beans
        ((LobbySessionCache) context.getBean("lobbySessionCache")).invalidateLocalSession();
        ((ReferrerSessionCache) context.getBean("referrerSessionCache")).getReferrer();
        ((PlayerFriendsCache) context.getBean("playerFriendsCache")).setLeaseTime(5000);
    }
}
