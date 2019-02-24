package com.meeting.helper.audiotool.player;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.meeting.helper.audiotest.audiotool.Config;

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
    public AudioPlayer() {
        file = new File(config.getFilePath());
        data = new byte[config.getBufferSize()];
        audioTrack = initAudioTrack();
        changeStatus(PlayerStatus.INITIALIZED);
        Log.d(TAG, "AudioPlayer init finished");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private AudioTrack initAudioTrack() {
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        return new AudioTrack(
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
            return true;
        }
        return false;
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

    @Override
    public void start() {
        if (checkBeforeExec()) {
            return;
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
                            while (fileInputStream.available() > 0) {
                                if (status == PlayerStatus.PAUSED) {
                                    Thread thread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            while (status == PlayerStatus.PAUSED) {
                                            }
                                        }
                                    });
                                    thread.start();
                                    thread.join();
                                }
                                if (status == PlayerStatus.STOPPED
                                        || status == PlayerStatus.RELEASED) {
                                    break;
                                } else {
                                    changeStatus(PlayerStatus.PLAYING);
                                }
                                int readCount = fileInputStream.read(data);
                                if (readCount == AudioTrack.ERROR_INVALID_OPERATION ||
                                        readCount == AudioTrack.ERROR_BAD_VALUE) {
                                    continue;
                                }
                                if (readCount != 0 && readCount != -1) {
                                    audioTrack.write(data, 0, readCount);
                                }
                            }
                            fileInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            changeStatus(PlayerStatus.EXCEPTION);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            changeStatus(PlayerStatus.EXCEPTION);
                        }
                        release();
                    }
                });
                playThread.start();
            } catch (IOException e) {
                e.printStackTrace();
                changeStatus(PlayerStatus.EXCEPTION);
            }
        }
    }

//    @Override
//    public void pause() {
//        if (checkBeforeExec()) {
//            return;
//        }
//        changeStatus(PlayerStatus.PAUSED);
//        audioTrack.stop();
//    }

//    @Override
//    public void resume() {
//        if (checkBeforeExec()) {
//            return;
//        }
//        changeStatus(PlayerStatus.PLAYING);
//        release();
//        new AudioPlayer()
//        audioTrack.play();
//    }

    @Override
    public void stop() {
        if (checkBeforeExec()) {
            return;
        }
        changeStatus(PlayerStatus.STOPPED);
        audioTrack.pause();
        audioTrack.flush();
    }

    @Override
    public void release() {
        if (checkBeforeExec()) {
            return;
        }
        changeStatus(PlayerStatus.RELEASED);
        audioTrack.release();
        file = null;
        data = null;
        playThread = null;
    }

    @Override
    public void setOnPlayerStatusChangedListener(OnPlayerStatusChangedListener listener) {
        this.listener = listener;
    }
}
