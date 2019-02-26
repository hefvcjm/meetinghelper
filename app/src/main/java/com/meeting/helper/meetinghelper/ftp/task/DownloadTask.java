package com.meeting.helper.meetinghelper.ftp.task;

import android.util.Log;

import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.ftp.OnFtpProcessListener;

public class DownloadTask extends AbstractFtpTask {

    private static final String TAG = "DownloadTask";

    private String remoteFile;
    private String localFile;
    private long fileSize;
    private OnFtpProcessListener listener;

    public DownloadTask(FtpClient client, String remoteFile, long fileSize, String localFile, OnFtpProcessListener listener) {
        super(client);
        this.remoteFile = remoteFile;
        this.fileSize = fileSize;
        this.localFile = localFile;
        this.listener = listener;
        Log.d(TAG, "init DownloadTask");
    }

    public long getFileSize() {
        return fileSize;
    }

    @Override
    protected void doTask() {
        Log.d(TAG, "DownloadTask doTask");
        changeStatus(FtpTaskStatus.EXECUTING, null);
        boolean result = client.download(remoteFile, fileSize, localFile, listener);
        if (result) {
            changeStatus(FtpTaskStatus.FINISHED, null);
        } else {
            changeStatus(FtpTaskStatus.EXCEPTION, null);
        }
    }
}
