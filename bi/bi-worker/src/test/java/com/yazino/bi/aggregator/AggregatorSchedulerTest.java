package com.yazino.bi.aggregator;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.*;

public class AggregatorSchedulerTest {
    private AggregatorScheduler underTest;
    private SchedulerDao schedulerDao = mock(SchedulerDao.class);
    public ClassPathXmlApplicationContext mockContext = mock(ClassPathXmlApplicationContext.class);

    @Test
    public void schedulerShouldCallDao() {
        underTest.checkForScheduledAggregators();
        verify(schedulerDao).getScheduledAggregators();
    }

    @Before
    public void setUp() throws Exception {
        underTest = new AggregatorScheduler(schedulerDao);
        underTest.setApplicationContext(mockContext);

    }

    @Test
    public void schedulerShouldScheduleLoadedSchedules() {//errk
        final List<String> scheduledAggregators = newArrayList();
        scheduledAggregators.add("testAggie");
        when(schedulerDao.getScheduledAggregators()).thenReturn(scheduledAggregators);
        final Aggregator mockAggie = mock(Aggregator.class);
        when(mockContext.getBean("testAggie", Aggregator.class)).thenReturn(mockAggie);

        underTest.checkForScheduledAggregators();
        verify(mockAggie).update();
    }

    @Test
    public void misspeltAggregatorShouldNotInterruptExecution() {
        final List<String> scheduledAggregators = newArrayList();
        scheduledAggregators.add("notAnAggie");
        scheduledAggregators.add("testAggie");
        when(schedulerDao.getScheduledAggregators()).thenReturn(scheduledAggregators);
        final Aggregator mockAggie = mock(Aggregator.class);
        when(mockContext.getBean("notAnAggie", Aggregator.class)).thenThrow(new NoSuchBeanDefinitionException("couldn't load"));
        when(mockContext.getBean("testAggie", Aggregator.class)).thenReturn(mockAggie);

        underTest.checkForScheduledAggregators();
        verify(mockAggie).update();
    }
}
