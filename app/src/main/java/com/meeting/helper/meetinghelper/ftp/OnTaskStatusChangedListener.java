package com.meeting.helper.meetinghelper.ftp;

import android.os.Bundle;

public interface OnTaskStatusChangedListener {
    void onStatusChanged(FtpTaskStatus status, Bundle bundle);
}
