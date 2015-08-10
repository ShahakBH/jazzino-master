package com.yazino.platform.service.statistic;

import com.yazino.platform.messaging.dispatcher.PlayerDocumentDispatcher;
import com.yazino.platform.model.statistic.Notification;
import com.yazino.platform.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

@Component("notificationService")
public class RabbitMQNotificationService extends ThreadPoolExecutor implements NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQNotificationService.class);
    public static final String DOCUMENT_TYPE = "NEWS_EVENT";
    public static final int KEEP_ALIVE = 60;

    private final JsonHelper jsonHelper = new JsonHelper();
    private final PlayerDocumentDispatcher dispatcher;

    @Autowired
    public RabbitMQNotificationService(@Value("${strata.worker.notification.core-threads}") final int corePoolSize,
                                       @Value("${strata.worker.notification.max-threads}") final int maxPoolSize,
                                       @Qualifier("playerDocumentDispatcher") final PlayerDocumentDispatcher dispatcher) {
        super(corePoolSize, maxPoolSize, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        notNull(dispatcher, "dispatcher is null");
        this.dispatcher = dispatcher;
    }

    @Override
    public void publish(final Notification notification) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Queueing task to publish notification " + notification);
        }
        this.execute(new PublishTask(notification));
    }

    private class PublishTask implements Runnable {
        private final Notification notification;

        public PublishTask(final Notification notification) {
            this.notification = notification;
        }

        @Override
        public void run() {
            final String json = jsonHelper.serialize(notification);
            dispatcher.dispatch(notification.getPlayerId(), DOCUMENT_TYPE, json);
        }


    }
}
