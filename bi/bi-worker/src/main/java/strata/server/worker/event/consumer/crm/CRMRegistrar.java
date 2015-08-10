package strata.server.worker.event.consumer.crm;

import com.yazino.engagement.email.application.EmailApi;
import com.yazino.engagement.email.domain.EmailData;
import com.yazino.platform.event.message.PlayerProfileEvent;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.yazino.engagement.email.domain.EmailVisionRestParams.DISPLAY_NAME;

@Service("crmRegistrar")
public class CRMRegistrar {
    private static final Logger LOG = LoggerFactory.getLogger(CRMRegistrar.class);

    @Autowired
    private EmailApi emailApi;

    @Async
    public void register(final PlayerProfileEvent playerProfile) {
        Validate.notNull(emailApi, "emailApi should not be null");
        LOG.debug("Sending registration email to user : {}, {}", playerProfile.getDisplayName(), playerProfile.getEmail());

        try {
            if (playerProfile.getEmail() != null) {
                if (emailApi.sendDayZeroEmail(emailDataFor(playerProfile))) {
                    LOG.debug("Registration email sent for player {}", playerProfile.getPlayerId());
                }
            } else {
                LOG.debug("Registration email will not be sent for player as they have no email address: {}", playerProfile.getPlayerId());
            }

        } catch (Exception e) {
            LOG.error("Registration email threw exception for user {}", playerProfile, e);
        }

    }

    private EmailData emailDataFor(final PlayerProfileEvent playerProfile) {
        Map<String, String> dynamicKeys = new LinkedHashMap<String, String>();
        dynamicKeys.put(DISPLAY_NAME.toString(), playerProfile.getDisplayName());

        return new EmailData(playerProfile.getEmail(), playerProfile.getRegistrationTime(), dynamicKeys);
    }
}
