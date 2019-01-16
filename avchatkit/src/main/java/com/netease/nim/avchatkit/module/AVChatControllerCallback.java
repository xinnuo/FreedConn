package com.netease.nim.avchatkit.module;

public interface AVChatControllerCallback<T> {

    void onSuccess(T t);

    void onFailed(int code, String errorMsg);
}
