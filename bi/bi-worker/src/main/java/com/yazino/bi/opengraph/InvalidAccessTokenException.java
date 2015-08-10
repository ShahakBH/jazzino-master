package com.yazino.bi.opengraph;

public class InvalidAccessTokenException extends RuntimeException {
    private static final long serialVersionUID = 857745634760822686L;

    public InvalidAccessTokenException(final String message) {
        super(message);
    }
}
