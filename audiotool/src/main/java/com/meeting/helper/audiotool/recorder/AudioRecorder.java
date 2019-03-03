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
    private Thread writeFileThread;

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
        return instance;
    }

    public static AudioRecorder getInstance(OnRecorderStatusChangedListener listener) {
        instance = getInstance();
        instance.setOnRecorderStatusChangedListener(listener);
        return instance;
    }

    private synchronized boolean checkAudioRecorder() {
        AudioRecord checkAudio = new AudioRecord(MediaRecorder.AudioSource.MIC
                , config.getSampleRateInHz()
                , config.getChannelConfig()
                , config.getAudioFormat()
                , config.getBufferSize());
        if (checkAudio.getState() != AudioRecord.STATE_UNINITIALIZED) {
            checkAudio.startRecording();
            if (checkAudio.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                checkAudio.stop();
                checkAudio.release();
                return true;
            }
        }
        changeStatus(RecorderStatus.OCCUPIED);
        return false;
    }

    private void initAudioRecord() {
        if (!checkAudioRecorder()) {
            return;
        }
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

    private synchronized boolean checkBeforeExec() {
        if (audioRecord == null) {
            changeStatus(RecorderStatus.NULL_RECORDER);
            return false;
        }
        if (status == RecorderStatus.UNINITIALIZED
                || status == RecorderStatus.RELEASED
                || status == RecorderStatus.NULL_RECORDER
                || status == RecorderStatus.EXCEPTION
                || status == RecorderStatus.OCCUPIED) {
            return false;
        }
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            changeStatus(RecorderStatus.UNINITIALIZED);
            return false;
        }
        return true;
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
        synchronized (file) {
            file = new File(filePath);
        }
    }

    public String getFilePath() {
        return config.getFilePath();
    }

    public RecorderStatus getStatus() {
        return status;
    }

    @Override
    public void start() {
        if (!checkAudioRecorder()) {
            return;
        }
        if (!checkBeforeExec()) {
            initAudioRecord();
            if (!checkBeforeExec()) {
                return;
            }
        }
        audioRecord.startRecording();
        changeStatus(RecorderStatus.RECORDING);
        writeFile(file, false);
    }

    public void start(boolean isAppend) {
        if (!checkAudioRecorder()) {
            return;
        }
        if (!checkBeforeExec()) {
            initAudioRecord();
            if (!checkBeforeExec()) {
                return;
            }
        }
        audioRecord.startRecording();
        changeStatus(RecorderStatus.RECORDING);
        writeFile(file, isAppend);
    }

    @Override
    public void pause() {
        if (!checkBeforeExec()) {
            return;
        }
        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
            changeStatus(RecorderStatus.PAUSED);
        }
    }

    @Override
    public void resume() {
        start(true);
    }

    @Override
    public void stop() {
        if (!checkBeforeExec()) {
            return;
        }
        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
            changeStatus(RecorderStatus.STOPPED);
        }
    }

    @Override
    public void release() {
        if (!checkBeforeExec()) {
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
