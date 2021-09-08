package com.yctc.websocket2.websoket;

import okhttp3.WebSocket;
import okio.ByteString;

interface MyWebSocketManagerListener {

    WebSocket getWebSocket();

    void startConnect();

    void stopConnect();

    boolean isWsConnected();

    int getCurrentStatus();

    void setCurrentStatus(int currentStatus);

    boolean sendMessage(String msg);

    boolean sendMessage(ByteString byteString);
}
