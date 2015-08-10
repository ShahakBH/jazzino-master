package com.yazino.platform.processor.community;

import java.util.Date;

public class SystemTimer implements Timer {
    public Date getCurrentTime() {
        return new Date();
    }
}
