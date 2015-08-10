package com.yazino.platform.chat;

import java.math.BigDecimal;

public interface ChatService {

    void processCommand(BigDecimal playerId,
                        String... chatCommand);

    void asyncProcessCommand(BigDecimal playerId,
                             String... chatCommand);

}
