package com.yazino.platform.tournament;

/**
 * An exception thrown to external clients on a service error.
 * <p/>
 * As this is used externally it must be serialisable using only the API package and other shared code.
 */
public class TournamentException extends Exception {
	private static final long serialVersionUID = 4521702868578047112L;

	private final TournamentOperationResult result;

	/**
	 * Create a new exception with the given result.
	 *
	 * @param result the result. May not be null.
	 */
	public TournamentException(final TournamentOperationResult result) {
		super(result.toString());

		this.result = result;
	}

	public TournamentOperationResult getResult() {
		return result;
	}
}
