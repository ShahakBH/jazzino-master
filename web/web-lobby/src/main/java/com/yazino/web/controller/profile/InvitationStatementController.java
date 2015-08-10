package com.yazino.web.controller.profile;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.invitation.Invitation;
import com.yazino.platform.invitation.InvitationQueryService;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.invitation.InvitationStatus;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Set;

import static com.yazino.platform.invitation.InvitationStatus.*;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
@RequestMapping("/player/invitations")
public class InvitationStatementController extends AbstractProfileController {

    private static final Logger LOG = LoggerFactory.getLogger(InvitationStatementController.class);

    public static final String TAB_CODE_NAME = "invitations";
    private final LobbySessionCache lobbySessionCache;
    private final InvitationQueryService invitationQueryService;

    @Autowired
    public InvitationStatementController(final LobbySessionCache lobbySessionCache,
                                         final InvitationQueryService invitationQueryService,
                                         final YazinoConfiguration yazinoConfiguration) {

        super(yazinoConfiguration);
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(invitationQueryService, "invitationQueryService may not be null");

        this.lobbySessionCache = lobbySessionCache;
        this.invitationQueryService = invitationQueryService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView displayRootPage(@RequestParam(value = PARTIAL_URI_FLAG,
            defaultValue = PARTIAL_URI_FLAG_DEFAULT,
            required = PARTIAL_URI_FLAG_IS_REQUIRED) final boolean partial,
                                        final HttpServletRequest request) {

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            throw new RuntimeException("No lobby session found");
        }

        final ModelAndView mav = getInitialisedModelAndView(partial);
        final ModelMap modelMap = mav.getModelMap();
        setupDefaultModel(modelMap);
        final ArrayList<InviteeVO> invitees = new ArrayList<InviteeVO>();
        modelMap.addAttribute("invitees", invitees);

        final Set<Invitation> invitations = invitationQueryService.findInvitationsByIssuingPlayer(
                lobbySession.getPlayerId());
        for (Invitation invitation : invitations) {
            invitees.add(toInvitationVO(invitation));
        }
        final InvitationStatement statement = new InvitationStatement(invitations);

        modelMap.addAttribute("totalInvitesSent", invitations.size());
        modelMap.addAttribute("totalChipsEarned", statement.getTotalChipsEarned());
        modelMap.addAttribute("totalInvitesPending", statement.getTotalInvitationsPending());
        modelMap.addAttribute("totalInvitesAccepted", statement.getTotalInvitationsAccepted());

        return mav;
    }

    @SuppressWarnings("unused")
    public class InvitationStatement {

        private final Set<Invitation> invitations;

        public InvitationStatement(final Set<Invitation> invitations) {
            this.invitations = invitations;
        }

        public int getTotalInvitationSent() {
            return invitations.size();
        }

        public int getTotalInvitationsPending() {
            int total = 0;
            for (Invitation invitation : invitations) {
                if (invitation.getStatus() == WAITING || invitation.getStatus() == WAITING_REMINDED) {
                    total++;
                }
            }
            return total;
        }

        public int getTotalInvitationsAccepted() {
            int total = 0;
            for (Invitation invitation : invitations) {
                if (invitation.getStatus() == ACCEPTED) {
                    total++;
                }
            }
            return total;
        }

        public BigInteger getTotalChipsEarned() {
            BigDecimal total = BigDecimal.ZERO;
            for (Invitation invitation : invitations) {
                if (invitation.getStatus() == ACCEPTED) {
                    total = total.add(invitation.getChipsEarned());
                }
            }
            return total.toBigInteger();
        }

    }

    private InviteeVO toInvitationVO(final Invitation invitation) {
        String statusCode = "?";
        String statusText = "?";
        boolean remindable = false;
        switch (invitation.getStatus()) {
            case ACCEPTED:
                statusCode = "accepted";
                statusText = "Accepted";
                break;
            case WAITING:
                statusCode = "pending";
                statusText = "Pending";
                remindable = true;
                break;
            case WAITING_REMINDED:
                statusCode = "reminded";
                statusText = "Pending";
                break;
            default:
                LOG.warn("Unsupported status \"" + invitation.getStatus() + "\".");
        }
        return new InviteeVO(
                invitation.getRecipientIdentifier(),
                invitation.getSource().name().toLowerCase(),
                invitation.getStatus(),
                statusCode,
                statusText,
                invitation.getLastUpdated(),
                remindable,
                invitation.getChipsEarned());
    }

    @Override
    String getCurrentTab() {
        return TAB_CODE_NAME;
    }

    @SuppressWarnings("unused")
    public final class InviteeVO {

        private final String recipientId;
        private final String source;
        private final InvitationStatus status;
        private final String statusCode;
        private final String statusText;
        private final DateTime lastUpdate;
        private final Boolean remindable;
        private final BigDecimal chipsEarned;

        private InviteeVO(final String recipientId,
                          final String source,
                          final InvitationStatus status,
                          final String statusCode,
                          final String statusText,
                          final DateTime lastUpdate,
                          final Boolean remindable,
                          final BigDecimal chipsEarned) {
            this.recipientId = recipientId;
            this.source = source;
            this.status = status;
            this.statusCode = statusCode;
            this.statusText = statusText;
            this.lastUpdate = lastUpdate;
            this.remindable = remindable;
            this.chipsEarned = chipsEarned;
        }

        public String getDisplayName() {
            return recipientId; // TODO handle Facebook, non-facebook pending, non-facebook accepted
        }

        public String getRecipientId() {
            return recipientId;
        }

        public boolean isFacebook() {
            return InvitationSource.FACEBOOK.name().toLowerCase().equals(source);
        }

        public String getSource() {
            return source;
        }

        public String getStatusText() {
            return statusText;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public boolean isRemindable() {
            return remindable;
        }

        public BigDecimal getChipsEarned() {
            return chipsEarned;
        }

        public DateTime getDateUpdated() {
            return lastUpdate;
        }
    }

}
