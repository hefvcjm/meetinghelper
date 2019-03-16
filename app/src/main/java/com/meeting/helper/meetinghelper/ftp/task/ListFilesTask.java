package com.meeting.helper.meetinghelper.ftp.task;

import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.model.FileInfo;

import java.util.ArrayList;

public class ListFilesTask extends AbstractFtpTask {

    private static final String TAG = "ListFilesTask";
    private String workingDirectory;

    public ListFilesTask(String workingDirectory) {
        super();
        this.workingDirectory = workingDirectory;
    }

    @Override
    protected void doTask() {
        changeStatus(FtpTaskStatus.EXECUTING, null);
        FtpClient client = FtpClient.getInstance();
        if (client != null) {
            ArrayList<FileInfo> fileInfo = client.getRemoteFileList(this.workingDirectory);
            if (fileInfo != null) {
                changeStatus(FtpTaskStatus.FINISHED, fileInfo);
            } else {
                changeStatus(FtpTaskStatus.EXCEPTION, null);
            }
        } else {
            changeStatus(FtpTaskStatus.DISCONNECTED, null);
        }
    }
}
