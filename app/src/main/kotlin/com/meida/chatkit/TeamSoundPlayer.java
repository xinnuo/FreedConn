package com.meida.chatkit;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.meida.freedconn.R;
import com.netease.nim.avchatkit.AVChatKit;

public class TeamSoundPlayer {

    private Context context;

    private SoundPool soundPool;
    private int streamId;
    private int soundId;

    private static TeamSoundPlayer instance = null;

    public static TeamSoundPlayer instance() {
        if (instance == null) {
            synchronized (TeamSoundPlayer.class) {
                if (instance == null) {
                    instance = new TeamSoundPlayer();
                }
            }
        }
        return instance;
    }

    private TeamSoundPlayer() {
        this.context = AVChatKit.getContext();
    }

    public void stop() {
        if (soundPool != null) {
            if (streamId != 0) {
                soundPool.stop(streamId);
                streamId = 0;
            }
            if (soundId != 0) {
                soundPool.unload(soundId);
                soundId = 0;
            }
        }
    }

    public void play() {
        initSoundPool();

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            soundId = soundPool.load(context, R.raw.audio_hold_mic_tip, 1);
        }
    }

    private void initSoundPool() {
        stop();
        if (soundPool == null) {
            soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
            soundPool.setOnLoadCompleteListener(onLoadCompleteListener);
        }
    }

    private SoundPool.OnLoadCompleteListener onLoadCompleteListener = new SoundPool.OnLoadCompleteListener() {
        @Override
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            if (soundId != 0 && status == 0) {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                    int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                    streamId = soundPool.play(soundId, curVolume, curVolume, 1, 0, 1f);
                }
            }
        }
    };

}
