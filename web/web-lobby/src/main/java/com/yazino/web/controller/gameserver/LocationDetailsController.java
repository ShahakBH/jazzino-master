package com.yazino.web.controller.gameserver;

import com.yazino.spring.security.AllowPublicAccess;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import com.yazino.web.domain.LocationDetails;
import com.yazino.web.data.LocationDetailsRepository;
import com.yazino.web.util.JsonHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class LocationDetailsController {

    private final LocationDetailsRepository locationDetailsRepository;
    private JsonHelper jsonHelper = new JsonHelper();

    @Autowired
    public LocationDetailsController(
            @Qualifier("locationDetailsRepository") final LocationDetailsRepository locationDetailsRepository) {
        notNull(locationDetailsRepository, "locationDetailsRepository is null");
        this.locationDetailsRepository = locationDetailsRepository;
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/locationDetails")
    public void locationDetails(final HttpServletRequest request,
                                final HttpServletResponse response) throws IOException {
        final String locationIdStr = request.getParameter("locationId");
        if (!StringUtils.isNumeric(locationIdStr)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "invalid argument locationId: " + locationIdStr);
            return;
        }
        final LocationDetails locationDetails = locationDetailsRepository.getLocationDetails(
                new BigDecimal(locationIdStr));
        response.setContentType("text/javascript");
        response.getWriter().write(jsonHelper.serialize(locationDetails));
    }
}
