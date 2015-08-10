package com.yazino.platform.invitation.service;

import com.yazino.platform.invitation.Invitation;
import com.yazino.platform.invitation.InvitationQueryService;
import com.yazino.platform.invitation.InvitationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service("invitationQueryService")
public class InvitationQueryServiceImpl implements InvitationQueryService {

    private InvitationRepository invitationRepository;

    @Autowired
    public InvitationQueryServiceImpl(final InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    public Set<Invitation> findInvitationsByIssuingPlayer(final BigDecimal playerId) {
        return toDTO(invitationRepository.getInvitationsSentBy(playerId));
    }

    private Set<Invitation> toDTO(final Collection<com.yazino.platform.invitation.persistence.Invitation>
                                          repositoryInvitations) {
        final Set<Invitation> invitations = new HashSet<Invitation>();
        for (com.yazino.platform.invitation.persistence.Invitation repositoryInvitation : repositoryInvitations) {
            invitations.add(toDTO(repositoryInvitation));
        }
        return invitations;
    }

    private Invitation toDTO(final com.yazino.platform.invitation.persistence.Invitation source) {
        BigDecimal chipsEarned = null;
        if (source.getRewardAmount() != null) {
            chipsEarned = BigDecimal.valueOf(source.getRewardAmount());
        }
        return new Invitation(
                source.getIssuingPlayerId(),
                source.getRecipientIdentifier(),
                source.getSource(),
                source.getStatus(),
                source.getCreateTime(),
                source.getUpdateTime(),
                chipsEarned);
    }
}
