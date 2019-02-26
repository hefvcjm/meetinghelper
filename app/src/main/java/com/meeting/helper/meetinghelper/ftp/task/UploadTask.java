package com.meeting.helper.meetinghelper.ftp.task;

import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.ftp.OnFtpProcessListener;

import java.io.File;

public class UploadTask extends AbstractFtpTask {

    private static final String TAG = "UploadTask";

    private String filePath;
    private long fileSize;
    private OnFtpProcessListener listener;

    public UploadTask(FtpClient client, String filePath, OnFtpProcessListener listener) {
        super(client);
        this.filePath = filePath;
        this.listener = listener;
        File file = new File(filePath);
        if (file.exists()) {
            fileSize = file.length();
        } else {
            fileSize = 0;
        }
    }

    public long getFileSize() {
        return fileSize;
    }

    @Override
    protected void doTask() {
        changeStatus(FtpTaskStatus.EXECUTING, null);
        if (client.upload(filePath, listener)) {
            changeStatus(FtpTaskStatus.FINISHED, null);
        } else {
            changeStatus(FtpTaskStatus.EXCEPTION, null);
        }
    }
}
