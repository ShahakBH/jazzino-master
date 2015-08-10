package com.yazino.web.payment;


import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TransactionIdGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionIdGenerator.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssSSS");
    private static final AtomicLong ATOMIC_LONG = new AtomicLong();
    private static final Object NUMERIC_LOCK = new Object();

    private static final long MACHINE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    private static final long EPOCH = 1288834974657L;

    private InetAddress localhost;

    private long sequence = 0L;
    private long lastTimeStamp = -1L;
    private long machineId;

    public TransactionIdGenerator() {
        try {
            localhost = Inet4Address.getLocalHost();
            machineId = localhost.getAddress()[3] & 0xFF;

        } catch (UnknownHostException e) {
            LOG.error("Unable to lookup IPv4 hostname for localhost", e);
        }
    }

    public String generateTransactionId(final BigDecimal accountId) {
        final DateTime dtLondon = new DateTime(DateTimeZone.forID("Europe/London"));

        return String.format("%s_%s_%s_%d", accountId.toPlainString(), DATE_TIME_FORMATTER.print(dtLondon),
                localhost.getHostAddress(), ATOMIC_LONG.getAndIncrement());
    }

    /**
     * Generates a 64bit numeric transaction ID.
     *
     * @return the transaction ID.
     */
    public long generateNumericTransactionId() {
        /*
            This generates a numeric ID for transaction, for systems that can't take the full
            length of the textual ID above. It's a simplified version of the Twitter Snowflake.
            In particular, we merge the datacentre & worker IDs into a single 10 bit field and populate
            it from the local IPv4 subnet. This will obviously only work while we play in a single /24.

            For a complete implementation see https://github.com/twitter/snowflake
         */

        synchronized (NUMERIC_LOCK) {
            long timestamp = DateTimeUtils.currentTimeMillis();
            if (lastTimeStamp == timestamp) {
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) {
                    timestamp = untilNextMillis(lastTimeStamp);
                }
            } else {
                sequence = 0;
            }

            lastTimeStamp = timestamp;
            return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                    | (machineId << MACHINE_ID_SHIFT)
                    | sequence;
        }
    }

    private long untilNextMillis(final long endTimeStamp) {
        long timestamp = DateTimeUtils.currentTimeMillis();
        while (timestamp <= endTimeStamp) {
            timestamp = DateTimeUtils.currentTimeMillis();
        }
        return timestamp;
    }
}
