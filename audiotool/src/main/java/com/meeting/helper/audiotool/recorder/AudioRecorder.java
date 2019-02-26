package com.meeting.helper.audiotool.recorder;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.meeting.helper.audiotool.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioRecorder implements Recorder {

    private static final String TAG = "AudioRecorder";
    private Config config = new Config();
    private static AudioRecord audioRecord;
    private RecorderStatus status;
    private File file;
    private byte[] data;

    private static AudioRecorder instance;

    private OnRecorderStatusChangedListener listener;

    private AudioRecorder() {
        initAudioRecord();
        Log.d(TAG, "AudioRecorder init finished");
    }

    public static AudioRecorder getInstance() {
        if (instance == null) {
            synchronized (AudioRecorder.class) {
                if (instance == null) {
                    instance = new AudioRecorder();
                }
            }
        }
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            instance.initAudioRecord();
        }
        return instance;
    }

    private void initAudioRecord() {
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC
                , config.getSampleRateInHz()
                , config.getChannelConfig()
                , config.getAudioFormat()
                , config.getBufferSize());
        file = new File(config.getFilePath());
        data = new byte[config.getBufferSize()];
        changeStatus(RecorderStatus.INITIALIZED);
    }

    private void changeStatus(RecorderStatus status) {
        if (this.status != status) {
            this.status = status;
            if (listener != null) {
                listener.onRecorderStatusChanged(status);
            }
            Log.d(TAG, "recorder status changed: " + status);
        }
    }

    private Thread writeFileThread;

    private void writeFile(final File file, final boolean isAppend) {
        if (writeFileThread == null || !writeFileThread.isAlive()) {
            writeFileThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (file) {
                        FileOutputStream os = null;
                        try {
                            os = new FileOutputStream(file, isAppend);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        if (null != os) {
                            while (status == RecorderStatus.RECORDING) {
                                int read = audioRecord.read(data, 0, config.getBufferSize());
//                                if (read == 0) {
//                                    changeStatus(RecorderStatus.OCCUPIED);
//                                    break;
//                                }
                                // 如果读取音频数据没有出现错误，就将数据写入到文件
                                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                                    try {
                                        os.write(data);
                                    } catch (IOException e) {
                                        changeStatus(RecorderStatus.EXCEPTION);
                                        e.printStackTrace();
                                    }
                                } else {
                                    changeStatus(RecorderStatus.EXCEPTION);
                                }
                            }
                            try {
                                Log.i(TAG, "run: close file output stream !");
                                os.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            writeFileThread.start();
        }
    }

    private boolean checkBeforeExec() {
        if (audioRecord == null) {
            changeStatus(RecorderStatus.NULL_RECORDER);
            return true;
        }
        if (status == RecorderStatus.UNINITIALIZED
                || status == RecorderStatus.RELEASED
                || status == RecorderStatus.NULL_RECORDER
                || status == RecorderStatus.EXCEPTION
                || status == RecorderStatus.OCCUPIED) {
            return true;
        }
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            changeStatus(RecorderStatus.UNINITIALIZED);
            return true;
        }
        return false;
    }

    public void setConfig(Config config) {
        this.config = config;
        file = new File(config.getFilePath());
    }

    public void setStatus(RecorderStatus status) {
        changeStatus(status);
    }

    public void setFilePath(String filePath) {
        this.config.setFilePath(filePath);
        file = new File(filePath);
    }

    public String getFilePath() {
        return config.getFilePath();
    }

    public RecorderStatus getStatus() {
        return status;
    }

    @Override
    public void start() {
        if (!checkBeforeExec()) {
            initAudioRecord();
        }
        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
            changeStatus(RecorderStatus.OCCUPIED);
            return;
        }
        audioRecord.startRecording();
        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
            changeStatus(RecorderStatus.OCCUPIED);
            return;
        }
        changeStatus(RecorderStatus.RECORDING);
        writeFile(file, false);
    }

    @Override
    public void pause() {
        if (checkBeforeExec()) {
            return;
        }
        audioRecord.stop();
        changeStatus(RecorderStatus.PAUSED);
    }

    @Override
    public void resume() {
        if (!checkBeforeExec()) {
            initAudioRecord();
        }
        start();
    }

    @Override
    public void stop() {
        if (checkBeforeExec()) {
            return;
        }
        audioRecord.stop();
        changeStatus(RecorderStatus.STOPPED);
    }

    @Override
    public void release() {
        if (checkBeforeExec()) {
            return;
        }
        if (status == RecorderStatus.RECORDING) {
            audioRecord.stop();
        }
        audioRecord.release();
        changeStatus(RecorderStatus.RELEASED);
    }

    @Override
    public void setOnRecorderStatusChangedListener(OnRecorderStatusChangedListener listener) {
        this.listener = listener;
    }
}
