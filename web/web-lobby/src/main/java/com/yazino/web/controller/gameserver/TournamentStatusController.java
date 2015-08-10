package com.yazino.web.controller.gameserver;

import com.yazino.platform.tournament.TournamentView;
import com.yazino.spring.security.AllowPublicAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.domain.TournamentDocumentRequest;
import com.yazino.web.service.TournamentViewDocumentWorker;
import com.yazino.web.data.TournamentViewRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;
import static com.yazino.web.service.TournamentViewDocumentWorker.DocumentType;

/**
 * handles tournament status requests, e.g. tournament overview, player ranking etc.
 */
@Controller
public class TournamentStatusController {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentStatusController.class);
    // number of ranks or players per page
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_PAGE_NUMBER = 1;
    public static final String EMPTY_STATUS = "{}";

    private final LobbySessionCache lobbySessionCache;
    private final TournamentViewDocumentWorker tournamentViewDocumentWorker;
    private final TournamentViewRepository tournamentViewRepository;

    @Autowired
    public TournamentStatusController(final LobbySessionCache lobbySessionCache,
                                      final TournamentViewDocumentWorker tournamentViewDocumentWorker,
                                      final TournamentViewRepository tournamentViewRepository) {
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(tournamentViewDocumentWorker, "tournamentViewDocumentWorker may not be null");
        notNull(tournamentViewRepository, "tournamentViewRepository may not be null");
        this.lobbySessionCache = lobbySessionCache;
        this.tournamentViewDocumentWorker = tournamentViewDocumentWorker;
        this.tournamentViewRepository = tournamentViewRepository;
    }

    /*
        pageNumber relates to ranks unless tournament is registering when it relates to registered players. In this case
        The ranks will just be the payout per ranking and isn't paginated
     */
    @AllowPublicAccess
    @RequestMapping(method = RequestMethod.GET, value = "/game-server/command/tournamentStatus")
    @ResponseBody
    public String tournamentStatus(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @RequestParam("tournamentId") final String tournamentIdAsString,
            @RequestParam("requestType") final DocumentType requestType,
            @RequestParam(value = "pageNumber", required = false) final Integer pageNumber,
            @RequestParam(value = "pageSize", required = false) final Integer pageSize) throws Exception {
        response.setContentType("text/javascript");

        final BigDecimal tournamentId;
        try {
            tournamentId = new BigDecimal(tournamentIdAsString);
        } catch (NumberFormatException e) {
            LOG.warn("Invalid tournamentId submitted: {}", tournamentIdAsString);
            response.sendError(HttpStatus.OK.value(), "ERROR|Tournament invalid");
            return EMPTY_STATUS;
        }

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            response.sendError(HttpStatus.OK.value(), "ERROR|Session Expired");
            return EMPTY_STATUS;
        }
        final BigDecimal playerId = lobbySession.getPlayerId();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Sending Tournament request requestType=[%s] playerId=[%s] tournamentId=[%s]",
                    requestType, playerId, tournamentId));
        }
        try {
            final TournamentView view = tournamentViewRepository.getTournamentView(tournamentId);
            if (view == null) {
                LOG.warn("Tournament not found: " + tournamentId);
                response.sendError(HttpStatus.OK.value(), "ERROR|Tournament not found");
                return EMPTY_STATUS;
            }
            final TournamentDocumentRequest docRequest = buildRequestDocument(requestType, pageNumber, pageSize,
                    playerId);
            return tournamentViewDocumentWorker.buildDocument(view, docRequest);

        } catch (Throwable e) {
            LOG.error("Request failed", e);
            response.sendError(HttpStatus.OK.value(), "ERROR|Command rejected");
            return EMPTY_STATUS;
        }
    }

    private TournamentDocumentRequest buildRequestDocument(final DocumentType requestType,
                                                           final Integer pageNumber,
                                                           final Integer pageSize,
                                                           final BigDecimal playerId) {
        return new TournamentDocumentRequest(requestType, playerId,
                getPageNumber(pageNumber), getPageSize(pageSize));
    }

    private Integer getPageSize(final Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return pageSize;
    }

    private Integer getPageNumber(final Integer pageNumber) {
        if (pageNumber == null || pageNumber < 1) {
            return DEFAULT_PAGE_NUMBER;
        }
        return pageNumber;
    }
}
