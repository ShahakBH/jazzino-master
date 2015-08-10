package com.yazino.bi.opengraph;

import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.yazino.bi.opengraph.AccessTokenStore.AccessToken;

@Component
@Qualifier("openGraphCredentialsEventConsumer")
public class OpenGraphCredentialsEventConsumer implements QueueMessageConsumer<OpenGraphCredentialsMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(OpenGraphCredentialsEventConsumer.class);

    private final AccessTokenStore accessTokenStore;

    @Autowired
    public OpenGraphCredentialsEventConsumer(final AccessTokenStore accessTokenStore) {
        this.accessTokenStore = accessTokenStore;
    }

    @Override
    public void handle(final OpenGraphCredentialsMessage message) {
        try {
            LOG.debug("Storing access token: {}", message);
            this.accessTokenStore.storeAccessToken(
                    new AccessTokenStore.Key(message.getPlayerId(), message.getGameType()), new AccessToken(message.getAccessToken()));
        } catch (Exception e) {
            LOG.error("Unable to process event.", e);
        }
    }
}
