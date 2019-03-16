package com.meeting.helper.meetinghelper.ftp.task;


import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;

public class DeleteTask extends AbstractFtpTask {

    private static final String TAG = "DeleteTask";

    private String remoteFile;
    private String workingDirectory;

    public DeleteTask(String workingDirectory, String remoteFile) {
        super();
        this.remoteFile = remoteFile;
        this.workingDirectory = workingDirectory;
    }


    @Override
    protected void doTask() {
        changeStatus(FtpTaskStatus.EXECUTING, null);
        FtpClient client = FtpClient.getInstance();
        if (client != null) {
            if (client.delete(workingDirectory, remoteFile)) {
                changeStatus(FtpTaskStatus.FINISHED, null);
            } else {
                changeStatus(FtpTaskStatus.EXCEPTION, null);
            }
        } else {
            changeStatus(FtpTaskStatus.DISCONNECTED, null);
        }

    }
}
