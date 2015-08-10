package com.yazino.web.util;

import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.service.SafeBuyChipsPromotionServiceWrapper;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.ModelAndView;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashSet;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.LOOSE;

public class CommonPropertiesHelper {
    private final Collection<ViewAspectConfiguration> viewAspectConfigurations = new HashSet<>();
    private final MessagingHostResolver messagingHostResolver;
    private final LobbySessionCache lobbySessionCache;
    private final SiteConfiguration siteConfiguration;
    private final CookieHelper cookieHelper;
    private final FacebookConfiguration facebookConfiguration;
    private final SafeBuyChipsPromotionServiceWrapper promotionService;
    private final FacebookCanvasDetection facebookCanvasDetection;
    private final Environment environment;

    private ThreadLocal<PegDownProcessor> pegDownProcessor = new ThreadLocal<PegDownProcessor>() {
        @Override
        protected PegDownProcessor initialValue() {
            return new PegDownProcessor(Extensions.SMARTYPANTS | Extensions.SUPPRESS_ALL_HTML);
        }
    };

    @Autowired
    public CommonPropertiesHelper(final MessagingHostResolver messagingHostResolver,
                                  final LobbySessionCache lobbySessionCache,
                                  final SiteConfiguration siteConfiguration,
                                  final CookieHelper cookieHelper,
                                  @Qualifier("facebookConfiguration") final FacebookConfiguration facebookConfiguration,
                                  final SafeBuyChipsPromotionServiceWrapper promotionService,
                                  final FacebookCanvasDetection facebookCanvasDetection,
                                  final Environment environment) {
        notNull(messagingHostResolver, "messagingHostResolver may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(siteConfiguration, "siteConfiguration may not be null");
        notNull(cookieHelper, "cookieHelper may not be null");
        notNull(facebookConfiguration, "facebookConfiguration may not be null");
        notNull(promotionService, "promotionService may not be null");
        notNull(facebookCanvasDetection, "facebookCanvasDetection may not be null");
        notNull(environment, "environment may not be null");

        this.messagingHostResolver = messagingHostResolver;
        this.lobbySessionCache = lobbySessionCache;
        this.siteConfiguration = siteConfiguration;
        this.cookieHelper = cookieHelper;
        this.facebookConfiguration = facebookConfiguration;
        this.promotionService = promotionService;
        this.facebookCanvasDetection = facebookCanvasDetection;
        this.environment = environment;
    }

    public void setViewAspectConfigurations(final Collection<ViewAspectConfiguration> viewAspectConfigurations) {
        this.viewAspectConfigurations.clear();

        if (viewAspectConfigurations != null) {
            this.viewAspectConfigurations.addAll(viewAspectConfigurations);
        }
    }

    public void setupCommonProperties(final HttpServletRequest request,
                                      final HttpServletResponse response,
                                      final ModelAndView modelAndView) {
        if (modelAndView != null) {
            modelAndView.addObject("markdown", pegDownProcessor.get());

            modelAndView.addObject("hostUrl", siteConfiguration.getHostUrl());
            modelAndView.addObject("partnerId", siteConfiguration.getPartnerId());

            modelAndView.addObject("development", environment.isDevelopment());

            modelAndView.addObject("viewName", modelAndView.getViewName());

            final String sourceId = request.getParameter("sourceId");
            if (sourceId != null && sourceId.trim().length() > 0) {
                modelAndView.addObject("sourceId", sourceId);
            }

            final String gameType = addGameType(request, response, modelAndView);
            final String originalGameType = cookieHelper.getOriginalGameType(request);

            final boolean onCanvas = facebookCanvasDetection.isOnCanvas(request);
            addFacebookConfiguration(modelAndView, gameType, originalGameType, onCanvas);
            addSessionInformation(request, modelAndView, gameType);
            addMessagingHost(request, modelAndView);


            modelAndView.addObject("gameType", gameType);
            modelAndView.addObject("canvas", onCanvas);

            final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
            if (lobbySession != null) {
                modelAndView.addObject("facebookConnect", "FACEBOOK".equals(lobbySession.getPartnerId().name()));
                modelAndView.addObject("hasPromotion", promotionService.hasPromotion(lobbySession.getPlayerId(), lobbySession.getPlatform()));
            }
        }
    }

    private void addMessagingHost(final HttpServletRequest request,
                                  final ModelAndView modelAndView) {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session != null) {
            modelAndView.addObject("messagingHost", messagingHostResolver.resolveMessagingHostForPlayer(
                    session.getPlayerId()));
        }
    }

    private void addViewAspects(final ModelAndView modelAndView,
                                final String gameType,
                                final String partnerId) {
        modelAndView.addObject("viewAspects", new ViewAspects(viewAspectConfigurations, gameType, partnerId));
    }

    private String addGameType(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final ModelAndView modelAndView) {
        final String gameType = getGameTypeFromRequestModelOrCookie(
                request, modelAndView);
        cookieHelper.setLastGameType(response, gameType);
        return gameType;
    }

    private String getGameTypeFromRequestModelOrCookie(final HttpServletRequest request,
                                                       final ModelAndView modelAndView) {
        String gameType = (String) modelAndView.getModel().get("gameType");

        if (isBlank(gameType)) {
            gameType = request.getParameter("gameType");
            if (isBlank(gameType)) {
                gameType = cookieHelper.getLastGameType(request.getCookies(), siteConfiguration.getDefaultGameType());
            } else {
                if (gameType.endsWith("/")) {
                    gameType = gameType.substring(0, gameType.length() - 1);
                }
                gameType = gameType.toUpperCase();
            }
        }

        return gameType;
    }

    private void addSessionInformation(final HttpServletRequest request,
                                       final ModelAndView modelAndView,
                                       final String gameType) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession != null) {
            modelAndView.addObject("lobbySession", lobbySession);
            addViewAspects(modelAndView, gameType, lobbySession.getPartnerId().name());
        } else {
            if (siteConfiguration.getPartnerId() == null) {
                addViewAspects(modelAndView, gameType, null);
            } else {
                addViewAspects(modelAndView, gameType, siteConfiguration.getPartnerId().name());
            }
        }
    }

    private void addFacebookConfiguration(final ModelAndView modelAndView,
                                          final String gameType,
                                          final String originalGameType,
                                          final boolean onCanvas) {
        if (facebookConfiguration != null) {
            modelAndView.addObject("conversionTracking", facebookConfiguration.getConversionTracking());

            if (facebookConfiguration.isConfigured()) {
                final FacebookAppConfiguration facebookAppConfiguration
                        = facebookConfiguration.getAppConfigFor(gameType, applicationTypeFor(onCanvas), LOOSE);

                modelAndView.addObject("facebookAppUrlRoot", facebookConfiguration.getAppUrlRoot());
                modelAndView.addObject("facebookAppName", facebookAppConfiguration.getAppName());
                modelAndView.addObject("facebookApiKey", facebookAppConfiguration.getApiKey());
                modelAndView.addObject("facebookApplicationId", facebookAppConfiguration.getApplicationId());
                modelAndView.addObject("facebookFanPageId", facebookAppConfiguration.getFanPageId());
                modelAndView.addObject("facebookReviewsEnabled", facebookConfiguration.isReviewsEnabled());
                modelAndView.addObject("facebookPublishStreamEnabled", facebookConfiguration.isPublishStreamEnabled());
                modelAndView.addObject("facebookAppsEnabled", facebookConfiguration.isAppsEnabled());
                modelAndView.addObject("facebookLoginUrl", facebookConfiguration.getLoginUrl());
                modelAndView.addObject("facebookCanvasActionsAllowed",
                        facebookAppConfiguration.isCanvasActionsAllowed());

                String facebookOriginalApplicationId = null;
                if (originalGameType != null) {
                    facebookOriginalApplicationId = facebookConfiguration.getAppConfigFor(
                            originalGameType, applicationTypeFor(onCanvas), LOOSE).getApplicationId();
                }
                modelAndView.addObject("facebookOriginalApplicationId", facebookOriginalApplicationId);
            }
        }
    }

    private FacebookConfiguration.ApplicationType applicationTypeFor(final boolean onCanvas) {
        if (onCanvas) {
            return FacebookConfiguration.ApplicationType.CANVAS;
        }
        return FacebookConfiguration.ApplicationType.CONNECT;
    }

}
