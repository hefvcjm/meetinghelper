package com.meeting.helper.audiotool.player;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;


import com.meeting.helper.audiotool.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class AudioPlayer implements Player {

    private static final String TAG = "AudioPlayer";
    private Config config = new Config();
    private AudioTrack audioTrack;
    private PlayerStatus status = PlayerStatus.UNINITIALIZED;
    private File file;
    private byte[] data;
    private Thread playThread;

    private OnPlayerStatusChangedListener listener;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AudioPlayer(Config config) {
        if (config != null) {
            this.config = config;
        }
        initAudioTrack();
        changeStatus(PlayerStatus.INITIALIZED);
        Log.d(TAG, "AudioPlayer init finished");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initAudioTrack() {
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        file = new File(config.getFilePath());
        Log.d(TAG, "file size:" + file.length());
        data = new byte[config.getBufferSize()];
        audioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder().setSampleRate(config.getSampleRateInHz())
                        .setEncoding(config.getAudioFormat())
                        .setChannelMask(channelConfig)
                        .build(),
                config.getBufferSize(),
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
    }

    public PlayerStatus getStatus() {
        return status;
    }

    private boolean checkBeforeExec() {
        if (audioTrack == null
                || file == null
                || data == null) {
            Log.d(TAG, "check no passed");
            return false;
        }
        if (audioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            return false;
        }
        return true;
    }

    private void changeStatus(PlayerStatus status) {
        if (this.status != status) {
            this.status = status;
            if (listener != null) {
                listener.onPlayerStatusChanged(status);
            }
            Log.d(TAG, "player status changed: " + status);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void start() {
        if (!checkBeforeExec()) {
            initAudioTrack();
        }
        changeStatus(PlayerStatus.PLAYING);
        if (playThread == null || !playThread.isAlive()) {
            audioTrack.play();
            final FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(file);
                playThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (status == PlayerStatus.PLAYING) {
                                int readCount = fileInputStream.read(data);
                                if (readCount == AudioTrack.ERROR_INVALID_OPERATION ||
                                        readCount == AudioTrack.ERROR_BAD_VALUE) {
                                    break;
                                }
                                if (readCount != 0 && readCount != -1) {
                                    audioTrack.write(data, 0, readCount);
                                } else {
                                    break;
                                }
                            }
                            fileInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            changeStatus(PlayerStatus.EXCEPTION);
                        }
                        audioTrack.stop();
                        audioTrack.release();
                        file = null;
                        data = null;
                        changeStatus(PlayerStatus.RELEASED);
                    }
                });
                playThread.start();
            } catch (IOException e) {
                e.printStackTrace();
                changeStatus(PlayerStatus.EXCEPTION);
            }
        }
    }

    @Override
    public void stop() {
        changeStatus(PlayerStatus.STOPPED);
    }

    @Override
    public void release() {
        changeStatus(PlayerStatus.RELEASED);
    }

    @Override
    public void setOnPlayerStatusChangedListener(OnPlayerStatusChangedListener listener) {
        this.listener = listener;
    }
}
