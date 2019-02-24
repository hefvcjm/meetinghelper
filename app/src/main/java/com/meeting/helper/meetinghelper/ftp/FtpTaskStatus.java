package com.meeting.helper.meetinghelper.ftp;

public enum FtpTaskStatus {
    WAITING,//等待中
    EXECUTING,//执行中
    FINISHED,//已完成
    STOPPED,//停止
    BLOCK,//阻塞
    DISCONNECTED,//断开连接
    WAIT_TIMEOUT,//等待超时
    EXEC_TIMEOUT,//执行超时
    EXCEPTION;//发生异常
}
