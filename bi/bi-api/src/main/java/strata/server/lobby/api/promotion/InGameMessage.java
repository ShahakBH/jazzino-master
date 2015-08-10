package strata.server.lobby.api.promotion;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.Serializable;

public class InGameMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String header;
    private String message;

    public InGameMessage(final String header, final String message) {
        this.header = header;
        this.message = message;
    }

    public String getHeader() {
        return header;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
