package com.meeting.helper.audiotool.player;

public enum PlayerStatus {
    PLAYING,//正在播放
    PAUSED,//已经暂停
    STOPPED,//已经停止
    UNINITIALIZED,//未初始化
    INITIALIZED,//已经初始化
    RELEASED,//已经释放资源
    EXCEPTION,//出现异常
    NULL_RECORDER;//AudioTrack实例为null
}
