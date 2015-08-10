package senet.server.community;

import com.yazino.platform.community.RelationshipType;
import fit.ColumnFixture;
import com.yazino.platform.community.RelationshipAction;

public class AllowedActions extends ColumnFixture{
	public RelationshipType playerAppearsAs;
	public boolean isOnline=true;
	public RelationshipAction[] allowedActions(){
		return playerAppearsAs.getAllowedActions(isOnline);
	}
}
