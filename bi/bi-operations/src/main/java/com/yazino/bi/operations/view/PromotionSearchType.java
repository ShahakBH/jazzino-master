package com.yazino.bi.operations.view;

import org.springframework.util.StringUtils;

public enum PromotionSearchType {
    LIVE, ARCHIVED;

    public String getDisplayName() {
        return StringUtils.capitalize(name().toLowerCase());
    }
}
