package com.yazino.bi.messaging;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.Message;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BIWorkerServiceFactoryIntegrationTest {
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private TestConsumer queueMessageConsumer = new TestConsumer();
    private WorkerServers servers;

    private BIWorkerServiceFactory underTest;

    @Before
    public void setUp() {
        underTest = new BIWorkerServiceFactory();

        // WARNING: this will connect to the local server.
        servers = new WorkerServers("hosts.property.name", 5672, "worker", "w0rk3r%", "maggie-test");

        when(yazinoConfiguration.getStringArray("hosts.property.name")).thenReturn(new String[] {"localhost"});
    }

    @Test
    public void commitMessagesAreDelegatedFromTheListenerToACommitAwareConsumer() {
        final SimpleMessageListenerContainer listenerContainer = underTest.startConcurrentConsumers(
                servers, yazinoConfiguration, "worker.biworkerserverfactorytest", "worker.biworkerserverfactorytest", "aRoutingKey", queueMessageConsumer, 1, 1);

        final Object messageListener = listenerContainer.getMessageListener();
        assertThat(messageListener instanceof CommitAware, is(true));
        ((CommitAware) messageListener).consumerCommitting();
        assertThat(queueMessageConsumer.isCommitted(), is(true));
    }

    @Test
    public void commitMessagesAreNotDelegatedFromTheListenerToANonCommitAwareConsumer() {
        final QueueMessageConsumer nonAwareConsumer = mock(QueueMessageConsumer.class);
        final SimpleMessageListenerContainer listenerContainer = underTest.startConcurrentConsumers(
                servers, yazinoConfiguration, "worker.biworkerserverfactorytest", "worker.biworkerserverfactorytest", "aRoutingKey", nonAwareConsumer, 1, 1);

        final Object messageListener = listenerContainer.getMessageListener();
        assertThat(messageListener instanceof CommitAware, is(false));
    }

    private class TestConsumer implements QueueMessageConsumer<Message>, CommitAware {
        private boolean committed = false;
        private boolean handled = false;

        @Override
        public void consumerCommitting() {
            committed = true;
        }

        @Override
        public void handle(final Message message) {
            handled = true;
        }

        private boolean isCommitted() {
            return committed;
        }

        private boolean isHandled() {
            return handled;
        }
    }

}
