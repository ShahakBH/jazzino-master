package com.yazino.payment.worldpay.nvp;

import com.google.common.base.Charsets;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class NVPMessage implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(NVPMessage.class);

    private static final long serialVersionUID = -2307373163503951415L;

    private static final int CARD_DIGITS_TO_RETAIN = 4;
    private static final char NUMBER_OBSCURED_CHARACTER = 'X';
    private static final String CARD_FIELD_NAME = "AcctNumber";

    private final Map<String, NVPField> fieldsByName = new HashMap<>();
    private final Map<String, String> valuesByFieldName = new TreeMap<>();

    {
        defineField("VersionUsed", NVPType.ALPHANUMERIC, 1, 4, true);
        defineField("MerchantId", NVPType.NUMERIC, true);
        defineField("UserName", NVPType.ALPHANUMERIC, null, 15, true);
        defineField("UserPassword", NVPType.ALPHANUMERIC, null, 15, true);
        defineField("TransactionType", NVPType.ALPHANUMERIC, null, 2, true);
        defineField("IsTest", NVPType.NUMERIC, false);
        defineField("TimeOut", NVPType.NUMERIC, true);
        defineField("StoreID", NVPType.ALPHANUMERIC, null, 10, false);
    }

    public NVPMessage withValue(final String fieldName,
                                final Object fieldValue) {
        return withValue(fieldName, fieldValue, false);
    }

    public NVPMessage withValueIfNotAlreadySet(final String fieldName,
                                               final Object fieldValue) {
        if (hasFieldValue(fieldName)) {
            return this;
        }
        return withValue(fieldName, fieldValue, false);
    }

    public NVPMessage withTruncatedValue(final String fieldName,
                                         final Object fieldValue) {
        return withValue(fieldName, fieldValue, true);
    }

    public boolean hasFieldValue(final String fieldName) {
        return valuesByFieldName.containsKey(fieldName);
    }

    public String getTransactionType() {
        if (valuesByFieldName.containsKey("TransactionType")) {
            return valuesByFieldName.get("TransactionType");
        }
        throw new IllegalStateException("Message has no value for field TransactionType");
    }


    public Integer getCurrencyId() {
        if (valuesByFieldName.containsKey("CurrencyId")) {
            return Integer.parseInt(valuesByFieldName.get("CurrencyId"));
        }
        return null;
    }

    public boolean isValid() {
        if (fieldsByName.isEmpty()) {
            LOG.debug("Message has no fields: {}", this);
            return false;
        }

        for (NVPField field : fieldsByName.values()) {
            if (field.isMandatory() && !valuesByFieldName.containsKey(field.getName())) {
                LOG.debug("Mandatory field {} has no value set", field.getName());
                return false;
            }
        }
        return true;
    }

    public String toObscuredMessage() {
        return fieldsToMessage(fieldsWithObscuredCardValues(), false);
    }

    public String toMessage() {
        return fieldsToMessage(valuesByFieldName, true);
    }

    private String fieldsToMessage(final Map<String, String> fields,
                                   final boolean validate) {
        if (validate && !isValid()) {
            throw new IllegalStateException("Message is invalid");
        }

        final StringBuilder message = new StringBuilder();
        for (String fieldName : fields.keySet()) {
            if (message.length() == 0) {
                message.append("StringIn=");
            } else {
                message.append("~");
            }
            message.append(fieldName).append("^").append(urlEncode(fields.get(fieldName)));
        }

        return message.toString();
    }

    protected NVPMessage defineField(final String name,
                                     final NVPType type,
                                     final boolean mandatory) {
        return defineField(name, type, null, null, mandatory);
    }

    protected NVPMessage defineField(final String name,
                                     final NVPType type,
                                     final Integer minLength,
                                     final Integer maxLength,
                                     final boolean mandatory) {
        fieldsByName.put(name, new NVPField(name, type, minLength, maxLength, mandatory));
        return this;
    }

    private NVPMessage withValue(final String fieldName,
                                 final Object fieldValue,
                                 final boolean truncateIfRequired) {
        notNull(fieldName, "fieldName may not be null");

        final NVPField field = fieldNamed(fieldName);

        final String fieldValueAsString = valueAsString(field, fieldValue, truncateIfRequired);

        validateLengthOf(field, fieldValueAsString);
        validateContentOf(field, fieldValueAsString);

        valuesByFieldName.put(fieldName, fieldValueAsString);
        return this;
    }

    private String urlEncode(final String stringToEncode) {
        try {
            return URLEncoder.encode(stringToEncode, Charsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            LOG.error("The JVM doesn't seem to support UTF-8. All bets are off.", e);
            throw new RuntimeException("The JVM doesn't seem to support UTF-8. All bets are off.", e);
        }
    }

    private String valueAsString(final NVPField field,
                                 final Object fieldValue,
                                 final boolean truncateIfRequired) {
        final String fieldValueAsString = StringUtils.trimToEmpty(ObjectUtils.toString(fieldValue));
        if (fieldValueAsString.length() == 0) {
            throw new IllegalArgumentException("Empty value supplied for field name " + field.getName());
        }

        if (truncateIfRequired && fieldValueAsString.length() > field.getMaxLength()) {
            return fieldValueAsString.substring(0, field.getMaxLength());
        }

        return fieldValueAsString;
    }

    private NVPField fieldNamed(final String fieldName) {
        final NVPField field = fieldsByName.get(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("No field exists with name " + fieldName);
        }
        return field;
    }

    private void validateContentOf(final NVPField field, final String fieldValueAsString) {
        if (!field.getType().validate(fieldValueAsString)) {
            throw new IllegalArgumentException("Field " + field.getName() + " with type " + field.getType()
                    + " cannot accept invalid value '" + fieldValueAsString + "'");
        }
    }

    private void validateLengthOf(final NVPField field, final String fieldValueAsString) {
        if (field.getMinLength() != null && fieldValueAsString.length() < field.getMinLength()) {
            throw new IllegalArgumentException("Value for field " + field.getName() + " must be at least "
                    + field.getMinLength() + " characters, value was: " + fieldValueAsString);
        }

        if (field.getMaxLength() != null && fieldValueAsString.length() > field.getMaxLength()) {
            throw new IllegalArgumentException("Value for field " + field.getName() + " must be at most "
                    + field.getMaxLength() + " characters, value was: " + fieldValueAsString);
        }
    }

    private Map<String, String> fieldsWithObscuredCardValues() {
        if (valuesByFieldName.containsKey(CARD_FIELD_NAME)) {
            final Map<String, String> safeValues = new HashMap<>(valuesByFieldName);
            safeValues.put(CARD_FIELD_NAME, obscureCardNumber(valuesByFieldName.get(CARD_FIELD_NAME)));
            return safeValues;
        }

        return valuesByFieldName;
    }

    private String obscureCardNumber(final String acctNumber) {
        if (acctNumber == null) {
            return null;
        }

        final int obscuredDigits = acctNumber.length() - CARD_DIGITS_TO_RETAIN * 2;
        if (obscuredDigits > 0) {
            return acctNumber.substring(0, CARD_DIGITS_TO_RETAIN)
                    + StringUtils.repeat(NUMBER_OBSCURED_CHARACTER, obscuredDigits)
                    + acctNumber.substring(CARD_DIGITS_TO_RETAIN + obscuredDigits);
        }

        return acctNumber;
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
        final NVPMessage rhs = (NVPMessage) obj;
        return new EqualsBuilder()
                .append(fieldsByName, rhs.fieldsByName)
                .append(valuesByFieldName, rhs.valuesByFieldName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(fieldsByName)
                .append(valuesByFieldName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(fieldsByName)
                .append(fieldsWithObscuredCardValues())
                .toString();
    }

    private class NVPField implements Serializable {
        private static final long serialVersionUID = 9082595558382424017L;

        private final String name;
        private final NVPType type;
        private final Integer minLength;
        private final Integer maxLength;
        private final boolean mandatory;

        public NVPField(final String name,
                        final NVPType type,
                        final Integer minLength,
                        final Integer maxLength,
                        final boolean mandatory) {
            notNull(name, "name may not be null");
            notNull(type, "type may not be null");

            this.name = name;
            this.type = type;
            this.minLength = minLength;
            this.maxLength = maxLength;
            this.mandatory = mandatory;
        }

        private String getName() {
            return name;
        }

        private NVPType getType() {
            return type;
        }

        private Integer getMinLength() {
            return minLength;
        }

        private Integer getMaxLength() {
            return maxLength;
        }

        private boolean isMandatory() {
            return mandatory;
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
            final NVPField rhs = (NVPField) obj;
            return new EqualsBuilder()
                    .append(name, rhs.name)
                    .append(type, rhs.type)
                    .append(minLength, rhs.minLength)
                    .append(maxLength, rhs.maxLength)
                    .append(mandatory, rhs.mandatory)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(name)
                    .append(type)
                    .append(minLength)
                    .append(maxLength)
                    .append(mandatory)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append(name)
                    .append(type)
                    .append(minLength)
                    .append(maxLength)
                    .append(mandatory)
                    .toString();
        }
    }
}
