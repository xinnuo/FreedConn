package com.netease.nim.avchatkit.common;

import android.content.Context;

import com.netease.nimlib.sdk.avchat.model.AVChatData;

import java.util.ArrayList;

public interface ICallUtil {

    void incomingCall(Context context,
                      AVChatData config,
                      String displayName,
                      int source);

    void outgoingCall(Context context,
                      String account,
                      String displayName,
                      int callType,
                      int source);

    void outgoingTeamCall(Context context, String roomName);

    void startSettings(Context context);

}
