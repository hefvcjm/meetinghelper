package com.meeting.helper.meetinghelper.ftp;

import android.util.Log;

import com.meeting.helper.meetinghelper.ftp.task.AbstractFtpTask;
import com.meeting.helper.meetinghelper.ftp.task.DeleteTask;
import com.meeting.helper.meetinghelper.ftp.task.DownloadTask;
import com.meeting.helper.meetinghelper.ftp.task.FtpTask;
import com.meeting.helper.meetinghelper.ftp.task.ListFilesTask;
import com.meeting.helper.meetinghelper.ftp.task.RenameTask;
import com.meeting.helper.meetinghelper.ftp.task.UploadTask;

import java.util.LinkedList;
import java.util.Queue;

public class FtpWorker {

    public enum FtpWorkerStatus {
        WAITING,//等待任务
        RUNNING,//正在运行
        STOPPED;//停止
    }

    private static final String TAG = "FtpWorker";

    private Queue<FtpTask> taskQueue = new LinkedList<>();
    private FtpTask nowTask;
    private FtpWorkerStatus status;
    private Thread thread;

    private static FtpWorker instance;

    private FtpWorker() {
        init();
    }

    private void init() {
        if (taskQueue == null) {
            taskQueue = new LinkedList<>();
        }
        Log.d(TAG, "ftp worker init finished");
    }

    public static FtpWorker getInstance() {
        if (instance == null) {
            synchronized (FtpWorker.class) {
                if (instance == null) {
                    instance = new FtpWorker();
                }
            }
        }
        instance.init();
        return instance;
    }

    public boolean addTask(FtpTask task) {
        if (taskQueue == null) {
            taskQueue = new LinkedList<>();
        }
        boolean flag = taskQueue.offer(task);
        if (flag) {
            Log.d(TAG, "add task:" + task.getClass().getName());
        }
        workerStart();
        return flag;
    }

    public boolean addDeleteTask(String remoteFile) {
        return addTask(new DeleteTask(remoteFile));
    }

    public boolean addDownloadTask(String remoteFile, long fileSize, String localFile, OnFtpProcessListener listener) {
        return addTask(new DownloadTask(remoteFile, fileSize, localFile, listener));
    }

    public boolean addListFilesTask() {
        return addTask(new ListFilesTask());
    }

    public boolean addRenameTask(String oldFile, String newFile) {
        return addTask(new RenameTask(oldFile, newFile));
    }

    public boolean addUploadTask(String filePath, OnFtpProcessListener listener) {
        return addTask(new UploadTask(filePath, listener));
    }

    public void clearAllTask() {
        if (taskQueue != null) {
            taskQueue.clear();
        }
    }

    public void stopNowTask() {
        if (nowTask != null) {
            nowTask.stop();
        }
    }

    public FtpTask getNowTask() {
        return nowTask;
    }

    public int countTask() {
        return taskQueue.size();
    }

    private void workerStart() {
        if (thread == null || !thread.isAlive()) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        synchronized (taskQueue) {
                            nowTask = taskQueue.poll();
                        }
                        while (nowTask != null) {
                            Log.d(TAG, "execute task: " + nowTask.getClass().getName());
                            status = FtpWorkerStatus.RUNNING;
                            nowTask.execute();
                            if (((AbstractFtpTask) nowTask).getStatus() == FtpTaskStatus.EXCEPTION
                                    || ((AbstractFtpTask) nowTask).getStatus() == FtpTaskStatus.DISCONNECTED) {
                                FtpClient.getInstance().resetClient();
                                nowTask.execute();
                            }
                            synchronized (taskQueue) {
                                nowTask = taskQueue.poll();
                            }
                        }
                        status = FtpWorkerStatus.WAITING;
                    }
                }
            });
            thread.start();
            Log.d(TAG, "ftp worker thread started");
        }
    }


}
