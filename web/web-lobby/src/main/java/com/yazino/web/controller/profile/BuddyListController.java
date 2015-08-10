package com.yazino.web.controller.profile;

import com.yazino.configuration.YazinoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/player/buddyList")
public class BuddyListController extends AbstractProfileController {

    static final String TAB_CODE_NAME = "buddyList";

    @Autowired
    public BuddyListController(final YazinoConfiguration yazinoConfiguration) {
        super(yazinoConfiguration);
    }

    @Override
    String getCurrentTab() {
        return TAB_CODE_NAME;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView getBuddyList(@RequestParam(value = PARTIAL_URI_FLAG,
            defaultValue = PARTIAL_URI_FLAG_DEFAULT,
            required = PARTIAL_URI_FLAG_IS_REQUIRED) final boolean partial) {
        final ModelAndView mav = getInitialisedModelAndView(partial);
        final ModelMap modelMap = mav.getModelMap();
        setupDefaultModel(modelMap);
        return mav;
    }

}
