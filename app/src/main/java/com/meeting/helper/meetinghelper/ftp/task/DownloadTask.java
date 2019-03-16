package com.meeting.helper.meetinghelper.ftp.task;

import android.util.Log;

import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.ftp.OnFtpProcessListener;

public class DownloadTask extends AbstractFtpTask {

    private static final String TAG = "DownloadTask";

    private String workingDirectory;
    private String remoteFile;
    private String localFile;
    private long fileSize;
    private OnFtpProcessListener listener;

    public DownloadTask(String workingDirectory, String remoteFile, long fileSize, String localFile, OnFtpProcessListener listener) {
        this.workingDirectory = workingDirectory;
        this.remoteFile = remoteFile;
        this.fileSize = fileSize;
        this.localFile = localFile;
        this.listener = listener;
        Log.d(TAG, workingDirectory + "|" + remoteFile + "|" + localFile);
        Log.d(TAG, "init DownloadTask");
    }

    public long getFileSize() {
        return fileSize;
    }

    @Override
    protected void doTask() {
        Log.d(TAG, "DownloadTask doTask");
        changeStatus(FtpTaskStatus.EXECUTING, null);
        FtpClient client = FtpClient.getInstance();
        if (client != null) {
            if (client.download(workingDirectory, remoteFile, fileSize, localFile, listener)) {
                changeStatus(FtpTaskStatus.FINISHED, null);
            } else {
                changeStatus(FtpTaskStatus.EXCEPTION, null);
            }
        } else {
            changeStatus(FtpTaskStatus.DISCONNECTED, null);
        }
    }
}
