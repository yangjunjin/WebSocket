package com.yctc.websocket2.websoket;

import android.util.Log;
import okhttp3.Response;
import okio.ByteString;

abstract class MyWebSocketStatusListener {
    private String TAG = "websocket---WsStatusListener";

    public void onOpen(Response response) {
        Log.e(TAG, "WsManager-----onOpen");
    }

    public void onMessage(String text) {
        Log.e(TAG, "WsManager-----onMessage");
    }

    public void onMessage(ByteString bytes) {
        Log.e(TAG, "WsManager-----onMessage");
    }

    public void onReconnect() {
        Log.e(TAG, "WsManager-----onReconnect");
    }

    public void onClosing(int code, String reason) {
        Log.e(TAG, "WsManager-----onClosing");
    }

    public void onClosed(int code, String reason) {
        Log.e(TAG, "WsManager-----onClosed");
    }

    public void onFailure(Throwable t, Response response) {
        Log.e(TAG, "WsManager-----onFailure");
    }
}
