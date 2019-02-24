package com.meeting.helper.audiotool.player;

public interface Player {
    /**
     * 开始播放
     */
    void start();

//    /**
//     * 暂停播放
//     */
//    void pause();
//
//    /**
//     * 继续播放
//     */
//    void resume();

    /**
     * 停止播放
     */
    void stop();

    /**
     * 释放资源
     */
    void release();

    /**
     * 设置Player状态改变监听器
     *
     * @param listener 监听器
     */
    void setOnPlayerStatusChangedListener(OnPlayerStatusChangedListener listener);
}
