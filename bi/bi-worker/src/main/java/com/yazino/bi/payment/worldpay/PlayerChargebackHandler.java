package com.yazino.bi.payment.worldpay;

import com.yazino.bi.payment.Chargeback;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.email.AsyncEmailService;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileStatus;
import com.yazino.platform.player.service.PlayerProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class PlayerChargebackHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerChargebackHandler.class);

    private static final String PROPERTY_FROM_ADDRESS = "strata.email.from-address";
    private static final String PROPERTY_SUBJECT = "payment.worldpay.chargeback.email.subject";
    private static final String PROPERTY_TEMPLATE = "payment.worldpay.chargeback.email.template";

    private static final String DEFAULT_FROM_ADDRESS = "contact@yazino.com";
    private static final String DEFAULT_SUBJECT = "{0}, important information about your Yazino account";
    private static final String DEFAULT_TEMPLATE = "backoffice/chargeback.vm";

    private static final String SYSTEM_USER = "system";
    private static final String CHARGEBACK_CREATED = "Chargeback created: %s";

    private final AsyncEmailService emailService;
    private final PlayerProfileService playerProfileService;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public PlayerChargebackHandler(final AsyncEmailService emailService,
                                   final PlayerProfileService playerProfileService,
                                   final YazinoConfiguration yazinoConfiguration) {
        notNull(emailService, "emailService may not be null");
        notNull(playerProfileService, "emailService may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.emailService = emailService;
        this.playerProfileService = playerProfileService;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    public void handleChargeback(Chargeback chargeback) {
        notNull(chargeback, "chargeback may not be null");

        final PlayerProfile playerProfile = playerProfileService.findByPlayerId(chargeback.getPlayerId());
        if (playerProfile == null) {
            LOG.warn("Cannot find player {}; no action taken", chargeback.getPlayerId());
            return;
        }

        if (playerProfile.getStatus() != PlayerProfileStatus.ACTIVE) {
            LOG.debug("Player {} has status {}; no action taken", chargeback.getPlayerId(), playerProfile.getStatus());
            return;
        }

        blockedPlayer(chargeback);
        notifyPlayer(chargeback, playerProfile);
    }

    private void notifyPlayer(final Chargeback chargeback, final PlayerProfile playerProfile) {
        LOG.debug("Notifying player {} of chargeback {} at {}", chargeback.getPlayerId(), chargeback.getReference(), playerProfile.getEmailAddress());
        emailService.send(playerProfile.getEmailAddress(), fromAddress(), subject(playerProfile.getDisplayName()), template(),
                propertiesFrom(chargeback, playerProfile));
    }

    private void blockedPlayer(final Chargeback chargeback) {
        LOG.debug("Blocking player {} for chargeback {}", chargeback.getPlayerId(), chargeback.getReference());
        playerProfileService.updateStatus(chargeback.getPlayerId(), PlayerProfileStatus.BLOCKED, SYSTEM_USER,
                String.format(CHARGEBACK_CREATED, chargeback.getReference()));
    }

    private String fromAddress() {
        return yazinoConfiguration.getString(PROPERTY_FROM_ADDRESS, DEFAULT_FROM_ADDRESS);
    }

    private String subject(final String displayName) {
        return new MessageFormat(yazinoConfiguration.getString(PROPERTY_SUBJECT, DEFAULT_SUBJECT)).format(new Object[]{displayName});
    }

    private String template() {
        return yazinoConfiguration.getString(PROPERTY_TEMPLATE, DEFAULT_TEMPLATE);
    }

    private Map<String, Object> propertiesFrom(final Chargeback chargeback,
                                               final PlayerProfile playerProfile) {
        final HashMap<String, Object> properties = new HashMap<>();
        properties.put("chargebackReference", chargeback.getReference());
        properties.put("displayName", playerProfile.getDisplayName());
        return properties;
    }

}
