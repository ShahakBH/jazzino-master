package com.yazino.bi.operations.controller;

import com.yazino.bi.tracking.TrackingDao;
import com.yazino.bi.tracking.TrackingEvent;
import org.apache.velocity.tools.generic.DateTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
@RequestMapping("/tracking")
public class TrackingEventViewerController {

    public static final int MOST_RECENT_EVENT_TABLE_SIZE = 100;

    private TrackingDao trackingDao;

    @Autowired
    public TrackingEventViewerController(TrackingDao trackingDao) {
        this.trackingDao = trackingDao;
    }

    @RequestMapping("/recent")
    public ModelAndView listRecentEvents() {
        ModelAndView modelAndView = new ModelAndView("tracking/event-table");
        List<TrackingEvent> mostRecentEvents = trackingDao.findMostRecentEvents(MOST_RECENT_EVENT_TABLE_SIZE);
        modelAndView.addObject("events", mostRecentEvents);
        modelAndView.addObject("dt", new DateTool());
        return modelAndView;
    }
}
