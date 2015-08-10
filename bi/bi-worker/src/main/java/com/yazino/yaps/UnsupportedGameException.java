package com.yazino.yaps;

/**
 * An exception raised when attempting to process an unsupported game.
 */
public class UnsupportedGameException extends Exception {

    private static final long serialVersionUID = 3160610155024124083L;

    public UnsupportedGameException(final String gameType) {
        super(String.format("Game [%s] not supported", gameType));
    }
}
