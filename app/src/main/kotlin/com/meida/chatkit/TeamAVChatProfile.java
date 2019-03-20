package com.meida.chatkit;

import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.meida.model.RefreshMessageEvent;
import com.netease.nim.avchatkit.AVChatKit;
import com.netease.nim.avchatkit.AVChatProfile;
import com.netease.nim.avchatkit.common.Handlers;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.auth.AuthServiceObserver;
import com.netease.nimlib.sdk.auth.constant.LoginSyncStatus;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.model.CustomNotification;

import org.greenrobot.eventbus.EventBus;

public class TeamAVChatProfile {

    private static final String KEY_TYPE = "type";
    private static final String KEY_RNAME = "room";
    private static final String KEY_MODIFY = "modify";

    private boolean isTeamAVChatting = false;
    private String teamAVChatId;
    private String teamAVChatName;
    private String chatModel;

    private boolean isSyncComplete = true; //未开始也算同步完成，可能存在不启动同步的情况

    private TeamAVChatProfile() {
        teamAVChatId = "";
        teamAVChatName = "";
        chatModel = "none";
    }

    public String buildContent(String roomMode) {
        JSONObject json = new JSONObject();
        json.put(KEY_TYPE, roomMode);
        return json.toString();
    }

    public String buildContent(String roomName, String roomMode) {
        JSONObject json = new JSONObject();
        json.put(KEY_RNAME, roomName);
        json.put(KEY_TYPE, roomMode);
        return json.toString();
    }

    public String buildContent(String roomName, String newName, String roomMode) {
        JSONObject json = new JSONObject();
        json.put(KEY_RNAME, roomName);
        json.put(KEY_MODIFY, newName);
        json.put(KEY_TYPE, roomMode);
        return json.toString();
    }

    private JSONObject parseContentJson(CustomNotification notification) {
        if (notification != null) {
            String content = notification.getContent();
            return JSONObject.parseObject(content);
        }
        return null;
    }

    private String getTeamAVChatType(JSONObject json) {
        String type = null;
        if (json != null) type = json.getString(KEY_TYPE);
        return type == null ? "" : type;
    }

    /**
     * 监听自定义通知消息
     */
    private Observer<CustomNotification> customNotificationObserver = new Observer<CustomNotification>() {
        @Override
        public void onEvent(CustomNotification customNotification) {
            try {
                JSONObject jsonObject = parseContentJson(customNotification);

                switch (getTeamAVChatType(jsonObject)) {
                    case TeamState.CHAT_CREATE: //建群通知
                        final String createId = jsonObject.getString(KEY_RNAME);
                        RefreshMessageEvent createData = new RefreshMessageEvent();
                        createData.setType("创建群组通知");
                        createData.setId(createId);
                        EventBus.getDefault().post(createData);

                        //接收到群邀请，启动来点界面
                        if (isTeamAVChatting || AVChatProfile.getInstance().isAVChatting()) {
                            Toast.makeText(AVChatKit.getContext(), "正在进行语音对讲通话", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // if (isSyncComplete) launchActivity(createId);
                        break;
                    case TeamState.CHAT_JOIN: //加群通知
                        final String joinId = jsonObject.getString(KEY_RNAME);
                        RefreshMessageEvent joinData = new RefreshMessageEvent();
                        joinData.setType("加入群组通知");
                        joinData.setId(joinId);
                        EventBus.getDefault().post(joinData);
                        break;
                    case TeamState.CHAT_QUIT: //退群通知
                        final String quitId = jsonObject.getString(KEY_RNAME);
                        RefreshMessageEvent quitData = new RefreshMessageEvent();
                        quitData.setType("退出群组通知");
                        quitData.setId(quitId);
                        EventBus.getDefault().post(quitData);
                        break;
                    case TeamState.CHAT_PULL: //入群通知
                        final String pullId = jsonObject.getString(KEY_RNAME);
                        RefreshMessageEvent pullData = new RefreshMessageEvent();
                        pullData.setType("拉入群组通知");
                        pullData.setId(pullId);
                        EventBus.getDefault().post(pullData);
                        break;
                    case TeamState.CHAT_KICK: //踢群通知
                        final String kickId = jsonObject.getString(KEY_RNAME);
                        RefreshMessageEvent kickData = new RefreshMessageEvent();
                        kickData.setType("踢出群组通知");
                        kickData.setId(kickId);
                        EventBus.getDefault().post(kickData);
                        break;
                    case TeamState.CHAT_LEVEL: //优先权通知
                        final String levelId = jsonObject.getString(KEY_RNAME);
                        RefreshMessageEvent levelData = new RefreshMessageEvent();
                        levelData.setType("优先权通知");
                        levelData.setId(levelId);
                        EventBus.getDefault().post(levelData);
                        break;
                    case TeamState.CHAT_MODIFY: //修改房间名通知
                        final String modifyId = jsonObject.getString(KEY_RNAME);
                        final String modifyName = jsonObject.getString(KEY_MODIFY);
                        RefreshMessageEvent modifyData = new RefreshMessageEvent();
                        modifyData.setType("修改群名通知");
                        modifyData.setId(modifyId);
                        modifyData.setName(modifyName);
                        EventBus.getDefault().post(modifyData);
                        break;
                    case TeamState.CHAT_FRIEND_ADD: //加好友通知
                        RefreshMessageEvent addData = new RefreshMessageEvent();
                        addData.setType("加好友通知");
                        EventBus.getDefault().post(addData);
                        break;
                    case TeamState.CHAT_FRIEND_DEL: //删好友通知
                        RefreshMessageEvent delData = new RefreshMessageEvent();
                        delData.setType("删好友通知");
                        EventBus.getDefault().post(delData);
                        break;
                    case TeamState.CHAT_FRIEND_AGREE: //好友同意通知
                        RefreshMessageEvent agreeData = new RefreshMessageEvent();
                        agreeData.setType("好友同意通知");
                        EventBus.getDefault().post(agreeData);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    private void launchActivity(final String roomName) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                //欢迎界面正在运行，则等MainActivity启动之后再启动，否则直接启动 TeamAVChatActivity
                if (!AVChatKit.isMainTaskLaunching()) {
                    AVChatKit.outgoingTeamCall(AVChatKit.getContext(), roomName);
                } else launchActivity(roomName);
            }
        };

        Handlers.sharedHandler(AVChatKit.getContext()).postDelayed(r, 200);
    }

    private Observer<LoginSyncStatus> loginSyncStatusObserver = new Observer<LoginSyncStatus>() {
        @Override
        public void onEvent(LoginSyncStatus loginSyncStatus) {
            isSyncComplete = (loginSyncStatus == LoginSyncStatus.SYNC_COMPLETED
                    || loginSyncStatus == LoginSyncStatus.NO_BEGIN);
        }
    };

    public void setTeamAVChatting(boolean teamAVChatting) {
        isTeamAVChatting = teamAVChatting;
    }

    public void setTeamAVChatId(String teamId) {
        teamAVChatId = teamId;
    }

    public void setTeamAVChatName(String teamName) {
        teamAVChatName = teamName;
    }

    public boolean isTeamAVChatting() {
        return isTeamAVChatting;
    }

    public String getTeamAVChatId() {
        return teamAVChatId;
    }

    public String getTeamAVChatName() {
        return teamAVChatName;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public void registerObserver(boolean register) {
        NIMClient.getService(AuthServiceObserver.class).observeLoginSyncDataStatus(loginSyncStatusObserver, register);
        NIMClient.getService(MsgServiceObserve.class).observeCustomNotification(customNotificationObserver, register);
    }

    public static TeamAVChatProfile sharedInstance() {
        return InstanceHolder.teamAVChatProfile;
    }

    private static class InstanceHolder {
        private final static TeamAVChatProfile teamAVChatProfile = new TeamAVChatProfile();
    }

}
