package com.yazino.platform.invitation;

import java.math.BigDecimal;
import java.util.Set;

public interface InvitationQueryService {

    Set<Invitation> findInvitationsByIssuingPlayer(BigDecimal playerId);

}
