package strata.server.lobby.api.facebook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FacebookDataContainer {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookDataContainer.class);
    public static final String TRACKING_REF_DATA_KEY = "ref";
//        tracking,
//    buyChipsPopup,
//    ref,
//    type

    private FacebookAppToUserRequestType type;

    private Map<String, Object> tracking;

    private List<String> actions;

    public FacebookDataContainer() {
    }

    public FacebookDataContainer(final FacebookAppToUserRequestType type,
                                 final Map<String, Object> tracking,
                                 final List<String> actions) {
        this.type = type;
        this.tracking = tracking;
        this.actions = actions;
    }


    public FacebookAppToUserRequestType getType() {
        return type;
    }

    public void setType(final FacebookAppToUserRequestType type) {
        this.type = type;
    }

    public Map<String, Object> getTracking() {
        return tracking;
    }

    public void setTracking(final Map<String, Object> tracking) {
        this.tracking = tracking;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(final List<String> actions) {
        this.actions = actions;
    }

    public String toJsonString() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        String trackingDataJson = null;
        try {
            trackingDataJson = mapper.writeValueAsString(this);
        } catch (IOException e) {
            LOG.warn("Could not serialise object into Json");
        }

        return trackingDataJson;

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
        final FacebookDataContainer rhs = (FacebookDataContainer) obj;
        return new EqualsBuilder()
                .append(type, rhs.type)
                .append(tracking, rhs.tracking)
                .append(actions, rhs.actions)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(type)
                .append(tracking)
                .append(actions)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(type)
                .append(tracking)
                .append(actions)
                .toString();
    }
}
