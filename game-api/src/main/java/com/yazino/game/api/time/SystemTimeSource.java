package com.yazino.game.api.time;

import java.io.Serializable;

public class SystemTimeSource implements TimeSource, Serializable {
	private static final long serialVersionUID = 5524442679833946864L;

	public long getCurrentTimeStamp() {
		return System.currentTimeMillis();
	}
}
