package com.coolweather.app.util;

/**
 * Created by itoysk on 2016/8/31.
 */
public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
