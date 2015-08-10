package com.yazino.web.service;

import com.yazino.web.domain.RegistrationRecord;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class HourlyRegistrations {

    private final Ehcache cache;

    @Autowired
    public HourlyRegistrations(@Qualifier("registrationsPerIpCache") final Ehcache cache) {
        this.cache = cache;
    }

    public int getRegistrationsFrom(final String ipAddress) {
        final DateTime currentTime = new DateTime();
        final RegistrationRecord record = getRecordFor(ipAddress);
        if (record == null || registrationHappenedOnPreviousHour(currentTime, record)) {
            return 0;
        }
        return record.getRegistrations();
    }

    public void incrementRegistrationsFrom(final String ipAddress) {
        final DateTime currentTime = new DateTime();
        RegistrationRecord record = getRecordFor(ipAddress);
        if (record == null || registrationHappenedOnPreviousHour(currentTime, record)) {
            record = new RegistrationRecord(1, currentTime);
        } else {
            record = new RegistrationRecord(record.getRegistrations() + 1, record.getEarliestRegistrationTime());
        }
        cache.put(new Element(ipAddress, record));
    }

    private RegistrationRecord getRecordFor(final String ipAddress) {
        final Element element = cache.get(ipAddress);
        if (element == null) {
            return null;
        }
        return (RegistrationRecord) element.getValue();
    }

    private boolean registrationHappenedOnPreviousHour(final DateTime currentTime, final RegistrationRecord record) {
        return record.getEarliestRegistrationTime().getMillis()
                < currentTime.withMinuteOfHour(0).withSecondOfMinute(0).getMillis();
    }
}
