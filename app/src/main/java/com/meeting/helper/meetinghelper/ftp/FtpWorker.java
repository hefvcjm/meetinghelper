package com.meeting.helper.meetinghelper.ftp;

import android.util.Log;

import com.meeting.helper.meetinghelper.ftp.task.FtpTask;

import java.util.LinkedList;
import java.util.Queue;

public class FtpWorker {

    public enum FtpWorkerStatus {
        WAITING,//等待任务
        RUNNING,//正在运行
        STOPPED;//停止
    }

    private static final String TAG = "FtpWorker";

    private FtpClient client;
    private Queue<FtpTask> taskQueue = new LinkedList<>();
    private FtpTask nowTask;
    private FtpWorkerStatus status;
    private Thread thread;

    private static FtpWorker instance;

    private FtpWorker() {
        if (client == null) {
            client = FtpClient.getInstance();
        }
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
