package com.yazino.engagement.facebook;

import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.Validate.notNull;

@Component("fbDeleteRequestConsumer")
public class FacebookDeleteRequestConsumer implements QueueMessageConsumer<FacebookDeleteAppRequestMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookDeleteRequestConsumer.class);

    private final FacebookRequestSender facebookRequestSender;

    @Autowired
    public FacebookDeleteRequestConsumer(final FacebookRequestSender facebookRequestSender) {
        notNull(facebookRequestSender, "FacebookRequestSender can not be Null");
        this.facebookRequestSender = facebookRequestSender;
    }

    @Override
    public void handle(final FacebookDeleteAppRequestMessage message) {
        LOG.debug("consuming facebook delete request message" + message);
        facebookRequestSender.deleteRequest(message.getAppRequestExternalReference());
    }
}
