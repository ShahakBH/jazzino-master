package com.yazino.platform.messaging;

import java.io.Serializable;

public interface Message<T> extends Serializable {
    int VERSION = 1;

    int getVersion();

    T getMessageType();
}
