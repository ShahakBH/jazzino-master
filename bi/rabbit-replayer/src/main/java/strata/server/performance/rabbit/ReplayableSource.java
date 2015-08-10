package strata.server.performance.rabbit;

import java.util.Iterator;

/**
 * An object that provides replayable data.
 */
public interface ReplayableSource extends Iterator<ReplayableMessage> {
    /**
     * Gives the source a chance to initialize itself.
     */
    void init();

    /**
     * Gives the source a change to clean up.
     */
    void destroy();

    /**
     * Returns the delay between messages.
     *
     * @return a long
     */
    long getMessageInterval();

    /**
     * Returns the exchange name.
     *
     * @return exchange, not null
     */
    String getExchange();

    /**
     * Returns the class type or this message, as returned by
     * {@link org.springframework.amqp.support.converter.DefaultClassMapper}
     * @return not null
     */
    String getMessageClass();

    /**
     * Returns the content type that is contained in this source.
     * @return contentType not null
     */
    String getContentType();


    /**
     * Reset this source, the next call to <code>public ReplayableMessage next()</code>
     * should return the first message.
     */
    void reset();

    ReplayableSource NULL = new ReplayableSource() {
        @Override
        public void init() {

        }

        @Override
        public void destroy() {

        }

        @Override
        public long getMessageInterval() {
            return 0;
        }

        @Override
        public String getExchange() {
            return toString();
        }

        @Override
        public void reset() {

        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public ReplayableMessage next() {
            return ReplayableMessage.NULL;
        }

        @Override
        public void remove() {

        }

        @Override
        public String getMessageClass() {
            return "java.lang.Object";
        }

        @Override
        public String getContentType() {
            return "json";
        }

        @Override
        public String toString() {
            return ReplayableSource.class.getSimpleName().concat(".NULL");
        }
    };
}
