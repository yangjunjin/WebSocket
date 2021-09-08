package com.yctc.websocket2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.yctc.websocket2.websoket.MyWsStatusListener;
import com.yctc.websocket2.websoket.WebSocketManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {
    private String url = "wss://sa.zhitingtech.com/ws";//服务器获取的url
    //private String url = "ws://sa.zhitingtech.com:8088/ws";//服务器获取的url
    //private String url = "ws://192.168.0.166:8088/ws";//服务器获取的url

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebSocketManager.initWebSockets(getApplication(),url,new MyWsStatusListener(){
            @Override
            public void onMessage(String text) {
                super.onMessage(text);
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onClick(View view) {
        if (view.getId()==R.id.send) {
            String text = "{\"domain\":\"yeelight\",\"id\":2616431230,\"type\":\"call_service\",\"service\":\"state\",\"service_data\":{\"device_id\":2}}";
            WebSocketManager.wsManager.sendMessage(text);
        }
    }
}