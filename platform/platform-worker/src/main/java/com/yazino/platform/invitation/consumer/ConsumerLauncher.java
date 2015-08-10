package com.yazino.platform.invitation.consumer;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public final class ConsumerLauncher {
    private ConsumerLauncher() {

    }

    public static void main(final String... args) {
        final ClassPathXmlApplicationContext context
                = new ClassPathXmlApplicationContext("classpath:/META-INF/spring/context.xml");

    }
}
