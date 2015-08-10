package strata.server.lobby.api.facebook;

import java.io.Serializable;

public class ConversionTrackingData implements Serializable {
    private static final long serialVersionUID = -7947491863508436801L;

    private final String id;
    private final String h;

    public ConversionTrackingData(final String id, final String h) {
        this.id = id;
        this.h = h;
    }

    public String getH() {
        return h;
    }

    public String getId() {
        return id;
    }
}
