package strata.server.lobby.promotion.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Wrapper for a player's external id and provider. Note that if the provider is YAZINO,
 * then the external id is actually the player's user id.
 */
public class ExternalCredentials {
    private static final long serialVersionUID = -1365037865712354135L;

    private final String providerName;
    private final String externalId;

    public ExternalCredentials(final String providerName, final String externalId) {
        this.providerName = providerName;
        this.externalId = externalId;
    }

    public String getProviderName() {
        return providerName;
    }

    /**
     * Player's external id if registered via RPX, or lobby user id is registered via yazino.
     * @return the external id
     */
    public String getExternalId() {
        return externalId;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof ExternalCredentials)) {
            return false;
        }
        final ExternalCredentials other = (ExternalCredentials) o;
        return new EqualsBuilder()
                .append(providerName, other.providerName)
                .append(externalId, other.externalId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(providerName).append(externalId).hashCode();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }
}
