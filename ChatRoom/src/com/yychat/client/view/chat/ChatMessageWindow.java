package com.yychat.client.view.chat;

import com.yychat.common.model.Message;

public interface ChatMessageWindow {
    void appendMessage(Message message,boolean received);
}
