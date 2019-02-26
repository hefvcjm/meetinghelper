package com.meeting.helper.audiotool.recorder;

public enum RecorderStatus {
    UNINITIALIZED,//未初始化
    OCCUPIED,//被占用
    INITIALIZED,//已经初始化
    RECORDING,//正在录音
    PAUSED,//已经暂停
    STOPPED,//已经停止录音
    RELEASED,//已经释放资源
    EXCEPTION,//出现异常
    NEED_TO_TRY,//需要重试
    NULL_RECORDER;//AudioRecord实例为null
}
