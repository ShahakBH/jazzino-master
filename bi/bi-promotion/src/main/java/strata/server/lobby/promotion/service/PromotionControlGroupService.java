package strata.server.lobby.promotion.service;

import com.yazino.platform.player.PlayerProfile;
import strata.server.lobby.api.promotion.Promotion;

public interface PromotionControlGroupService {

    boolean isControlGroupMember(PlayerProfile userProfile, Promotion promotion);
}
