package com.yazino.platform.messaging.host.format;

public enum HostDocumentHeader {

    MESSAGE("message", false),
    TABLE_PROPERTIES("tableProperies", false),
    GAME_ID("gameId", true),
    COMMAND_UUID("commandUUID", true),
    CHANGES("changes", true),
    WARNINGS("warnings", true),
    IS_A_PLAYER("isAPlayer", true);

    private final String header;
    private final boolean insertQuotes;

    private HostDocumentHeader(final String header,
                               final boolean insertQuotes) {
        this.header = header;
        this.insertQuotes = insertQuotes;
    }

    public String getHeader() {
        return header;
    }

    public void appendTo(final StringBuilder sb,
                         final String value) {
        sb.append("\"").append(header).append("\":");
        if (insertQuotes) {
            sb.append("\"");
            if (value != null) {
                sb.append(value.replaceAll("\\\"", "\\\\\\\""));
            } else {
                sb.append(value);
            }
            sb.append("\"");
        } else {
            sb.append(value);
        }
    }

    public void appendTo(final StringBuilder sb,
                         final long value) {
        sb.append("\"").append(header).append("\":");
        if (insertQuotes) {
            sb.append("\"");
        }
        sb.append(value);
        if (insertQuotes) {
            sb.append("\"");
        }
    }
}
