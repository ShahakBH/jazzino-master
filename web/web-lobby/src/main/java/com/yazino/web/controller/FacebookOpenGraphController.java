package com.yazino.web.controller;


import com.yazino.platform.account.WalletServiceException;
import com.yazino.web.domain.facebook.FacebookOGResources;
import com.yazino.web.payment.facebook.FacebookProductUrl;
import com.yazino.web.util.OpenGraphObjectId;
import com.yazino.web.util.OpenGraphObjectIdParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
@RequestMapping("/fbog/")
public class FacebookOpenGraphController {

    private static final Logger LOG = LoggerFactory.getLogger(FacebookOpenGraphController.class);
    private FacebookOGResources resourceMap;
    private FacebookConfiguration facebookConfiguration;

    @Autowired
    public FacebookOpenGraphController(
            final FacebookOGResources fbObjectResources,
            @Qualifier("facebookConfiguration") final FacebookConfiguration facebookConfiguration) {
        notNull(fbObjectResources, "fbObjectResources is null");
        notNull(facebookConfiguration, "facebookConfig is null");
        this.facebookConfiguration = facebookConfiguration;
        this.resourceMap = fbObjectResources;
    }

    @RequestMapping("/level/{objectId}")
    public ModelAndView handleLevel(@PathVariable(value = "objectId") final String objectId,
                                    @RequestParam(value = "fb_ref", required = false) final String fbRef,
                                    final HttpServletResponse response) throws IOException {
        String title;
        String description;
        String openGraphObjectPrefix;

        try {
            final OpenGraphObjectId parsedId = OpenGraphObjectIdParser.parse(objectId);
            openGraphObjectPrefix = parsedId.getPrefix();
            final int levelNumber = parsedId.getSuffix();
            title = resourceMap.getTitle(openGraphObjectPrefix + "_level_" + levelNumber);
            description = resourceMap.getDescription(openGraphObjectPrefix + "_level_" + levelNumber);

        } catch (Exception e) {
            LOG.error("invalid id passed to FB OG Controller:" + objectId);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid id passed to FB OG Controller:" + objectId);
            return null;
        }

        try {
            return returnModel(objectId, "level", fbRef, title, description, openGraphObjectPrefix,
                    new ModelAndView("fbog/object"));
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Internal error");
            return null;
        }
    }


    @RequestMapping("/product/{packageId}")
    public ModelAndView getProductInfo(@PathVariable(value = "packageId") final String productId,
                                       final HttpServletResponse response) throws IOException {

        String description;
        String packageNumber;
        try {
            final FacebookProductUrl facebookProductUrl = FacebookProductUrl.fromProductIdWithoutUrl(productId);
            description = facebookProductUrl.getChips() + " Chips";
            packageNumber = facebookProductUrl.getPackageNumber();
        } catch (WalletServiceException e) {
            LOG.error("couldn't parse productId", e);
            return null;
        }

        final ModelAndView modelAndView = new ModelAndView("fbog/product");
        modelAndView.addObject("objectId", productId);
        modelAndView.addObject("objectTitle", description);
        modelAndView.addObject("objectImg", packageNumber);
        modelAndView.addObject("objectDesc", description);
        return modelAndView;
    }

    @RequestMapping("/currency/{objectId}")
    public ModelAndView getCurrencyInfo(@PathVariable(value = "objectId") final String objectId,
                                        final HttpServletResponse response) throws IOException {
        String gameType;
        try {
            final OpenGraphObjectId parsedId = OpenGraphObjectIdParser.parseGameType(objectId);
            gameType = parsedId.getPrefix();

        } catch (Exception e) {
            LOG.error("invalid id passed to FB OG Controller:" + objectId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "invalid id passed to FB OG Controller:" + objectId);
            return null;
        }
        final FacebookAppConfiguration appConfig = facebookConfiguration.getAppConfigFor(gameType,
                FacebookConfiguration.ApplicationType.CANVAS,
                FacebookConfiguration.MatchType.STRICT);
        notNull(appConfig);

        final ModelAndView modelAndView = new ModelAndView("fbog/currency");

        modelAndView.addObject("appId", appConfig.getApplicationId());
        modelAndView.addObject("appName", appConfig.getAppName());
        modelAndView.addObject("objectId", objectId);
        modelAndView.addObject("objectType", "currency");

        return modelAndView;
    }

    @RequestMapping("/{objectType}/{objectId}")
    public ModelAndView getOGData(@PathVariable(value = "objectId") final String objectId,
                                  @PathVariable(value = "objectType") final String objectType,
                                  @RequestParam(value = "fb_ref", required = false) final String submittedFbRef,
                                  final HttpServletResponse response) throws IOException {

        final String title = resourceMap.getTitle(objectId);
        final String description = resourceMap.getDescription(objectId);

        if (title == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Open Graph Object " + objectId + " does not exist.");
            return null;
        }

        String openGraphObjectPrefix;
        try {
            final OpenGraphObjectId parsedId = OpenGraphObjectIdParser.parse(objectId);
            openGraphObjectPrefix = parsedId.getPrefix();

        } catch (Exception e) {
            LOG.error("invalid id passed to FB OG Controller:" + objectId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "invalid id passed to FB OG Controller:" + objectId);
            return null;
        }

        final String fbRef = buildFbRef(objectId, submittedFbRef);

        final ModelAndView modelAndView = new ModelAndView("fbog/object");

        try {
            return returnModel(objectId, objectType, fbRef, title, description, openGraphObjectPrefix, modelAndView);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Internal error");
            return null;
        }


    }

    private String buildFbRef(final String objectId,
                              final String submittedFbRef) {
        String fbRef = submittedFbRef;
        if (fbRef == null || fbRef.length() == 0) {
            fbRef = "fb_og_" + objectId;
        }
        return fbRef;
    }

    private ModelAndView returnModel(final String objectId,
                                     final String objectType,
                                     final String fbRef,
                                     final String title,
                                     final String description,
                                     final String openGraphObjectPrefix,
                                     final ModelAndView modelAndView)
            throws IllegalArgumentException {

        final FacebookAppConfiguration facebookAppConfiguration
                = facebookConfiguration.getAppConfigForOpenGraphObjectPrefix(openGraphObjectPrefix);
        if (facebookAppConfiguration == null) {
            throw new IllegalArgumentException();
        }

        modelAndView.addObject("appId", facebookAppConfiguration.getApplicationId());
        modelAndView.addObject("appName", facebookAppConfiguration.getAppName());

        modelAndView.addObject("objectType", objectType);
        modelAndView.addObject("objectTitle", title);
        modelAndView.addObject("objectId", objectId);
        modelAndView.addObject("objectArticle", resourceMap.getArticle(objectId));
        modelAndView.addObject("objectImg", objectId);
        modelAndView.addObject("objectDesc", description);
        modelAndView.addObject("ref", fbRef);

        return modelAndView;

    }

    private class ChipPackage {

        private final String id;
        private final String description;

        private int getPackageNumber() {
            return packageNumber;
        }

        private String getDescription() {
            return description;
        }

        private String getId() {
            return id;
        }

        private final int packageNumber;

        public ChipPackage(final String id, final String description, final int packageNumber) {
            this.id = id;
            this.description = description;
            this.packageNumber = packageNumber;
        }
    }
}
