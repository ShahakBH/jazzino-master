package com.yazino.controller;

import com.yazino.model.FlashvarsSource;
import com.yazino.model.config.InterceptedPropertyPlaceholderConfigurer;
import com.yazino.model.config.RabbitMQTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ConfigController {

    private final InterceptedPropertyPlaceholderConfigurer configurer;
    private final RabbitMQTester rabbitMQTester;
    private final FlashvarsSource flashvarsSource;

    @Autowired
    public ConfigController(final InterceptedPropertyPlaceholderConfigurer configurer,
                            final RabbitMQTester rabbitMQTester,
                            final FlashvarsSource flashvarsSource) {
        this.configurer = configurer;
        this.rabbitMQTester = rabbitMQTester;
        this.flashvarsSource = flashvarsSource;
    }

    @RequestMapping("/config/properties")
    public ModelAndView properties() {
        return new ModelAndView("config/properties", "properties", configurer.getServerProperties());
    }

    @RequestMapping("/config/rabbit")
    public ModelAndView rabbit() {
        return new ModelAndView("config/rabbit", "diagnostics", rabbitMQTester.runDiagnostics());
    }

    @RequestMapping("/config/variation")
    public ModelAndView variation() {
        return new ModelAndView("config/variation", "properties", configurer.getVariationProperties());
    }

    @RequestMapping("/config/flashvars")
    public ModelAndView flashvars() {
        return new ModelAndView("config/flashvars", "properties", configurer.getFlashvars());
    }
}
