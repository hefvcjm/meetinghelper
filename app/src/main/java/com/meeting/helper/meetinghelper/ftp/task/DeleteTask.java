package com.meeting.helper.meetinghelper.ftp.task;


import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;

public class DeleteTask extends AbstractFtpTask {

    private static final String TAG = "DeleteTask";

    private String remoteFile;

    public DeleteTask(FtpClient client, String remoteFile) {
        super(client);
        this.remoteFile = remoteFile;
    }


    @Override
    protected void doTask() {
        changeStatus(FtpTaskStatus.EXECUTING, null);
        client.delete(remoteFile);
        changeStatus(FtpTaskStatus.FINISHED, null);
    }
}
