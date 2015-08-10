package com.yazino.engagement.mobile;

enum MobileDeviceEvent {

    ADDED,
    UPDATED_PUSH_TOKEN,
    DEACTIVATED_DEREGISTERED,
    DEACTIVATED_REGISTERED_BY_DIFFERENT_PLAYER,

    // old data migration
    ADDED_NO_DEVICE_ID,
    SET_DEVICE_ID,
    DEACTIVATED_NO_DEVICE_ID

}
