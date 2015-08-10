package strata.server.worker.event.consumer;

import com.yazino.engagement.mobile.MobileDeviceService;
import com.yazino.platform.android.MessagingDeviceRegistrationEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("messagingDeviceRegistrationEventConsumer")
public class MessagingDeviceRegistrationEventConsumer implements QueueMessageConsumer<MessagingDeviceRegistrationEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingDeviceRegistrationEventConsumer.class);

    private final MobileDeviceService mobileDeviceDao;

    @Autowired
    public MessagingDeviceRegistrationEventConsumer(MobileDeviceService mobileDeviceDao) {
        this.mobileDeviceDao = mobileDeviceDao;
    }

    @Override
    public void handle(final MessagingDeviceRegistrationEvent message) {
        try {
            mobileDeviceDao.register(message.getPlayerId(), message.getGameType(), message.getPlatform(),
                    message.getAppId(), message.getDeviceId(), message.getRegistrationId());
        } catch (Exception e) {
            LOG.error("Unable to handle device registration message: {}", message, e);
        }
    }
}
