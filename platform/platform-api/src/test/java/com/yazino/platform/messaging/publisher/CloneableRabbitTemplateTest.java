package com.yazino.platform.messaging.publisher;

import org.junit.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class CloneableRabbitTemplateTest {

    @Test
    public void aCloneableRabbitTemplateIsARabbitTemplate() {
        assertThat(aCloneableTemplate(), is(instanceOf(RabbitTemplate.class)));
    }

    @Test
    public void aCloneOfATemplateIsNotTheSameObject() {
        final CloneableRabbitTemplate template = aCloneableTemplate();
        assertThat(template.newWith(mock(ConnectionFactory.class)),
                is(not(sameInstance(template))));
    }

    @Test
    public void aCloneCopiesAllValuesExceptConnectionFactory() {
        final CloneableRabbitTemplate original = aCloneableTemplate();
        final ConnectionFactory expectedConnectionFactory = mock(ConnectionFactory.class);

        final CloneableRabbitTemplate clone = original.newWith(expectedConnectionFactory);

        assertThat(clone.isChannelTransacted(), is(equalTo(original.isChannelTransacted())));
        assertThat(clone.getConnectionFactory(), is(sameInstance(expectedConnectionFactory)));
        assertThat(clone.getEncoding(), is(equalTo(original.getEncoding())));
        assertThat(clone.getExchange(), is(equalTo(original.getExchange())));
        assertThat(clone.getMessageConverter(), is(sameInstance(original.getMessageConverter())));
        assertThat(clone.getMessagePropertiesConverter(), is(sameInstance(original.getMessagePropertiesConverter())));
        assertThat(clone.getQueue(), is(equalTo(original.getQueue())));
        assertThat(clone.getReplyTimeout(), is(equalTo(original.getReplyTimeout())));
        assertThat(clone.getRoutingKey(), is(equalTo(original.getRoutingKey())));
    }

    @Test
    public void aCloneDoesNotTryToCopyNullFields() {
        final CloneableRabbitTemplate original = new CloneableRabbitTemplate();
        final ConnectionFactory expectedConnectionFactory = mock(ConnectionFactory.class);

        final CloneableRabbitTemplate clone = original.newWith(expectedConnectionFactory);
        assertThat(clone.getQueue(), is(equalTo(original.getQueue())));
        assertThat(clone.getMessageConverter(), is(equalTo(original.getMessageConverter())));
        assertThat(clone.getMessagePropertiesConverter(), is(equalTo(original.getMessagePropertiesConverter())));
    }

    private CloneableRabbitTemplate aCloneableTemplate() {
        final CloneableRabbitTemplate cloneableRabbitTemplate = new CloneableRabbitTemplate();

        cloneableRabbitTemplate.setChannelTransacted(true);
        cloneableRabbitTemplate.setConnectionFactory(mock(ConnectionFactory.class));
        cloneableRabbitTemplate.setEncoding("anEncoding");
        cloneableRabbitTemplate.setExchange("anExchange");
        cloneableRabbitTemplate.setMessageConverter(mock(MessageConverter.class));
        cloneableRabbitTemplate.setMessagePropertiesConverter(mock(MessagePropertiesConverter.class));
        cloneableRabbitTemplate.setQueue("aQueue");
        cloneableRabbitTemplate.setReplyTimeout(1001);
        cloneableRabbitTemplate.setRoutingKey("aRoutingKey");

        return cloneableRabbitTemplate;
    }
}
