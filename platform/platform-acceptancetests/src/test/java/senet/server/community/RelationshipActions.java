package senet.server.community;

import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.service.community.PlayerWorker;
import fit.ColumnFixture;

public class RelationshipActions extends ColumnFixture{
	public String comment;
	public RelationshipType bAppearsToA;
	public RelationshipType aAppearsToB;
	public RelationshipAction actionByA;
	PlayerWorker pw=new PlayerWorker();
	public RelationshipType bAppearsToAafterAction(){
		return pw.calculateNewRelationshipType(bAppearsToA, actionByA,false);
	}
	public RelationshipType aAppearsToBafterAction(){
		return pw.calculateNewRelationshipType(aAppearsToB, actionByA,true);	
	}
	
}
