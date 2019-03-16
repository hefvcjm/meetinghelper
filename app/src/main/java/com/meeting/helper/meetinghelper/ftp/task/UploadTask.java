package com.meeting.helper.meetinghelper.ftp.task;

import android.util.Log;

import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.ftp.OnFtpProcessListener;

import java.io.File;

public class UploadTask extends AbstractFtpTask {

    private static final String TAG = "UploadTask";

    private String workingDirectory;
    private String filePath;
    private long fileSize;
    private OnFtpProcessListener listener;

    public UploadTask(String workingDirectory, String filePath, OnFtpProcessListener listener) {
        super();
        this.filePath = filePath;
        this.listener = listener;
        this.workingDirectory = workingDirectory;
        Log.d(TAG, workingDirectory + "|" + filePath);
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
        FtpClient client = FtpClient.getInstance();
        if (client != null) {
            if (client.upload(workingDirectory, filePath, listener)) {
                changeStatus(FtpTaskStatus.FINISHED, null);
            } else {
                changeStatus(FtpTaskStatus.EXCEPTION, null);
            }
        } else {
            changeStatus(FtpTaskStatus.DISCONNECTED, null);
        }
    }
}
