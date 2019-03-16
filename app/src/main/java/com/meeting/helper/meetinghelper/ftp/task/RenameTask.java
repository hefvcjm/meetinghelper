package com.meeting.helper.meetinghelper.ftp.task;

import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.model.FileInfo;

import java.util.ArrayList;

public class RenameTask extends AbstractFtpTask {

    private static final String TAG = "RenameTask";

    private String workingDirectory;
    private String oleFile;
    private String newFile;

    public RenameTask(String workingDirectory, String oleFile, String newFile) {
        super();
        this.workingDirectory = workingDirectory;
        this.oleFile = oleFile;
        this.newFile = newFile;
    }

    @Override
    protected void doTask() {
        changeStatus(FtpTaskStatus.EXECUTING, null);
        FtpClient client = FtpClient.getInstance();
        if (client != null) {
            if (client.rename(workingDirectory, oleFile, newFile)) {
                changeStatus(FtpTaskStatus.FINISHED, null);
            } else {
                changeStatus(FtpTaskStatus.EXCEPTION, null);
            }
        } else {
            changeStatus(FtpTaskStatus.DISCONNECTED, null);
        }
    }
}
