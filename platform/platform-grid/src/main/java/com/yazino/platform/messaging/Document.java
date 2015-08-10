package com.yazino.platform.messaging;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class Document implements Serializable {
    private static final long serialVersionUID = -4531189375652082536L;

    private final Map<String, String> headers;
    private final String type;
    private final String body;
    private final String encoding;
    private final DateTime ts;

    /**
     * @param type    type to be sent in the header
     * @param body    object to be sent to the recipients
     * @param headers additional headers for routing
     */
    public Document(final String type,
                    final String body,
                    final Map<String, String> headers) {
        this(type, body, headers, null);
    }

    /**
     * @param type     type to be sent in the header
     * @param body     object to be sent to the recipients
     * @param headers  additional headers for routing
     * @param encoding the content encoding.
     */
    public Document(final String type,
                    final String body,
                    final Map<String, String> headers,
                    final String encoding) {
        this.headers = new HashMap<String, String>(headers);
        this.type = type;
        this.body = body;
        this.encoding = encoding;
        this.ts = new DateTime();
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getType() {
        return type;
    }

    public String getBody() {
        return body;
    }

    public DateTime getTs() {
        return ts;
    }

    public String getEncoding() {
        return encoding;
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
        final Document rhs = (Document) obj;
        return new EqualsBuilder()
                .append(body, rhs.body)
                .append(type, rhs.type)
                .append(headers, rhs.headers)
                .append(encoding, rhs.encoding)
                        // Timestamp is intentionally omitted
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(body)
                .append(type)
                .append(headers)
                .append(encoding)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(body)
                .append(type)
                .append(headers)
                .append(encoding)
                .append(ts)
                .toString();
    }
}
