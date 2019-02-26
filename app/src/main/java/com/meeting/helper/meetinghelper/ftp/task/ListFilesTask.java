package com.meeting.helper.meetinghelper.ftp.task;

import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.model.FileInfo;

import java.util.ArrayList;

public class ListFilesTask extends AbstractFtpTask {

    private static final String TAG = "ListFilesTask";

    public ListFilesTask(FtpClient client) {
        super(client);
        changeStatus(FtpTaskStatus.WAITING, null);
    }

    @Override
    protected void doTask() {
        changeStatus(FtpTaskStatus.EXECUTING, null);
        ArrayList<FileInfo> fileInfo = client.getRemoteFileList();
        changeStatus(FtpTaskStatus.FINISHED, fileInfo);
    }
}
