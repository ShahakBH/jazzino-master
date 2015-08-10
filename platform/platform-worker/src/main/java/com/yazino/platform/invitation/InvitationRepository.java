package com.yazino.platform.invitation;

import com.yazino.platform.event.message.InvitationEvent;
import com.yazino.platform.invitation.persistence.Invitation;
import com.yazino.platform.invitation.persistence.JDBCInvitationDAO;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("invitationRepository")
public class InvitationRepository {

    private final JDBCInvitationDAO dao;
    private final QueuePublishingService<InvitationEvent> invitationEventPublisher;

    @Autowired
    public InvitationRepository(final JDBCInvitationDAO dao,
                                @Qualifier("invitationEventQueuePublishingService")
                                final QueuePublishingService<InvitationEvent> invitationEventPublisher) {
        notNull(dao, "dao is null");
        notNull(invitationEventPublisher, "invitationEventPublisher is null");
        this.dao = dao;
        this.invitationEventPublisher = invitationEventPublisher;
    }

    public void save(final Invitation invitation) {
        notNull(invitation, "invitation is null");
        dao.save(invitation);
        invitationEventPublisher.send(invitation.toEvent());
    }

    public Collection<Invitation> getInvitationsSentTo(final String recipient, final InvitationSource source) {
        notNull(recipient, "recipient is null");
        notNull(source, "source is null");
        return dao.getInvitations(recipient, source);
    }

    public int getNumberOfAcceptedInvites(final BigDecimal issuingPlayerId, final DateTime sinceCreateTime) {
        return dao.getNumberOfAcceptedInvites(issuingPlayerId, sinceCreateTime);
    }

    public Collection<Invitation> getInvitationsSentBy(final BigDecimal issuingPlayerId) {
        notNull(issuingPlayerId, "issuingPlayerId is null");
        return dao.findInvitationsByIssuingPlayerId(issuingPlayerId);
    }

    public Invitation getInvitationByIssuingPlayerRecipientAndSource(final BigDecimal issuingPlayerId,
                                                                     final String recipientIdentifier,
                                                                     final InvitationSource source) {
        notNull(issuingPlayerId, "issuingPlayerId is null");
        notNull(recipientIdentifier, "recipientIdentifier is null");
        notNull(source, "source is null");
        return dao.findInvitationsByIssuingPlayerRecipientAndSource(issuingPlayerId, recipientIdentifier, source);
    }
}
