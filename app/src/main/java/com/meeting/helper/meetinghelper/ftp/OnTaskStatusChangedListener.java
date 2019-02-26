package com.meeting.helper.meetinghelper.ftp;

import com.meeting.helper.meetinghelper.ftp.task.FtpTask;

public interface OnTaskStatusChangedListener {
    void onStatusChanged(FtpTask ftpTask, FtpTaskStatus status, Object object);
}
