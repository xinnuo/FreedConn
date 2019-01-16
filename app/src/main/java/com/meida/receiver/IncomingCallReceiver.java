package com.meida.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.meida.model.RefreshMessageEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * 来电状态监听
 * <p/>
 */
public class IncomingCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            final String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            switch (state) {
                //响铃
                case "RINGING":
                    break;
                //挂断
                case "IDLE":
                    RefreshMessageEvent idleData = new RefreshMessageEvent();
                    idleData.setType("电话挂断");
                    EventBus.getDefault().post(idleData);
                    break;
                //接听(来电或去电)
                case "OFFHOOK":
                    RefreshMessageEvent hookData = new RefreshMessageEvent();
                    hookData.setType("电话接听");
                    EventBus.getDefault().post(hookData);
                    break;
            }
        }
    }
}
