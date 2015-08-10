package com.yazino.web.service;

import com.yazino.configuration.YazinoConfiguration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class InvitationLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(InvitationLimiter.class);
    private final HashMap<BigDecimal, List<DateTime>> invitationsPerUser = new HashMap<BigDecimal, List<DateTime>>();
    private final HashMap<String, List<DateTime>> invitationsPerIp = new HashMap<String, List<DateTime>>();

    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public InvitationLimiter(final YazinoConfiguration yazinoConfiguration) {
        notNull(yazinoConfiguration, "yazinoConfiguration is null");
        this.yazinoConfiguration = yazinoConfiguration;
    }

    public boolean canSendInvitations(final int invitationsToSend,
                                      final BigDecimal userId,
                                      final String ip) {
        final int totalInvitationsForUser =
                sentInvitationsCountFor(userId, invitationsPerUser, getFromConfig("perUser.hours"));
        final int totalInvitationsForIp =
                sentInvitationsCountFor(ip, invitationsPerIp, getFromConfig("perIp.hours"));

        return isWithinLimit(totalInvitationsForUser + invitationsToSend, getFromConfig("perUser.maxAttempts"))
                && isWithinLimit(totalInvitationsForIp + invitationsToSend, getFromConfig("perIp.maxAttempts"));
    }

    private boolean isWithinLimit(final int totalInvitationsPerUser, final int limit) {
        return (limit == -1 || totalInvitationsPerUser <= limit);
    }

    public void hasSentInvitations(final int numOfInvitations, final BigDecimal userId, final String ip) {
        putInvitationInList(numOfInvitations, userId, invitationsPerUser);
        putInvitationInList(numOfInvitations, ip, invitationsPerIp);
    }

    private <T> int sentInvitationsCountFor(final T key,
                                            final Map<T, List<DateTime>> list,
                                            final int hoursBeforeExpiry) {
        final List<DateTime> items = list.get(key);
        if (items == null || items.isEmpty()) {
            return 0;
        }

        removeOldItems(hoursBeforeExpiry, items);

        return items.size();
    }

    private void removeOldItems(final int hoursBeforeExpiry, final List<DateTime> items) {
        final long minTime = new DateTime().minusHours(hoursBeforeExpiry).getMillis();

        final Iterator<DateTime> datesOfInvitationSends = items.iterator();
        while (datesOfInvitationSends.hasNext()) {
            final DateTime dt = datesOfInvitationSends.next();
            if (dt.getMillis() <= minTime) {
                datesOfInvitationSends.remove();
            }
        }
    }

    private <T> void putInvitationInList(final int numOfInvitations,
                                         final T key,
                                         final Map<T, List<DateTime>> list) {
        List<DateTime> usersInvitations = list.get(key);
        if (usersInvitations == null) {
            usersInvitations = new ArrayList<DateTime>();
            list.put(key, usersInvitations);
        }
        final DateTime currentTime = new DateTime();
        for (int i = 0; i < numOfInvitations; i++) {
            usersInvitations.add(currentTime);
        }
    }

    private int getFromConfig(final String keyPart) {
        try {
            return yazinoConfiguration.getInt("invitationLimiter." + keyPart);
        } catch (NoSuchElementException exception) {
            LOG.warn("Invitation limiter tried to lookup [{}] but no key is defined, defaulting to -1 (unlimited)",
                    exception);
            return -1;
        }
    }

}
