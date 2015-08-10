package strata.server.lobby.api.facebook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FacebookDataContainerBuilder {
    private FacebookAppToUserRequestType type;
    private Map<String, Object> tracking;
    private List<String> actions;

    public FacebookDataContainerBuilder withType(final FacebookAppToUserRequestType newType) {
        this.type = newType;
        return this;
    }

    public FacebookDataContainerBuilder withTrackingData(final Map<String, Object> newTracking) {
        this.tracking = newTracking;
        return this;
    }

    public FacebookDataContainerBuilder withTrackingRef(final String trackingRef) {
        if (tracking == null) {
            tracking = new HashMap<String, Object>();
        }
        tracking.put(FacebookDataContainer.TRACKING_REF_DATA_KEY, trackingRef);
        return this;
    }

    public FacebookDataContainerBuilder withActions(final List<String> newActions) {
        this.actions = newActions;
        return this;
    }

    public FacebookDataContainer build() {
        return new FacebookDataContainer(type, tracking, actions);
    }
}
