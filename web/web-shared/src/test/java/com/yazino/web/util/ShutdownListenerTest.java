package com.yazino.web.util;

import com.yazino.web.util.ShutdownListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class ShutdownListenerTest {

    @Mock
    private GenericWebApplicationContext applicationContext;
    @Mock
    private SchedulerFactoryBean schedulerBeanFactory;
    @Mock
    private Scheduler scheduler;

    private ShutdownListener underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(schedulerBeanFactory.getScheduler()).thenReturn(scheduler);

        underTest = new TestShutdownListener();
    }

    @Test
    public void contextInitialisedDoesNothing() {
        underTest.contextInitialized(null);

        verifyZeroInteractions(applicationContext);
    }

    @Test
    public void theApplicationContextIsClosedOnDestruction() {
        underTest.contextDestroyed(null);

        verify((ConfigurableApplicationContext) applicationContext).close();
    }

    @Test
    public void allRegisteredSchedulersAreShutDown() throws SchedulerException {
        when(applicationContext.getBeansOfType(SchedulerFactoryBean.class)).thenReturn(Collections.singletonMap("aBean", schedulerBeanFactory));

        underTest.contextDestroyed(null);

        verify(scheduler).shutdown(true);
    }

    private class TestShutdownListener extends ShutdownListener {
        @Override
        WebApplicationContext currentApplicationContext() {
            return applicationContext;
        }
    }

}
