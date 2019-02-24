package com.meeting.helper.audiotool;

import android.media.AudioFormat;
import android.media.AudioRecord;

public class Config {

    /**
     * 录音保存路径(.pcm)
     */
    private String filePath = "/storage/emulated/0/meetinghelper/temp/temp_record.pcm";

    /**
     * 采样率，现在能够保证在所有设备上使用的采样率是44100Hz,
     * 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
     */
    private int sampleRateInHz = 44100;

    /**
     * 声道数。CHANNEL_IN_MONO and CHANNEL_IN_STEREO.
     * 其中CHANNEL_IN_MONO是可以保证在所有设备能够使用的。
     */
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;

    /**
     * 返回的音频数据的格式。 ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
     */
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * buffer大于等于AudioRecord对象用于写声音数据的buffer大小
     * 最小录音缓存buffer大小可以通过AudioRecord.getMinBufferSize方法得到
     */
    private int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);

    public String getFilePath() {
        return filePath;
    }

    public int getSampleRateInHz() {
        return sampleRateInHz;
    }

    public int getChannelConfig() {
        return channelConfig;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

    public int getBufferSize() {
        return bufferSize;
    }
}
