package senet.server;

import fitlibrary.DoFixture;

public class WiredDoFixture extends DoFixture {
	protected static String DEALER_NAME = "DEALER";
	protected static String PLAYER_NAME = "PLAYER";
	public WiredDoFixture(){
		super();
		FixtureWirer.wire(this);
	}
}
