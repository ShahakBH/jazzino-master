package com.yazino.novomatic.cgs;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static junit.framework.Assert.assertNotNull;

public class ClientSpringSupportIntegrationTest {

    @Test
    public void shouldAllowContextScanning() {
        final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:/novomatic-test-context.xml");
        final NovomaticClient novomaticClient = context.getBean(NovomaticClient.class);
        assertNotNull(novomaticClient);
    }
}
