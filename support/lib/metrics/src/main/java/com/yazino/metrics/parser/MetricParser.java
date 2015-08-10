package com.yazino.metrics.parser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.yazino.metrics.model.Meter;
import com.yazino.metrics.model.MeterBuilder;
import com.yazino.metrics.repository.MetricRepository;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class MetricParser {
    private static final Logger LOG = LoggerFactory.getLogger(MetricParser.class);

    private final JsonFactory jsonFactory = new JsonFactory();
    private final MetricRepository repository;

    @Autowired
    public MetricParser(final MetricRepository repository) {
        notNull(repository, "repository may not be null");

        this.repository = repository;
    }

    public void parse(final InputStream streamToParse) throws IOException {
        notNull(streamToParse, "steamToParse may not be null");

        String clientId = null;
        final Map<String, Meter> meters = new HashMap<>();

        try (final JsonParser parser = jsonFactory.createParser(streamToParse)) {
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                final String childName = parser.getCurrentName();
                if (childName == null) {
                    continue;
                }

                switch (childName) {
                    case "clientId":
                        parser.nextToken();
                        clientId = parser.getText();
                        break;
                    case "meters":
                        parser.nextToken();
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            parseMeter(parser, meters);
                        }
                        break;

                    default:
                        throw new IllegalStateException("Unrecognised field '" + childName + "'!");
                }
            }
        }

        if (!meters.isEmpty() && clientId == null) {
            throw new IllegalStateException("Message did not contain a client ID");
        }

        for (Map.Entry<String, Meter> meterEntry : meters.entrySet()) {
            LOG.debug("Updating meter for client {}: {} - {}", clientId, meterEntry.getKey(), meterEntry.getValue());
            repository.save(meterEntry.getKey(), clientId, meterEntry.getValue());
        }

    }

    private void parseMeter(final JsonParser parser,
                            final Map<String, Meter> meters) throws IOException {
        String name = null;
        MeterBuilder meterBuilder = MeterBuilder.newMeter();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            final String fieldName = parser.getCurrentName();
            if (fieldName == null) {
                continue;
            }

            parser.nextToken();
            switch (fieldName) {
                case "name":
                    name = parser.getText();
                    break;
                case "timestamp":
                    meterBuilder = meterBuilder.withTimestamp(new DateTime(parser.getLongValue()));
                    break;
                case "count":
                    meterBuilder = meterBuilder.withCount(parser.getIntValue());
                    break;
                case "m1Rate":
                    meterBuilder = meterBuilder.withOneMinuteRatePerSecond(parser.getDoubleValue());
                    break;
                case "m5Rate":
                    meterBuilder = meterBuilder.withFiveMinuteRatePerSecond(parser.getDoubleValue());
                    break;
                case "m15Rate":
                    meterBuilder = meterBuilder.withFifteenMinuteRatePerSecond(parser.getDoubleValue());
                    break;
                case "meanRate":
                    meterBuilder = meterBuilder.withMeanRatePerSecond(parser.getDoubleValue());
                    break;
                default:
                    throw new IllegalStateException("Unrecognised field '" + fieldName + "'!");
            }
        }

        if (name != null) {
            meters.put(name, meterBuilder.build());
        }
    }
}
