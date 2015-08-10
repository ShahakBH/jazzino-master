package com.yazino.bi.operations.engagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class EngagementCampaignSenderController {
    private static final Logger LOG = LoggerFactory.getLogger(EngagementCampaignSenderController.class);

    private static final String LIST_REDIRECT_VIEW = "redirect:/engagementCampaign";

    private final EngagementCampaignDao dao;
    private final EngagementCampaignSender engagementCampaignSender;

    @Autowired(required = true)
    public EngagementCampaignSenderController(final EngagementCampaignDao dao,
                                              final EngagementCampaignSender engagementCampaignSender) {
        notNull(dao, "dao was null");
        notNull(engagementCampaignSender, "engagementCampaignSender was null");
        this.dao = dao;
        this.engagementCampaignSender = engagementCampaignSender;
    }

    @RequestMapping(value = {"/engagementCampaign/send"}, method = RequestMethod.GET)
    public String sendAppRequestForId(final Model model, @RequestParam final Integer id) {
        LOG.debug("Sending engagementCampaign, id={}", id);

        final EngagementCampaign engagementCampaign = dao.findById(id);
        final boolean sent = engagementCampaignSender.sendAppRequest(engagementCampaign);
        if (sent) {
            model.addAttribute("msg", "'" + engagementCampaign.getDescription() + "'" + " has been sent.");
        } else {
            model.addAttribute("msg", "Failed to send campaign.");
        }
        return LIST_REDIRECT_VIEW;
    }
}
