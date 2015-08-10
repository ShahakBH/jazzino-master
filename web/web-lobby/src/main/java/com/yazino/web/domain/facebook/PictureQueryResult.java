package com.yazino.web.domain.facebook;

import com.restfb.Facebook;

public class PictureQueryResult {
    @Facebook
    private String pic_square;

    public String getPic_square() {
        return pic_square;
    }

    @Override
    public String toString() {
        return "PictureQueryResult{"
                + "pic_square=" + pic_square
                + '}';
    }
}
