package com.meeting.helper.meetinghelper.ftp.task;

import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.ftp.OnFtpProcessListener;

public class UploadTask extends AbstractFtpTask {

    private static final String TAG = "UploadTask";

    private String filePath;
    private OnFtpProcessListener listener;

    public UploadTask(FtpClient client, String filePath, OnFtpProcessListener listener) {
        super(client);
        this.filePath = filePath;
        this.listener = listener;
    }

    @Override
    protected void doTask() {
        changeStatus(FtpTaskStatus.EXECUTING, null);
        client.upload(filePath, listener);
        changeStatus(FtpTaskStatus.FINISHED, null);
    }
}
