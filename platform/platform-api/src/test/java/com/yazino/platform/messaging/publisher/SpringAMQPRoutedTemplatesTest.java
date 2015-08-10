package com.yazino.platform.messaging.publisher;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.Message;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SpringAMQPRoutedTemplatesTest {

    private final Map<String, CachingConnectionFactory> connectionFactories = new HashMap<>();

    @Mock
    private CloneableRabbitTemplate sourceTemplate;
    @Mock
    private MessageProperties messageProperties;
    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private ConnectionFactoryFactory connectionFactoryFactory;

    private CloneableRabbitTemplate template1;
    private CloneableRabbitTemplate template2;
    private CloneableRabbitTemplate template3;

    private SpringAMQPRoutedTemplates underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(10000000L);

        when(yazinoConfiguration.getStringArray("a.test.property")).thenReturn(new String[]{"host1", "host2", "host3"});
        when(yazinoConfiguration.getLong("strata.rabbitmq.blacklist-time", 60000L)).thenReturn(60000L);

        connectionFactories.put("host1", mock(CachingConnectionFactory.class));
        connectionFactories.put("host2", mock(CachingConnectionFactory.class));
        connectionFactories.put("host3", mock(CachingConnectionFactory.class));

        when(connectionFactoryFactory.forHost(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                return connectionFactories.get(invocation.getArguments()[0].toString());
            }
        });
        when(connectionFactoryFactory.isAvailable(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                return connectionFactories.containsKey(invocation.getArguments()[0].toString());
            }
        });

        template1 = createTemplateFor("host1");
        template2 = createTemplateFor("host2");
        template3 = createTemplateFor("host3");

        underTest = new SpringAMQPRoutedTemplates("a.test.property", connectionFactoryFactory, sourceTemplate, yazinoConfiguration);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void aNullHostListThrowsANullPointerException() {
        new SpringAMQPRoutedQueuePublishingService(null, 5672, "theVirtualHost", "aUser", "aSecret", sourceTemplate, yazinoConfiguration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void aBlankHostListThrowsANullPointerException() {
        new SpringAMQPRoutedQueuePublishingService("  ", 5672, "theVirtualHost", "aUser", "aSecret", sourceTemplate, yazinoConfiguration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void aSubZeroPortThrowsAnIllegalArgumentException() {
        new SpringAMQPRoutedQueuePublishingService("a.test.property", -35, "theVirtualHost", "aUser", "aSecret", sourceTemplate, yazinoConfiguration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void aPortAbove16BitsThrowsAnIllegalArgumentException() {
        new SpringAMQPRoutedQueuePublishingService("a.test.property", 70000, "theVirtualHost", "aUser", "aSecret", sourceTemplate, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void aNullVirtualHostThrowsANullPointerException() {
        new SpringAMQPRoutedQueuePublishingService("a.test.property", 5672, null, "aUser", "aSecret", sourceTemplate, yazinoConfiguration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void aBlankVirtualHostThrowsANullPointerException() {
        new SpringAMQPRoutedQueuePublishingService("a.test.property", 5672, " ", "aUser", "aSecret", sourceTemplate, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void aNullTemplateThrowsANullPointerException() {
        new SpringAMQPRoutedQueuePublishingService("a.test.property", 5672, "theVirtualHost", "aUser", "aSecret", null, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void aNullYazinoConfigurationThrowsANullPointerException() {
        new SpringAMQPRoutedQueuePublishingService("a.test.property", 5672, "theVirtualHost", "aUser", "aSecret", sourceTemplate, null);
    }

    @Test
    public void aNullUserIsAccepted() {
        new SpringAMQPRoutedQueuePublishingService("a.test.property", 5672, "theVirtualHost", null, "aSecret", sourceTemplate, yazinoConfiguration);
    }

    @Test
    public void aNullPasswordIsAccepted() {
        new SpringAMQPRoutedQueuePublishingService("a.test.property", 5672, "theVirtualHost", "aUser", null, sourceTemplate, yazinoConfiguration);
    }

    @Test
    public void aMessageIsSentToATemplate() throws UnsupportedEncodingException {
        final TestMessage message = new TestMessage(1);

        underTest.templateFor(underTest.hostFor(message)).convertAndSend(message);

        verify(template1).convertAndSend(eq(message));
    }

    @Test
    public void messagesAreRoutedToTheCorrectTemplates() throws UnsupportedEncodingException {
        underTest.templateFor(underTest.hostFor(new TestMessage(1))).convertAndSend(new TestMessage(1));
        underTest.templateFor(underTest.hostFor(new TestMessage(5))).convertAndSend(new TestMessage(5));
        underTest.templateFor(underTest.hostFor(new TestMessage(9))).convertAndSend(new TestMessage(9));

        verify(template1).convertAndSend(eq(new TestMessage(1)));
        verify(template2).convertAndSend(eq(new TestMessage(5)));
        verify(template3).convertAndSend(eq(new TestMessage(9)));
    }

    @Test
    public void messagesAreNoLongerRoutedToABlacklistedTemplates() throws UnsupportedEncodingException {
        underTest.blacklist(underTest.hostFor(new TestMessage(5)));

        underTest.templateFor(underTest.hostFor(new TestMessage(1))).convertAndSend(new TestMessage(1));
        underTest.templateFor(underTest.hostFor(new TestMessage(5))).convertAndSend(new TestMessage(5));
        underTest.templateFor(underTest.hostFor(new TestMessage(10))).convertAndSend(new TestMessage(10));

        verify(template1).convertAndSend(eq(new TestMessage(1)));
        verify(template1).convertAndSend(eq(new TestMessage(5)));
        verify(template3).convertAndSend(eq(new TestMessage(10)));
        verifyZeroInteractions(template2);
    }

    @Test
    public void messagesAreNoLongerRoutedToABlacklistedTemplatesForTheEntireBlacklistPeriod() throws UnsupportedEncodingException {
        underTest.blacklist(underTest.hostFor(new TestMessage(5)));
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(DateTimeUtils.currentTimeMillis() + 29999);

        underTest.templateFor(underTest.hostFor(new TestMessage(1))).convertAndSend(new TestMessage(1));
        underTest.templateFor(underTest.hostFor(new TestMessage(5))).convertAndSend(new TestMessage(5));
        underTest.templateFor(underTest.hostFor(new TestMessage(10))).convertAndSend(new TestMessage(10));

        verify(template1).convertAndSend(eq(new TestMessage(1)));
        verify(template1).convertAndSend(eq(new TestMessage(5)));
        verify(template3).convertAndSend(eq(new TestMessage(10)));
        verifyZeroInteractions(template2);
    }

    @Test
    public void aBlacklistedHostIsRestoredAfterThirtySecondsIfAvailable() throws UnsupportedEncodingException {
        underTest.blacklist(underTest.hostFor(new TestMessage(5)));
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(DateTimeUtils.currentTimeMillis() + 300000);

        underTest.templateFor(underTest.hostFor(new TestMessage(1))).convertAndSend(new TestMessage(1));
        underTest.templateFor(underTest.hostFor(new TestMessage(5))).convertAndSend(new TestMessage(5));
        underTest.templateFor(underTest.hostFor(new TestMessage(9))).convertAndSend(new TestMessage(9));

        verify(template1).convertAndSend(eq(new TestMessage(1)));
        verify(template2).convertAndSend(eq(new TestMessage(5)));
        verify(template3).convertAndSend(eq(new TestMessage(9)));
    }

    @Test
    public void aBlacklistedHostIsNotRestoredAfterThirtySecondsIfItIsNotAvailable() throws UnsupportedEncodingException {
        when(connectionFactoryFactory.isAvailable("host2")).thenReturn(false);
        underTest.blacklist("host2");
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(DateTimeUtils.currentTimeMillis() + 300000);

        underTest.templateFor(underTest.hostFor(new TestMessage(1))).convertAndSend(new TestMessage(1));
        underTest.templateFor(underTest.hostFor(new TestMessage(5))).convertAndSend(new TestMessage(5));
        underTest.templateFor(underTest.hostFor(new TestMessage(9))).convertAndSend(new TestMessage(9));

        verifyZeroInteractions(template2);
    }

    @Test
    public void theLastHostAvailableIsNeverBlacklisted() throws UnsupportedEncodingException {
        underTest.blacklist(underTest.hostFor(new TestMessage(1)));
        underTest.blacklist(underTest.hostFor(new TestMessage(5)));
        underTest.blacklist(underTest.hostFor(new TestMessage(9)));

        underTest.templateFor(underTest.hostFor(new TestMessage(1))).convertAndSend(new TestMessage(1));
        underTest.templateFor(underTest.hostFor(new TestMessage(5))).convertAndSend(new TestMessage(5));
        underTest.templateFor(underTest.hostFor(new TestMessage(9))).convertAndSend(new TestMessage(9));

        verify(template3).convertAndSend(eq(new TestMessage(1)));
        verify(template3).convertAndSend(eq(new TestMessage(5)));
        verify(template3).convertAndSend(eq(new TestMessage(9)));
    }

    @Test
    public void connectionFactoriesAreInitialisedOnlyOnce() throws UnsupportedEncodingException {
        underTest.templateFor(underTest.hostFor(new TestMessage(1)));
        underTest.templateFor(underTest.hostFor(new TestMessage(1)));
        underTest.templateFor(underTest.hostFor(new TestMessage(1)));

        verify(connectionFactoryFactory, times(1)).forHost("host1");
    }

    @Test
    public void connectionFactoriesAreOnlyInitialisedOnUse() throws UnsupportedEncodingException {
        verifyZeroInteractions(connectionFactories.get("host2"));

        underTest.templateFor(underTest.hostFor(new TestMessage(5)));

        verify(connectionFactoryFactory, times(1)).forHost("host2");
        verifyZeroInteractions(connectionFactories.get("host1"));
        verifyZeroInteractions(connectionFactories.get("host3"));
    }

    @Test
    public void hostsAreIgnoredWhenBlank() throws UnsupportedEncodingException {
        reset(yazinoConfiguration);
        when(yazinoConfiguration.getStringArray("a.test.property")).thenReturn(new String[]{"", " ", "host1  ", "  host2", "host3 ", " ", ""});
        underTest = new SpringAMQPRoutedTemplates("a.test.property", connectionFactoryFactory, sourceTemplate, yazinoConfiguration);

        underTest.templateFor(underTest.hostFor(new TestMessage(1))).convertAndSend(new TestMessage(1));
        underTest.templateFor(underTest.hostFor(new TestMessage(5))).convertAndSend(new TestMessage(5));
        underTest.templateFor(underTest.hostFor(new TestMessage(9))).convertAndSend(new TestMessage(9));

        verify(template1).convertAndSend(eq(new TestMessage(1)));
        verify(template2).convertAndSend(eq(new TestMessage(5)));
        verify(template3).convertAndSend(eq(new TestMessage(9)));
    }

    @Test
    public void aConfigurationListenerIsRegisteredOnCreation() {
        verify(yazinoConfiguration).addConfigurationListener(Matchers.any(ConfigurationListener.class));
    }

    @Test
    public void configurationListenerConfigChangesCauseAReloadOfTheHosts() {
        connectionFactories.put("host4", mock(CachingConnectionFactory.class));
        createTemplateFor("host4");
        final ArgumentCaptor<ConfigurationListener> configurationListenerCaptor = ArgumentCaptor.forClass(ConfigurationListener.class);
        verify(yazinoConfiguration).addConfigurationListener(configurationListenerCaptor.capture());
        final ConfigurationListener listener = configurationListenerCaptor.getValue();
        reset(yazinoConfiguration);
        when(yazinoConfiguration.getStringArray("a.test.property")).thenReturn(new String[]{"host1", "host2", "host3", "host4"});

        listener.configurationChanged(new ConfigurationEvent(this, 1, "a.test.property", null, false));

        underTest.templateFor(underTest.hostFor(new TestMessage(2)));
        verify(connectionFactoryFactory, times(1)).forHost("host4");
    }

    @Test
    public void updatingTheHostsDeletesNoLongerAvailableConnectionFactories() {
        final ArgumentCaptor<ConfigurationListener> configurationListenerCaptor = ArgumentCaptor.forClass(ConfigurationListener.class);
        verify(yazinoConfiguration).addConfigurationListener(configurationListenerCaptor.capture());
        final ConfigurationListener listener = configurationListenerCaptor.getValue();
        reset(yazinoConfiguration);
        when(yazinoConfiguration.getStringArray("a.test.property")).thenReturn(new String[]{"host1", "host3"});

        listener.configurationChanged(new ConfigurationEvent(this, 1, null, null, false));

        underTest.templateFor(underTest.hostFor(new TestMessage(1)));
        underTest.templateFor(underTest.hostFor(new TestMessage(2)));
        underTest.templateFor(underTest.hostFor(new TestMessage(3)));

        verifyZeroInteractions(connectionFactories.get("host2"));
    }

    private CloneableRabbitTemplate createTemplateFor(final String host) {
        final CloneableRabbitTemplate template = mock(CloneableRabbitTemplate.class);
        when(sourceTemplate.newWith(connectionFactories.get(host))).thenReturn(template);
        return template;
    }

    private class TestMessage implements Message<String> {
        private static final long serialVersionUID = 5091679082893738810L;

        private final int messageId;

        private TestMessage(final int messageId) {
            this.messageId = messageId;
        }

        @Override
        public int getVersion() {
            return 1;
        }

        @Override
        public String getMessageType() {
            return "testMessage:" + messageId;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            final TestMessage rhs = (TestMessage) obj;
            return new EqualsBuilder()
                    .append(messageId, rhs.messageId)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(13, 17)
                    .append(messageId)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append(messageId)
                    .toString();
        }
    }
}
