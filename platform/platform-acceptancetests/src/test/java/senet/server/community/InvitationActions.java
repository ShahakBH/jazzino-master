package senet.server.community;

import fit.ColumnFixture;
import com.yazino.platform.service.community.PlayerWorker;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.community.RelationshipType;

public class InvitationActions extends ColumnFixture{

	public RelationshipAction invitationActionByA;
	
	PlayerWorker pw=new PlayerWorker();
	
	public RelationshipType bAppearsToAafterAction(){
		return pw.calculateNewRelationshipType(RelationshipType.INVITATION_RECEIVED, invitationActionByA,false);
	}
	public RelationshipType aAppearsToBafterAction(){
		return pw.calculateNewRelationshipType(RelationshipType.INVITATION_SENT, invitationActionByA,true);
	}
}
