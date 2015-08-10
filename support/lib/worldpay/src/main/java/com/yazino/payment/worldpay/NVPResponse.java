package com.yazino.payment.worldpay;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class NVPResponse implements Serializable {
    private static final long serialVersionUID = -7010294140075576172L;

    private static final Logger LOG = LoggerFactory.getLogger(NVPResponse.class);

    private final Map<String, String> valuesByFieldName = new HashMap<String, String>();
    private final String requestString;
    private final String responseString;

    public NVPResponse(final String requestString,
                       final String responseString) {
        this.requestString = requestString;
        this.responseString = responseString;

        parse(responseString);
    }

    public boolean isEmpty() {
        return valuesByFieldName.isEmpty();
    }

    public Optional<String> get(final String fieldName) {
        return Optional.fromNullable(valuesByFieldName.get(fieldName));
    }

    public Optional<Long> getLong(final String fieldName) {
        final String value = valuesByFieldName.get(fieldName);
        if (value != null) {
            return Optional.of(Long.valueOf(value));
        }
        return Optional.absent();
    }

    public Optional<BigDecimal> getBigDecimal(final String fieldName) {
        final String value = valuesByFieldName.get(fieldName);
        if (value != null) {
            return Optional.of(new BigDecimal(value));
        }
        return Optional.absent();
    }

    public String getResponseString() {
        return responseString;
    }

    public String getRequestString() {
        return requestString;
    }

    private void parse(final String unparsedResponse) {
        LOG.debug("Parsing response: {}", unparsedResponse);

        for (String field : StringUtils.trimToEmpty(unparsedResponse).split("~")) {
            if (field.isEmpty()) {
                continue;
            }

            final String[] fieldParts = field.split("\\^");
            if (fieldParts.length == 2) {
                valuesByFieldName.put(urlDecode(fieldParts[0].trim()), urlDecode(fieldParts[1].trim()));

            } else {
                LOG.debug("Malformed field, skipping: {}", fieldParts);
            }
        }
    }

    private String urlDecode(final String encodedString) {
        try {
            return URLDecoder.decode(encodedString, Charsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            LOG.error("The JVM doesn't seem to support UTF-8. All bets are off.", e);
            throw new RuntimeException("The JVM doesn't seem to support UTF-8. All bets are off.", e);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final NVPResponse rhs = (NVPResponse) obj;
        return new EqualsBuilder()
                .append(valuesByFieldName, rhs.valuesByFieldName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(valuesByFieldName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(valuesByFieldName)
                .toString();
    }
}
