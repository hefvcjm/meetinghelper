package com.meeting.helper.meetinghelper.ftp.task;


import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;

public class DeleteTask extends AbstractFtpTask {

    private static final String TAG = "DeleteTask";

    private String remoteFile;

    public DeleteTask(String remoteFile) {
        super();
        this.remoteFile = remoteFile;
    }


    @Override
    protected void doTask() {
        changeStatus(FtpTaskStatus.EXECUTING, null);
        FtpClient client = FtpClient.getInstance();
        if (client != null) {
            if (client.delete(remoteFile)) {
                changeStatus(FtpTaskStatus.FINISHED, null);
            } else {
                changeStatus(FtpTaskStatus.EXCEPTION, null);
            }
        } else {
            changeStatus(FtpTaskStatus.DISCONNECTED, null);
        }

    }
}
