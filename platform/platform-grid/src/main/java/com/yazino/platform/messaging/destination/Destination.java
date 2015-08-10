package com.yazino.platform.messaging.destination;


import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;

import java.io.Serializable;

public interface Destination extends Serializable {

    void send(Document document,
              DocumentDispatcher documentDispatcher);

}
