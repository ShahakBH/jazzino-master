package com.yazino.web.domain.facebook;

import com.restfb.Facebook;

public class RewardQueryResult {
    @Facebook
    private int email;

    @Facebook
    private int bookmarked;

    @Override
    public String toString() {
        return String.format("%s - %s", email, bookmarked);
    }

    public int getEmail() {
        return email;
    }

    public int getBookmarked() {
        return bookmarked;
    }
}
