package senet.server;

import fit.ColumnFixture;

public class WiredColumnFixture extends ColumnFixture {
	public WiredColumnFixture() {
		super();
		FixtureWirer.wire(this);
	}
}
