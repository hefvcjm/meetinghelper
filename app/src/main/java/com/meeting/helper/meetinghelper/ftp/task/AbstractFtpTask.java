package com.meeting.helper.meetinghelper.ftp.task;

import android.os.Bundle;
import android.util.Log;

import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.ftp.OnTaskStatusChangedListener;

import java.util.Timer;

public abstract class AbstractFtpTask implements FtpTask {

    private static final String TAG = "AbstractFtpTask";

    protected FtpClient client;
    protected OnTaskStatusChangedListener listener;
    protected FtpTaskStatus status;
    protected Thread thread;

    protected String name;
    protected long id;
    protected int waitTimeout = -1;
    protected int execTimeout = -1;

    protected Timer waitingTimer;

    private Timer executingTimer;

    public AbstractFtpTask(FtpClient client) {
        this.client = client;
        changeStatus(FtpTaskStatus.WAITING, null);

    }

    protected void changeStatus(FtpTaskStatus status, Bundle bundle) {
        if (this.status != status) {
            this.status = status;
            if (listener != null) {
                listener.onStatusChanged(status, bundle);
            }
            Log.d(TAG, "Task status changed: " + status);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getWaitTimeout() {
        return waitTimeout;
    }

    public void setWaitTimeout(int waitTimeout) {
        this.waitTimeout = waitTimeout;
    }

    public int getExecTimeout() {
        return execTimeout;
    }

    public void setExecTimeout(int execTimeout) {
        this.execTimeout = execTimeout;
    }

    public FtpTaskStatus getStatus() {
        return status;
    }

    protected abstract void doTask();

    @Override
    public void setOnTaskStatusChangedListener(OnTaskStatusChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public void execute() {
        if (client != null) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    doTask();
                }
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                changeStatus(FtpTaskStatus.STOPPED, null);
            }
        } else {
            changeStatus(FtpTaskStatus.DISCONNECTED, null);
        }
    }

    @Override
    public void stop() {
        if (status == FtpTaskStatus.EXECUTING) {
            if (thread != null) {
                thread.interrupt();
            }
        }
    }
}
