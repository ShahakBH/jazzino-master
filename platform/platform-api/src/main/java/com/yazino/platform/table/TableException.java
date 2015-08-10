package com.yazino.platform.table;


/**
 * An exception thrown to external clients on a service error.
 * <p/>
 * As this is used externally it must be serialisable using only the API package and other shared code.
 */
public class TableException extends Exception {
    private static final long serialVersionUID = 4521834868534247112L;

    private final TableOperationResult result;

    /**
     * Create a new exception with the given result.
     *
     * @param result the result. May not be null.
     */
    public TableException(final TableOperationResult result) {
        super(result.toString());

        this.result = result;
    }

    public TableOperationResult getResult() {
        return result;
    }
}
