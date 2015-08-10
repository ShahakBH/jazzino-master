package com.yazino.platform.model.table;

public enum TableControlMessageType {
    LOAD,
    UNLOAD,

    /**
     * Ask the table to close when the game next allows it.
     */
    CLOSE,

    /**
     * Close the table and shutdown the game immediately.
     */
    SHUTDOWN,

    REOPEN,
    RESET
}
