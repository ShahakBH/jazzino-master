package com.yazino.platform.lightstreamer.adapter;

import com.lightstreamer.interfaces.data.ItemEventListener;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.yazino.configuration.YazinoConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MobileSubscriptionContainerFactoryTest {
    @Mock
    private Exchange exchange;
    @Mock
    private ItemEventListener eventListener;
    @Mock
    private ConnectionFactoryFactory connectionFactoryFactory;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private ConnectionFactory connectionFactory;
    private MobileSubscriptionContainerFactory underTest;

    @Before
    public void setup() throws Exception {
        DateTimeUtils.setCurrentMillisFixed(100000000L);

        connectionFactory = connectionFactoryFor("aHost");

        when(yazinoConfiguration.getLong("strata.rabbitmq.blacklist-time", 300000L)).thenReturn(30000L);
        when(yazinoConfiguration.getStringArray("strata.rabbitmq.host")).thenReturn(new String[]{"aHost"});
        when(connectionFactoryFactory.forHost("aHost")).thenReturn(connectionFactory);

        underTest = new MobileSubscriptionContainerFactory(exchange, connectionFactoryFactory, yazinoConfiguration);
        underTest.setEventListener(eventListener);
    }

    @After
    public void resetJodaTime() {
        DateTimeUtils.currentTimeMillis();
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfSubjectIsNull() throws Exception {
        underTest.containerForSubject(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfSubjectIsEmpty() throws Exception {
        underTest.containerForSubject("   ");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfItemListenerNotSetup() throws Exception {
        underTest.setEventListener(null);
        underTest.containerForSubject("foo");
    }

    @Test
    public void shouldReturnContainerWithSameFactoryWhenOnlyOne() throws Exception {
        assertSame(connectionFactory, underTest.containerForSubject("PLAYER.12345").getConnectionFactory());
        assertSame(connectionFactory, underTest.containerForSubject("PLAYER.678").getConnectionFactory());
        assertSame(connectionFactory, underTest.containerForSubject("PLAYER.123000").getConnectionFactory());
        assertSame(connectionFactory, underTest.containerForSubject("PLAYER.500").getConnectionFactory());
    }

    @Test
    public void shouldReturnContainerWithQueueForPlayer() throws Exception {
        SimpleMessageListenerContainer container = underTest.containerForSubject("PLAYER.12345");
        assertEquals(1, container.getQueueNames().length);
    }

    @Test
    public void shouldReturnContainerWithCorrectMessageListener() throws Exception {
        SimpleMessageListenerContainer container = underTest.containerForSubject("PLAYER.12345");
        Object listener = container.getMessageListener();
        assertEquals(MobileMessageListenerAdapter.class, listener.getClass());
    }

    @Test
    public void shouldReturnContainerWithListenerForSubject() throws Exception {
        SimpleMessageListenerContainer container = underTest.containerForSubject("PLAYER.12345");
        MobileMessageListenerAdapter adapter = (MobileMessageListenerAdapter) container.getMessageListener();
        assertEquals("PLAYER.12345", adapter.getSubject());
    }

    @Test
    public void shouldReturnContainerWithListenerWithEventListener() throws Exception {
        SimpleMessageListenerContainer container = underTest.containerForSubject("PLAYER.12345");
        MobileMessageListenerAdapter adapter = (MobileMessageListenerAdapter) container.getMessageListener();
        assertEquals(eventListener, adapter.getEventListener());
    }

    @Test
    public void shouldRestoreBlacklistedHostsAfterThirtySecondsIfHostIsAvailable() throws Exception {
        ConnectionFactory oddPlayersFactory = connectionFactoryFor("hostOdd");
        ConnectionFactory evenPlayersFactory = connectionFactoryFor("hostEven");
        when(yazinoConfiguration.getStringArray("strata.rabbitmq.host")).thenReturn(new String[]{"hostEven", "hostOdd"});
        when(connectionFactoryFactory.isAvailable("hostEven")).thenReturn(true);

        underTest = new MobileSubscriptionContainerFactory(exchange, connectionFactoryFactory, yazinoConfiguration);
        underTest.setEventListener(eventListener);
        underTest.blacklist("hostEven");

        DateTimeUtils.setCurrentMillisFixed(DateTimeUtils.currentTimeMillis() + 30000);

        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.1").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.11").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.577").getConnectionFactory());
        assertSame(evenPlayersFactory, underTest.containerForSubject("PLAYER.2").getConnectionFactory());
        assertSame(evenPlayersFactory, underTest.containerForSubject("PLAYER.20").getConnectionFactory());
        assertSame(evenPlayersFactory, underTest.containerForSubject("PLAYER.800862").getConnectionFactory());
    }

    @Test
    public void shouldNotRestoreBlacklistedHostsAfterThirtySecondsIfHostIsNotAvailable() throws Exception {
        ConnectionFactory oddPlayersFactory = connectionFactoryFor("hostOdd");
        when(yazinoConfiguration.getStringArray("strata.rabbitmq.host")).thenReturn(new String[]{"hostEven", "hostOdd"});
        when(connectionFactoryFactory.isAvailable("hostEven")).thenReturn(false);

        underTest = new MobileSubscriptionContainerFactory(exchange, connectionFactoryFactory, yazinoConfiguration);
        underTest.setEventListener(eventListener);
        underTest.blacklist("hostEven");

        DateTimeUtils.setCurrentMillisFixed(DateTimeUtils.currentTimeMillis() + 30000);

        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.1").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.11").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.577").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.2").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.20").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.800862").getConnectionFactory());
    }

    @Test
    public void shouldRemoveBlacklistedHostsFromSelection() throws Exception {
        ConnectionFactory oddPlayersFactory = connectionFactoryFor("hostOdd");
        connectionFactoryFor("hostEven");
        when(yazinoConfiguration.getStringArray("strata.rabbitmq.host")).thenReturn(new String[]{"hostEven", "hostOdd"});

        underTest = new MobileSubscriptionContainerFactory(exchange, connectionFactoryFactory, yazinoConfiguration);
        underTest.setEventListener(eventListener);
        underTest.blacklist("hostEven");

        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.1").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.11").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.577").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.2").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.20").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.800862").getConnectionFactory());
    }

    @Test
    public void shouldNeverBlacklistedLastHosts() throws Exception {
        ConnectionFactory oddPlayersFactory = connectionFactoryFor("hostOdd");
        connectionFactoryFor("hostEven");
        when(yazinoConfiguration.getStringArray("strata.rabbitmq.host")).thenReturn(new String[]{"hostEven", "hostOdd"});

        underTest = new MobileSubscriptionContainerFactory(exchange, connectionFactoryFactory, yazinoConfiguration);
        underTest.setEventListener(eventListener);
        underTest.blacklist("hostEven");
        underTest.blacklist("hostOdd");

        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.1").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.11").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.577").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.2").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.20").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.800862").getConnectionFactory());
    }

    @Test
    public void shouldUpdateHostsWhenEventReceived() throws Exception {
        ConnectionFactory oddPlayersFactory = connectionFactoryFor("hostOdd");
        ConnectionFactory evenPlayersFactory = connectionFactoryFor("hostEven");
        reset(yazinoConfiguration);
        when(yazinoConfiguration.getStringArray("strata.rabbitmq.host")).thenReturn(new String[]{"hostEven", "hostOdd"});
        underTest = new MobileSubscriptionContainerFactory(exchange, connectionFactoryFactory, yazinoConfiguration);
        underTest.setEventListener(eventListener);
        ArgumentCaptor<ConfigurationListener> configurationListenerCaptor = ArgumentCaptor.forClass(ConfigurationListener.class);
        verify(yazinoConfiguration).addConfigurationListener(configurationListenerCaptor.capture());

        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.1").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.11").getConnectionFactory());

        when(yazinoConfiguration.getStringArray("strata.rabbitmq.host")).thenReturn(new String[]{"hostOdd", "hostEven"});
        configurationListenerCaptor.getValue().configurationChanged(new ConfigurationEvent(this, 0, "strata.rabbitmq.host", new String[]{"hostOdd", "hostEven"}, false));

        assertSame(evenPlayersFactory, underTest.containerForSubject("PLAYER.1").getConnectionFactory());
        assertSame(evenPlayersFactory, underTest.containerForSubject("PLAYER.11").getConnectionFactory());
    }

    @Test
    public void shouldReturnContainerWithFactoryForPlayer() throws Exception {
        ConnectionFactory oddPlayersFactory = connectionFactoryFor("hostOdd");
        ConnectionFactory evenPlayersFactory = connectionFactoryFor("hostEven");
        when(yazinoConfiguration.getStringArray("strata.rabbitmq.host")).thenReturn(new String[]{"hostEven", "hostOdd"});

        underTest = new MobileSubscriptionContainerFactory(exchange, connectionFactoryFactory, yazinoConfiguration);
        underTest.setEventListener(eventListener);

        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.1").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.11").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYER.577").getConnectionFactory());
        assertSame(evenPlayersFactory, underTest.containerForSubject("PLAYER.2").getConnectionFactory());
        assertSame(evenPlayersFactory, underTest.containerForSubject("PLAYER.20").getConnectionFactory());
        assertSame(evenPlayersFactory, underTest.containerForSubject("PLAYER.800862").getConnectionFactory());
    }

    @Test
    public void shouldReturnContainerWithFactoryForPlayerTable() throws Exception {
        ConnectionFactory oddPlayersFactory = connectionFactoryFor("hostOdd");
        ConnectionFactory evenPlayersFactory = connectionFactoryFor("hostEven");
        when(yazinoConfiguration.getStringArray("strata.rabbitmq.host")).thenReturn(new String[]{"hostEven", "hostOdd"});

        underTest = new MobileSubscriptionContainerFactory(exchange, connectionFactoryFactory, yazinoConfiguration);
        underTest.setEventListener(eventListener);

        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYERTABLE.1.45").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYERTABLE.11.66").getConnectionFactory());
        assertSame(oddPlayersFactory, underTest.containerForSubject("PLAYERTABLE.577.90").getConnectionFactory());
        assertSame(evenPlayersFactory, underTest.containerForSubject("PLAYERTABLE.2.65634").getConnectionFactory());
        assertSame(evenPlayersFactory, underTest.containerForSubject("PLAYERTABLE.20.20").getConnectionFactory());
        assertSame(evenPlayersFactory, underTest.containerForSubject("PLAYERTABLE.800862.780").getConnectionFactory());
    }

    private ConnectionFactory connectionFactoryFor(final String hostname) throws Exception {
        final AMQP.Queue.DeclareOk declareOk = mock(AMQP.Queue.DeclareOk.class);
        when(declareOk.getQueue()).thenReturn("aQueueName");

        final Channel channel = mock(Channel.class);
        when(channel.queueDeclare()).thenReturn(declareOk);

        final Connection connection = mock(Connection.class);
        when(connection.createChannel(anyBoolean())).thenReturn(channel);

        final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connectionFactory.getHost()).thenReturn(hostname);

        when(connectionFactoryFactory.forHost(hostname)).thenReturn(connectionFactory);

        return connectionFactory;
    }

}
