package com.yctc.websocket2.websoket;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketManager implements MyWebSocketManagerListener {
    private String TAG = "WebSocketManager======";
    private final static int RECONNECT_INTERVAL = 2 * 1000;    //重连自增步长
    private final static long RECONNECT_MAX_TIME = 120 * 1000;   //最大重连间隔
    private Lock mLock;
    private String wsUrl;
    private Context mContext;
    private Request mRequest;
    private WebSocket mWebSocket;
    private OkHttpClient mOkHttpClient;
    private MyWebSocketStatusListener wsStatusListener;
    private Handler wsMainHandler = new Handler(Looper.getMainLooper());
    private Handler handler = new Handler();
    private int mCurrentStatus = WebSocketStatus.DISCONNECTED;     //websocket连接状态
    private int reconnectCount = 1;//重连次数
    private boolean isNeedReconnect;//是否需要断线自动重连
    private boolean isManualClose = false;//是否为手动关闭websocket连接
    public static WebSocketManager wsManager;

    public static void initWebSockets(Context context, String url, MyWsStatusListener listener) {
        Log.e("WebSocketManager","-websocket--connitWbSockets---开始连接---");
        if (wsManager != null && wsManager.isWsConnected()) {
            wsManager.stopConnect();
            wsManager = null;
        }
        wsManager = new WebSocketManager.Builder(context)
                .client(new OkHttpClient().newBuilder()
                        .retryOnConnectionFailure(true)
                        .pingInterval(15, TimeUnit.SECONDS)
                        .build())
                .needReconnect(true)
                .wsUrl(url)
                .build();
        wsManager.setWsStatusListener(listener);
        wsManager.startConnect();
    }

    private Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (wsStatusListener != null) {
                wsStatusListener.onReconnect();
            }
            buildConnect();
        }
    };

    private WebSocketListener mWebSocketListener = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, final Response response) {
            Log.e(TAG,"-----------onOpen-------------");
            mWebSocket = webSocket;
            setCurrentStatus(WebSocketStatus.CONNECTED);
            connected();
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wsStatusListener.onOpen(response);
                        }
                    });
                } else {
                    wsStatusListener.onOpen(response);
                }
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, final ByteString bytes) {
            Log.e(TAG,"-----------onMessage1-------------");
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wsStatusListener.onMessage(bytes);
                        }
                    });
                } else {
                    wsStatusListener.onMessage(bytes);
                }
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, final String text) {
            Log.e(TAG,"-----------onMessage2-------------");
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wsStatusListener.onMessage(text);
                        }
                    });
                } else {
                    wsStatusListener.onMessage(text);
                }
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, final int code, final String reason) {
            Log.e(TAG,"-----------onClosing-------------");
            if (wsStatusListener != null) {
                Log.e(TAG, "---onClosing------code----" + code);
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wsStatusListener.onClosing(code, reason);
                        }
                    });
                } else {
                    wsStatusListener.onClosing(code, reason);
                }
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, final int code, final String reason) {
            Log.e(TAG,"-----------onClosed-------------");
            if (wsStatusListener != null) {
                Log.e(TAG, "---onClosed------code----" + code);
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wsStatusListener.onClosed(code, reason);
                        }
                    });
                } else {
                    wsStatusListener.onClosed(code, reason);
                }
            }
            tryReconnect();
        }

        @Override
        public void onFailure(WebSocket webSocket, final Throwable t, final Response response) {
            Log.e(TAG,"-----------onFailure-------------");
            Log.e(TAG,"Throwable=="+t.getMessage());
            if(response!=null){
                Log.e(TAG,"response=="+response.message());
            }
            Log.e(TAG,"-----------end-------------");
            if (wsStatusListener != null) {
                setCurrentStatus(WebSocketStatus.DISCONNECTED);
                Log.e(TAG, "---onFailure----------" + t.toString());
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wsStatusListener.onFailure(t, response);
                        }
                    });
                } else {
                    wsStatusListener.onFailure(t, response);
                }
            }
            tryReconnect();
        }
    };

    public WebSocketManager(Builder builder) {
        mContext = builder.mContext;
        wsUrl = builder.wsUrl;
        isNeedReconnect = builder.needReconnect;
        mOkHttpClient = builder.mOkHttpClient;
        this.mLock = new ReentrantLock();
    }

    private void initWebSocket() {
        if (mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .build();
        }
        if (mRequest == null) {
            mRequest = new Request.Builder()
                    .url(wsUrl)
                    .build();
        }
        mOkHttpClient.dispatcher().cancelAll();
        try {
            mLock.lockInterruptibly();
            try {
                mOkHttpClient.newWebSocket(mRequest, mWebSocketListener);
            } finally {
                mLock.unlock();
            }
        } catch (Exception e) {
            Log.e(TAG, "--------websocket初始化失败---");
        }
    }

    @Override
    public WebSocket getWebSocket() {
        return mWebSocket;
    }

    public void setWsStatusListener(MyWebSocketStatusListener wsStatusListener) {
        this.wsStatusListener = wsStatusListener;
    }

    @Override
    public synchronized boolean isWsConnected() {
        return mCurrentStatus == WebSocketStatus.CONNECTED;
    }

    @Override
    public synchronized int getCurrentStatus() {
        return mCurrentStatus;
    }

    @Override
    public synchronized void setCurrentStatus(int currentStatus) {
        this.mCurrentStatus = currentStatus;
    }

    @Override
    public void startConnect() {
        isManualClose = false;
        buildConnect();
    }

    @Override
    public void stopConnect() {
        isManualClose = true;
        disconnect();
    }

    /**
     * 只有登录才重连
     */
    private void tryReconnect() {
        if (reconnectCount > 50) {
            Log.e(TAG, "-----------重连次数超过50次了");
            return;
        }
        handler.postDelayed(() -> {

            if (!isNetworkConnected(mContext)) {
                setCurrentStatus(WebSocketStatus.DISCONNECTED);
                return;
            }
            setCurrentStatus(WebSocketStatus.RECONNECT);

            long delay = reconnectCount * RECONNECT_INTERVAL;
            wsMainHandler.postDelayed(reconnectRunnable, delay > RECONNECT_MAX_TIME ? RECONNECT_MAX_TIME : delay);
            reconnectCount++;
            Log.e(TAG, "-websocket----delay-----" + delay);
        }, 1500);
    }

    private void cancelReconnect() {
        wsMainHandler.removeCallbacks(reconnectRunnable);
        reconnectCount = 1;
    }

    private void connected() {
        cancelReconnect();
    }

    private void disconnect() {
        if (mCurrentStatus == WebSocketStatus.DISCONNECTED) {
            return;
        }
        cancelReconnect();
        if (mOkHttpClient != null) {
            mOkHttpClient.dispatcher().cancelAll();
        }
        if (mWebSocket != null) {
            boolean isClosed = mWebSocket.close(WebSocketStatus.CODE.NORMAL_CLOSE, WebSocketStatus.TIP.NORMAL_CLOSE);
            //非正常关闭连接
            if (!isClosed) {
                if (wsStatusListener != null) {
                    wsStatusListener.onClosed(WebSocketStatus.CODE.ABNORMAL_CLOSE, WebSocketStatus.TIP.ABNORMAL_CLOSE);
                }
            }
        }
        setCurrentStatus(WebSocketStatus.DISCONNECTED);
    }

    private synchronized void buildConnect() {
        if (!isNetworkConnected(mContext)) {
            setCurrentStatus(WebSocketStatus.DISCONNECTED);
            return;
        }
        switch (getCurrentStatus()) {
            case WebSocketStatus.CONNECTED:
            case WebSocketStatus.CONNECTING:
                break;
            default:
                setCurrentStatus(WebSocketStatus.CONNECTING);
                initWebSocket();
        }
    }

    //发送消息
    @Override
    public boolean sendMessage(String msg) {
        return send(msg);
    }

    @Override
    public boolean sendMessage(ByteString byteString) {
        return send(byteString);
    }

    private boolean send(Object msg) {
        boolean isSend = false;
        if (mWebSocket != null && mCurrentStatus == WebSocketStatus.CONNECTED) {
            if (msg instanceof String) {
                isSend = mWebSocket.send((String) msg);
            } else if (msg instanceof ByteString) {
                isSend = mWebSocket.send((ByteString) msg);
            }
            //发送消息失败，尝试重连
            if (!isSend) {
                tryReconnect();
            }
        }
        return isSend;
    }

    //检查网络是否连接
    private boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    public static final class Builder {
        private String wsUrl;
        private Context mContext;
        private OkHttpClient mOkHttpClient;
        private boolean needReconnect = true;

        public Builder(Context val) {
            mContext = val;
        }

        public Builder wsUrl(String val) {
            wsUrl = val;
            return this;
        }

        public Builder client(OkHttpClient val) {
            mOkHttpClient = val;
            return this;
        }

        public Builder needReconnect(boolean val) {
            needReconnect = val;
            return this;
        }

        public WebSocketManager build() {
            return new WebSocketManager(this);
        }
    }
}
