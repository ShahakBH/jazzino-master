package com.yazino.platform.processor.community;

import java.util.Date;

public class StaticTimer implements Timer {
    private Date time;

    public StaticTimer(Date time) {
        this.time = time;
    }

    public Date getCurrentTime() {
        return time;
    }
}
