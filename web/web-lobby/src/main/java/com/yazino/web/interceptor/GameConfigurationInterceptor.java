package com.yazino.web.interceptor;

import com.yazino.web.service.GameConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang3.Validate.notNull;

public class GameConfigurationInterceptor extends HandlerInterceptorAdapter {

    private GameConfigurationRepository gameConfigurationRepository;

    @Autowired
    public GameConfigurationInterceptor(
            @Qualifier("gameConfigurationRepository") final GameConfigurationRepository gameConfigurationRepository) {
        notNull(gameConfigurationRepository);
        this.gameConfigurationRepository = gameConfigurationRepository;
    }

    @Override
    public void postHandle(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final Object handler,
                           final ModelAndView modelAndView) throws Exception {
        if (modelAndView != null) {
            modelAndView.addObject("gameConfigurations", gameConfigurationRepository.findAll());
        }
        super.postHandle(request, response, handler, modelAndView);
    }
}
