package strata.server.performance.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.springframework.util.Assert.notNull;

/**
 * This class will read a file in and publish its contents to a rabbit queue.
 */
public class ReplayPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(ReplayPublisher.class);

    private final AmqpTemplate mTemplate;
    private ReplayableSource mSource = ReplayableSource.NULL;

    public ReplayPublisher(final AmqpTemplate template) {
        notNull(template);
        mTemplate = template;
    }

    public void start() throws Exception {
        LOG.info(String.format("Starting replay using template [%s] and source [%s]", mTemplate, mSource));

        final SourceReplayer replayer = new SourceReplayer(mTemplate, mSource);
        final Thread t = new Thread(replayer);
        t.start();
        t.join();

        LOG.info("Finished replay");
    }

    @Autowired(required = true)
    public ReplayableSource getSource() {
        return mSource;
    }

    @Autowired(required = true)
    public void setSource(final ReplayableSource source) {
        notNull(source);
        mSource = source;
    }

    public static void main(final String[] args) throws Exception {
        if (args.length != 1) {
            LOG.error("Invalid number of arguments, expected <filename>");
            return;
        }

        final String fileSource = args[0];
        final ApplicationContext context = new ClassPathXmlApplicationContext("rabbit-replayer-spring.xml");
        final ReplayPublisher replayPublisher = (ReplayPublisher) context.getBean("replayer");
        final ReplayableFileSource source = new ReplayableFileSource(fileSource);
        replayPublisher.setSource(source);
        replayPublisher.start();
        System.exit(0);
    }

}
