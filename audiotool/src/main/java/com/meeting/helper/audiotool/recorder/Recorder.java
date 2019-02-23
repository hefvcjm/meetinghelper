package com.meeting.helper.audiotool.recorder;

public interface Recorder {

    /**
     * 开始录音
     */
    void start();

    /**
     * 暂停录音
     */
    void pause();

    /**
     * 继续录音
     */
    void resume();

    /**
     * 停止录音
     */
    void stop();

    /**
     * 释放资源
     */
    void release();

    /**
     * 设置Recorder状态改变监听器
     *
     * @param listener 监听器
     */
    void setOnRecorderStatusChangedListener(OnRecorderStatusChangedListener listener);
}
