package strata.server.performance.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.DefaultClassMapper;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.util.Assert.notNull;

/**
 * This class is responsible for taking the configuration of a {@link ReplayableSource} object and managing the
 * messages to be sent.
 */
public class SourceReplayer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(SourceReplayer.class);

    private static final AtomicInteger MESSAGE_ID_GENERATOR = new AtomicInteger(0);
    private final AmqpTemplate mTemplate;
    private final ReplayableSource mSource;

    private Sleeper mSleeper = new ThreadSleeper();
    private int mNumberReplays = 1;
    private MessageProperties properties = new MessageProperties();

    public SourceReplayer(final AmqpTemplate template,
                          final ReplayableSource source) {
        notNull(template, "channel was null");
        notNull(source, "source was null");
        mTemplate = template;
        mSource = source;
    }

    @Override
    public void run() {
        try {
            mSource.init();
            final long messageInterval = mSource.getMessageInterval();
            final String exchange = mSource.getExchange();
            final String messageType = mSource.getMessageClass();
            final String contentType = mSource.getContentType();
            properties.getHeaders().put(DefaultClassMapper.DEFAULT_CLASSID_FIELD_NAME, messageType);

            properties.setContentType(contentType);

            LOG.info(String.format("Initialized Replayable [%s], messageInterval [%d], exchange [%s]",
                    mSource, messageInterval, exchange));

            int loops = 0;
            while (loops < mNumberReplays) {
                while (mSource.hasNext()) {
                    final ReplayableMessage message = mSource.next();
                    sendMessage(exchange, message);
                    mSleeper.sleep(messageInterval);
                }
                loops++;
                mSource.reset();
            }

            LOG.info(String.format("Finished running [%s]", mSource));

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            destroySourceQuietly();
        }
    }

    private void sendMessage(String exchange, ReplayableMessage message) {
        final String routingKey = message.getRoutingKey();
        final byte[] body = message.getBody();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Publishing [%s] using routing key [%s]", new String(body), routingKey));
        }
        String id = message.getID();
        if (id == null) {
            id = String.valueOf(MESSAGE_ID_GENERATOR.incrementAndGet());
        }
        properties.setMessageId(id);
        mTemplate.send(exchange, routingKey, new Message(body, properties));
    }

    private void destroySourceQuietly() {
        LOG.info(String.format("Destroying Replayable [%s]", mSource));
        try {
            mSource.destroy();
        } catch (Exception e) {
            LOG.debug("Failed to destroy source quietly due to:" + e.getMessage());
        }
    }

    public void setSleeper(final Sleeper sleeper) {
        notNull(sleeper, "sleeper was null");
        this.mSleeper = sleeper;
    }

    public void setNumberReplays(final int numberReplays) {
        this.mNumberReplays = numberReplays;
    }
}
