package com.yazino.mobile.ws.views;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

/**
 * A {@link ViewResolver} that always returns the same View.
 */
public class StaticViewResolver implements ViewResolver, Ordered {

    private final View mView;
    private int mOrder = Integer.MAX_VALUE;

    public StaticViewResolver(View view) {
        mView = view;
    }

    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        return mView;
    }

    public void setOrder(int order) {
        mOrder = order;
    }

    @Override
    public int getOrder() {
        return mOrder;
    }
}
