package com.yazino.interceptor;

import com.yazino.model.session.StandalonePlayerSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component("standalonePlayerSessionInterceptor")
public class StandalonePlayerSessionInterceptor extends HandlerInterceptorAdapter {

    private final StandalonePlayerSession session;

    @Autowired
    public StandalonePlayerSessionInterceptor(final StandalonePlayerSession session) {
        this.session = session;
    }

    @Override
    public void postHandle(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final Object handler,
                           final ModelAndView modelAndView) throws Exception {
        if (modelAndView != null) {
            addParameterAndHandle(request, response, handler, modelAndView);
            return;
        }
        addParameterAndHandle(request, response, handler, new ModelAndView());
    }

    private void addParameterAndHandle(final HttpServletRequest request,
                                       final HttpServletResponse response,
                                       final Object handler,
                                       final ModelAndView modelAndView) throws Exception {
        modelAndView.addObject("standalonePlayerSession", session);
        super.postHandle(request, response, handler, modelAndView);
    }

}
