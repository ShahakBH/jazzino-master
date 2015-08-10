package com.yazino.host.chat;

import com.yazino.platform.model.chat.GigaspaceChatRequest;

public interface ChatRequestSource {
    GigaspaceChatRequest getNextRequest();
}
