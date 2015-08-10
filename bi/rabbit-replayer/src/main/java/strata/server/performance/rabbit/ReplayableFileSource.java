package strata.server.performance.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.Assert.notNull;

/**
 * Uses content from a file as its source for messages.
 */
public class ReplayableFileSource implements ReplayableSource {
    private static final Logger LOG = LoggerFactory.getLogger(ReplayableFileSource.class);

    private final List<ReplayableMessage> lines = new ArrayList<ReplayableMessage>();
    private final String filePath;
    private String exchange = "";
    private long messageInterval = 0L;
    private String messageClass = null;
    private String contentType = "json";
    private int lineIndex = 0;

    public ReplayableFileSource(final String filePath) {
        notNull(filePath, "filePath was null");
        this.filePath = filePath;
    }

    @Override
    public void init() {
        LOG.info(String.format("Initializing [%s] from file [%s]", getClass().getSimpleName(), filePath));
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            final String config = reader.readLine();
            final String[] parts = config.split(",");
            exchange = parts[0];
            messageInterval = Long.parseLong(parts[1]);
            contentType = parts[2];
            messageClass = parts[3];
            String line;
            while ((line = reader.readLine()) != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Read line [%s]", line));
                }
                final ReplayableMessage message = new LineReplayableMessage(line);
                lines.add(message);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            QuietCloser.closeQuietly(reader);
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public String getExchange() {
        return exchange;
    }

    @Override
    public long getMessageInterval() {
        return messageInterval;
    }

    @Override
    public String getMessageClass() {
        return messageClass;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean hasNext() {
        return lines.size() > lineIndex;
    }

    @Override
    public ReplayableMessage next() {
        return lines.get(lineIndex++);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove not supported");
    }

    @Override
    public void reset() {
        lineIndex = 0;
    }

    public String toString() {
        return String.format("%s using file [%s]", getClass().getSimpleName(), filePath);
    }

    private static class LineReplayableMessage implements ReplayableMessage {
        private final String routingKey;
        private final byte[] body;

        public LineReplayableMessage(final String line) throws UnsupportedEncodingException {
            final int keyEnd = line.indexOf(',');
            routingKey = line.substring(0, keyEnd);
            body = line.substring(keyEnd + 1).getBytes("UTF-8");
        }

        @Override
        public byte[] getBody() {
            return body;
        }

        @Override
        public String getRoutingKey() {
            return routingKey;
        }

        @Override
        public String getID() {
            // TODO get from file...
            return null;
        }
    }
}
