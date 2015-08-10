package senet.server;

import fitlibrary.SetUpFixture;

public class WiredSetUpFixture extends SetUpFixture {
	public WiredSetUpFixture(){
		super();
		FixtureWirer.wire(this);
	}
}
