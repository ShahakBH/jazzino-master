package com.yazino.platform.table;

import java.io.Serializable;

// if you update these make sure you update the TABLE_STATUS table in the DB

public enum TableStatus implements Serializable {
    open("O"), closed("C"), closing("S"), error("E");

    private String statusName;

    private TableStatus(final String status) {
        this.statusName = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public static TableStatus forStatusName(final String status) {
        for (TableStatus tableStatus : values()) {
            if (tableStatus.statusName.equals(status)) {
                return tableStatus;
            }
        }
        throw new IllegalArgumentException(status);
    }

    public static TableStatus parse(final String value) {
        return TableStatus.valueOf(value);
    }
}
