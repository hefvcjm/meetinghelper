package com.meeting.helper.meetinghelper.ftp.task;

import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;

public class RenameTask extends AbstractFtpTask {

    private static final String TAG = "RenameTask";

    private String oleFile;
    private String newFile;

    public RenameTask(FtpClient client, String oleFile, String newFile) {
        super(client);
        this.oleFile = oleFile;
        this.newFile = newFile;
    }

    @Override
    protected void doTask() {
        changeStatus(FtpTaskStatus.EXECUTING, null);
        client.rename(oleFile, newFile);
        changeStatus(FtpTaskStatus.FINISHED, null);
    }
}
