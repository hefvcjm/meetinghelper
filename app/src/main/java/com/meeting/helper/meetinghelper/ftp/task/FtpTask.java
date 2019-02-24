package com.meeting.helper.meetinghelper.ftp.task;


import com.meeting.helper.meetinghelper.ftp.OnTaskStatusChangedListener;

public interface FtpTask {

    /**
     * 执行任务
     */
    void execute();

    /**
     * 终止任务
     */
    void stop();

    /**
     * 设置状态监听器
     *
     * @param listener 监听器
     */
    void setOnTaskStatusChangedListener(OnTaskStatusChangedListener listener);
}
