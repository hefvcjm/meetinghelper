package com.meeting.helper.audiotool.recorder;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioRecorder implements Recorder {

    private static final String TAG = "AudioRecorder";
    private Config config = new Config();
    private AudioRecord audioRecord;
    private RecorderStatus status;
    private File file;
    private byte[] data;

    private static AudioRecorder instance;

    private OnRecorderStatusChangedListener listener;

    private AudioRecorder() {
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC
                , config.getSampleRateInHz()
                , config.getChannelConfig()
                , config.getAudioFormat()
                , config.getBufferSize());
        file = new File(config.getFilePath());
        data = new byte[config.getBufferSize()];
        changeStatus(RecorderStatus.INITIALIZED);
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
        new Thread(new Runnable() {
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
                            if (read == 0) {
                                changeStatus(RecorderStatus.OCCUPIED);
                                break;
                            }
                            // 如果读取音频数据没有出现错误，就将数据写入到文件
                            if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                                try {
                                    os.write(data);
                                } catch (IOException e) {
                                    changeStatus(RecorderStatus.EXCEPTION);
                                    e.printStackTrace();
                                }
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
        }).start();
    }

    public RecorderStatus getStatus() {
        return status;
    }

    @Override
    public void start() {
        if (audioRecord == null) {
            changeStatus(RecorderStatus.NULL_RECORDER);
            return;
        }
        audioRecord.startRecording();
        changeStatus(RecorderStatus.RECORDING);
        writeFile(file, false);
    }

    @Override
    public void pause() {
        if (audioRecord == null) {
            changeStatus(RecorderStatus.NULL_RECORDER);
            return;
        }
        audioRecord.stop();
        changeStatus(RecorderStatus.PAUSED);
    }

    @Override
    public void resume() {
        if (audioRecord == null) {
            changeStatus(RecorderStatus.NULL_RECORDER);
            return;
        }
        audioRecord.startRecording();
        changeStatus(RecorderStatus.RECORDING);
        writeFile(file, true);
    }

    @Override
    public void stop() {
        if (audioRecord == null) {
            changeStatus(RecorderStatus.NULL_RECORDER);
            return;
        }
        audioRecord.stop();
        changeStatus(RecorderStatus.STOPPED);
    }

    @Override
    public void release() {
        if (audioRecord == null) {
            changeStatus(RecorderStatus.NULL_RECORDER);
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
