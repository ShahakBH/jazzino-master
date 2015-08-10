package com.yazino.platform.service.statistic;

import com.yazino.platform.model.statistic.Notification;

public interface NotificationService {
    void publish(Notification notification);
}
