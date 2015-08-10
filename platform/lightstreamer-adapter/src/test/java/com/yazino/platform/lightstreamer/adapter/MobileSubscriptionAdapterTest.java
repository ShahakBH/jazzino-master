package com.yazino.platform.lightstreamer.adapter;

import com.yazino.configuration.YazinoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MobileSubscriptionAdapterTest {

    @Mock
    private LifecycleListeningMessageListenerContainer listenerContainer;
    @Mock
    private MobileSubscriptionContainerFactory containerFactory;
    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private ScheduledExecutorService scheduledExecutorService;

    private MobileSubscriptionAdapter underTest;

    @Before
    public void setup() {
        when(containerFactory.containerForSubject(anyString())).thenReturn(listenerContainer);

        when(yazinoConfiguration.getInt("strata.rabbitmq.retry-count", 10)).thenReturn(2);
        when(yazinoConfiguration.getLong("strata.rabbitmq.retry-delay", 3000)).thenReturn(3000L);
        when(yazinoConfiguration.getInt("strata.rabbitmq.connection-expiry", 14400000)).thenReturn(60000);

        underTest = new MobileSubscriptionAdapter(containerFactory, yazinoConfiguration, new ImmediateExecutorService(), scheduledExecutorService);
    }

    @Test
    public void shouldAddSubscriptionOnSubscribe() throws Exception {
        underTest.subscribe("1234", null, false);
        assertEquals(1, underTest.getSubscriptions().size());
        assertEquals(listenerContainer, underTest.getSubscriptions().get("1234"));
    }

    @Test
    public void shouldStartContainerOnSubscribe() throws Exception {
        underTest.subscribe("1234", null, false);
        verify(listenerContainer).start();
    }

    @Test
    public void shouldRemoveSubscriptionOnUnsubscribe() throws Exception {
        underTest.subscribe("1234", null, false);
        underTest.unsubscribe("1234");
        assertTrue(underTest.getSubscriptions().isEmpty());
    }

    @Test
    public void shouldStopContainerOnUnsubscribe() throws Exception {
        underTest.subscribe("1234", null, false);
        underTest.unsubscribe("1234");
        verify(listenerContainer).stop();
    }

    @Test
    public void shouldRemoveSubscriptionIfSubscriptionAlreadyExistsOnSubscribe() throws Exception {
        underTest.subscribe("1234", null, false);
        underTest.subscribe("1234", null, false);
        assertEquals(1, underTest.getSubscriptions().size());
    }

    @Test
    public void shouldNotStopContainerIfSubscriptionAlreadyExistsOnSubscribe() throws Exception {
        underTest.subscribe("1234", null, false);
        underTest.subscribe("1234", null, false);
        verify(listenerContainer, times(0)).stop();
    }

    @Test
    public void shouldScheduleDelayedSubscriptionCheckerEveryHalfASecond() {
        final ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduledExecutorService).scheduleAtFixedRate(taskCaptor.capture(), eq(500L), eq(500L), eq(TimeUnit.MILLISECONDS));
        assertThat(taskCaptor.getValue().getClass().getName(), containsString("DelayedSubscriptionChecker"));
    }

    @Test
    public void shouldScheduleHealthLoggerEveryFiveSeconds() {
        final ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduledExecutorService).scheduleAtFixedRate(taskCaptor.capture(), eq(5000L), eq(5000L), eq(TimeUnit.MILLISECONDS));
        assertThat(taskCaptor.getValue().getClass().getName(), containsString("HealthLogger"));
    }

    @Test
    public void shouldScheduleConnectionReaperEveryFiveMinutes() {
        final ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduledExecutorService).scheduleAtFixedRate(taskCaptor.capture(), eq(300000L), eq(300000L), eq(TimeUnit.MILLISECONDS));
        assertThat(taskCaptor.getValue().getClass().getName(), containsString("AbandonedConnectionReaper"));
    }

    private static class ImmediateExecutorService implements ExecutorService {
        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return null;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public <T> Future<T> submit(final Callable<T> task) {
            return null;
        }

        @Override
        public <T> Future<T> submit(final Runnable task, final T result) {
            task.run();
            return null;
        }

        @Override
        public Future<?> submit(final Runnable task) {
            task.run();
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
            return null;
        }

        @Override
        public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }

        @Override
        public void execute(final Runnable command) {
            command.run();
        }
    }
}
