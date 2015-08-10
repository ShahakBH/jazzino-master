package com.yazino.platform.session;

public class InvalidPlayerSessionException extends Exception {
	private static final long serialVersionUID = 4243228768192204266L;

	public InvalidPlayerSessionException(final String message) {
		super(message);
	}
}
