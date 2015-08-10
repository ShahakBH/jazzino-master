package com.yazino.platform.messaging.publisher;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;

import java.io.UnsupportedEncodingException;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SpringAMQPRoutedQueuePublishingServiceTest {

    @Mock
    private MessageProperties messageProperties;
    @Mock
    private SpringAMQPRoutedTemplates templates;
    
    private CloneableRabbitTemplate template1;

    private SpringAMQPRoutedQueuePublishingService underTest;
    
    @Before
    public void setUp() {
        template1 = createTemplateFor();
        underTest = new SpringAMQPRoutedQueuePublishingService(templates);
    }
    
    @Test
    public void aNullMessageIsIgnored() throws UnsupportedEncodingException {
        underTest.send(null);
        verifyZeroInteractions(templates);
    }

    @Test
    public void aMessageIsSentToATemplate() throws UnsupportedEncodingException {
        final TestMessage message = new TestMessage(1);
        when(templates.hostFor(message)).thenReturn("host1");
        when(templates.templateFor("host1")).thenReturn(template1);
        underTest.send(message);
        verify(template1).convertAndSend(eq(message), Matchers.any(MessagePostProcessor.class));
    }
    
    @Test
    public void theMessageTypeIsSetToTheMessagesTypeInTheHeader() throws UnsupportedEncodingException {
        final TestMessage message = new TestMessage(1);
        when(templates.hostFor(message)).thenReturn("host1");
        when(templates.templateFor("host1")).thenReturn(template1);
        underTest.send(message);
        verify(messageProperties).setType(message.getMessageType());
    }

    @Test
    public void theMessageDeliveryTypeIsSetToPersistentInTheHeader() throws UnsupportedEncodingException {
        final TestMessage message = new TestMessage(1);
        when(templates.hostFor(message)).thenReturn("host1");
        when(templates.templateFor("host1")).thenReturn(template1);
        underTest.send(message);
        verify(messageProperties).setDeliveryMode(MessageDeliveryMode.PERSISTENT);
    }

    private CloneableRabbitTemplate createTemplateFor() {
        final CloneableRabbitTemplate template = mock(CloneableRabbitTemplate.class);
        doAnswer(new Answer<org.springframework.amqp.core.Message>() {
            @Override
            public org.springframework.amqp.core.Message answer(final InvocationOnMock invocation) throws Throwable {
                final TestMessage testMessage = (TestMessage) invocation.getArguments()[0];
                return ((MessagePostProcessor) invocation.getArguments()[1]).postProcessMessage(
                        new org.springframework.amqp.core.Message(testMessage.getMessageType().getBytes("UTF-8"),
                                messageProperties));
            }
        }).when(template).convertAndSend(
                Matchers.any(org.springframework.amqp.core.Message.class),
                Matchers.any(MessagePostProcessor.class));
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
