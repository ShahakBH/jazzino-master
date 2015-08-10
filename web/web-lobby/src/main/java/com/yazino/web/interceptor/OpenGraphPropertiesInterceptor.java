package com.yazino.web.interceptor;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class OpenGraphPropertiesInterceptor extends HandlerInterceptorAdapter {

    @Override
    public void postHandle(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final Object handler,
                           final ModelAndView modelAndView) throws Exception {
        if (modelAndView != null && modelAndView.getModel() != null) {
            final String gameType = (String) modelAndView.getModel().get("gameType");
            if (gameType != null) {
                final String accessToken = (String) request.getSession().getAttribute(
                        "facebookAccessToken." + gameType);
                if (accessToken != null) {
                    modelAndView.addObject("facebookAccessToken", accessToken);
                }
            }
        }
        super.postHandle(request, response, handler, modelAndView);
    }

}
