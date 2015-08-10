package com.yazino.web.api;

import com.yazino.platform.invitation.InvitationService;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.web.data.GameTypeRepository;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.SpringErrorResponseFormatter;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * An endpoint to track invitations that have been sent from any supported {@link InvitationSource}.
 */
@Controller
@RequestMapping("/api/1.0/invitation/*")
public class InvitationRecordingController {
    private static final Logger LOG = LoggerFactory.getLogger(InvitationRecordingController.class);

    private final LobbySessionCache lobbySessionCache;
    private final InvitationService invitationService;
    private final InvitationSentRecordValidator validator;
    private final WebApiResponses webApiResponses;
    private final SpringErrorResponseFormatter springErrorResponseFormatter;

    @Autowired
    public InvitationRecordingController(final LobbySessionCache lobbySessionCache,
                                         final InvitationService invitationService,
                                         final GameTypeRepository gameTypeRepository,
                                         final WebApiResponses webApiResponses,
                                         final SpringErrorResponseFormatter springErrorResponseFormatter) {
        Validate.noNullElements(new Object[]{lobbySessionCache, invitationService, gameTypeRepository, webApiResponses, springErrorResponseFormatter});
        this.lobbySessionCache = lobbySessionCache;
        this.invitationService = invitationService;
        this.validator = new InvitationSentRecordValidator(gameTypeRepository);
        this.webApiResponses = webApiResponses;
        this.springErrorResponseFormatter = springErrorResponseFormatter;
    }

    /**
     * Record sent invitations.
     *
     * @param request          the request
     * @param response         the response
     * @param invitationSource the source, must be one of {@link InvitationSource}
     * @param record           the record to save
     * @param result           the result complete with errors if validation fails.
     * @return view name
     */
    @RequestMapping(value = "/{invitationSource}", method = RequestMethod.POST)
    public void recordSentInvites(HttpServletRequest request,
                                  HttpServletResponse response,
                                  @PathVariable("invitationSource") String invitationSource,
                                  @ModelAttribute("record") InvitationSentRecord record,
                                  BindingResult result) throws IOException {
        InvitationSource source = quietlyParseSource(invitationSource);
        if (source == null) {
            LOG.debug("Attempt to record unknown invitation source {}", invitationSource);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        validator.validate(record, result);
        LOG.debug("Validation result for record {} was {}", record, result.getFieldErrors());
        if (result.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            webApiResponses.write(response, HttpServletResponse.SC_BAD_REQUEST, springErrorResponseFormatter.toJson(result));
            return;
        }

        BigDecimal playerId = lobbySessionCache.getActiveSession(request).getPlayerId();
        DateTime now = new DateTime();
        String csv = record.getSourceIds();

        String[] sourceIds = csv.split(",");

        for (String sourceId : sourceIds) {
            invitationService.invitationSent(playerId, sourceId, source, now, record.getGameType(), record.getPlatform());
        }
        LOG.debug("Successfully recorded {} invitations for record {}", sourceIds.length, record);

        webApiResponses.writeNoContent(response, HttpServletResponse.SC_ACCEPTED);
    }

    private static InvitationSource quietlyParseSource(String invitationSource) {
        try {
            return InvitationSource.valueOf(invitationSource.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
