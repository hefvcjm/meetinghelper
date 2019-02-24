package com.meeting.helper.meetinghelper.utils;

import android.util.Log;

import com.meeting.helper.meetinghelper.interfaces.OnFtpProcessListener;
import com.meeting.helper.meetinghelper.model.FileInfo;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MyFtpClient {

    private static final String TAG = "MyFtpClient";
    private static final String REMOTE_PATH = "/home/uftp";
    private static final String LOCAL_PATH = "/storage/emulated/0/meetinghelper/records";
    private static final String LOCAL_DOWNLOAD_TEMP_PATH = "/storage/emulated/0/meetinghelper/temp/download";

    private static final int DIRECTION_UP = 0;
    private static final int DIRECTION_DOWN = 1;

    private String hostName = "39.106.3.215";
    private String username = "uftp";
    private String password = "xA123456";
    private int port = 21;

    private static MyFtpClient client;
    private static FTPClient ftpClient = new FTPClient();

    private static Queue<String> uploadQueue = new LinkedList<>();
    private static Queue<String> downloadQueue = new LinkedList<>();
    private int totalDownload = 0;
    private int successDownload = 0;
    private static int failureDownload = 0;
    private int totalUpload = 0;
    private int successUpload = 0;
    private static int failureUpload = 0;

    private static double totalSizeDownload = Double.MIN_VALUE;
    private long precessSizeDownload = 0;

    private static double totalSizeUpload = Double.MIN_VALUE;
    private long precessSizeUpload = 0;

    private static boolean isUploading = false;
    private static boolean isDownloading = false;

    private static FTPFile[] remoteFileList;

    private OnFtpProcessListener listener;

    public static MyFtpClient getFtpClient() {
        if (client == null) {
            synchronized (MyFtpClient.class) {
                if (client == null) {
                    Log.d(TAG, "client is null, new one");
                    client = new MyFtpClient();
                }
            }
        }
        if (!ftpClient.isConnected()) {
            return null;
        }
        return client;
    }

    private MyFtpClient() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                initFtpClient();
            }
        }).start();
        clearUpload();
        clearDownload();
    }

    private void initFtpClient() {
        try {
            if (ftpClient == null) {
                ftpClient = new FTPClient();
            }
            ftpClient.connect(hostName, port);
            if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                if (ftpClient.login(username, password)) {
                    ftpClient.setControlEncoding("GBK");
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);//二进制文件类型
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.changeWorkingDirectory(REMOTE_PATH);
                    FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"));
                    init();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        try {
            remoteFileList = ftpClient.listFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File file = new File(LOCAL_DOWNLOAD_TEMP_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(LOCAL_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        clearUpload();
        clearDownload();
    }

    public boolean addUploadFile(String path) {
        if (uploadQueue.contains(path)) {
            return false;
        }
        boolean flag = uploadQueue.offer(path);
        if (flag) {
            totalUpload++;
            File file = new File(path);
            if (file != null) {
                totalSizeUpload += file.length();
                Log.d(TAG, "totalSizeUpload:" + totalSizeUpload);
            }
        }
        return flag;
    }

    public void addDownloadFile(String path) {
        ArrayList<String> list = new ArrayList<>();
        list.add(path);
        addDownloadFiles(list);
    }

    public void addUploadFiles(ArrayList<String> paths) {
        for (String path : paths) {
            addUploadFile(path);
        }
    }

    public void addDownloadFiles(final ArrayList<String> paths) {
        try {
            if (!isUploading && !isDownloading) {
                remoteFileList = ftpClient.listFiles(new String(REMOTE_PATH.getBytes("GBK"), "iso-8859-1"));
            }
            for (FTPFile file : remoteFileList) {
                String path = file.getName();
                if (paths.contains(file.getName())) {
                    if (downloadQueue.contains(path)) {
                        continue;
                    } else {
                        if (downloadQueue.offer(path)) {
                            totalSizeDownload += file.getSize();
                            Log.d(TAG, "totalSizeDownload:" + totalSizeDownload);
                        } else {
                            failureDownload++;
                        }
                    }
                }
            }
            totalDownload += paths.size();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearUpload() {
        Log.d(TAG, "clear upload status");
        uploadQueue.clear();
        totalUpload = 0;
        successUpload = 0;
        failureUpload = 0;
        totalSizeUpload = Double.MIN_VALUE;
        precessSizeUpload = 0;
        isUploading = false;
    }

    public void clearDownload() {
        Log.d(TAG, "clear download status");
        downloadQueue.clear();
        totalDownload = 0;
        successDownload = 0;
        failureDownload = 0;
        totalSizeDownload = Double.MIN_VALUE;
        precessSizeDownload = 0;
        isDownloading = false;
    }

    public void setOnProcessListener(OnFtpProcessListener listener) {
        this.listener = listener;
    }

    public void doTask() {
        if (!isUploading) {
            Log.d(TAG, "new uploading");
            upload();
        } else {
            Log.d(TAG, "append uploading");
        }
        if (!isDownloading) {
            Log.d(TAG, "new downloading");
            download();
        } else {
            Log.d(TAG, "appending downloading");
        }
    }


    public static ArrayList<FileInfo> getRemoteFileList() {
        ArrayList<FileInfo> files = new ArrayList<>();
        if (!isUploading && !isDownloading) {
            getFtpClient();
            try {
                remoteFileList = ftpClient.listFiles();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (remoteFileList == null) {
            return files;
        }
        List<FTPFile> remoteFiles = Arrays.asList(remoteFileList);
        Collections.sort(remoteFiles, new Comparator<FTPFile>() {
            @Override
            public int compare(FTPFile o1, FTPFile o2) {
                return (int) (o2.getTimestamp().getTimeInMillis() - o1.getTimestamp().getTimeInMillis());
            }
        });
        for (FTPFile file : remoteFiles) {
            FileInfo info = new FileInfo();
            info.setFileTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(file.getTimestamp().getTime()));
            info.setFileSize(FileUtils.getFileSize(file.getSize()));
            info.setFileName(file.getName());
            info.setFilePath(file.getName());
            files.add(info);
            Log.d(TAG, file.getName());
        }
        return files;
    }

    public static boolean rename(String oldName, String newName) {
        boolean flag = false;
        try {
            flag = ftpClient.rename(new String(oldName.getBytes("GBK"), "iso-8859-1"),
                    new String(newName.getBytes("GBK"), "iso-8859-1"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    public static boolean delete(String fileName) {
        boolean flag = false;
        try {
            flag = ftpClient.deleteFile(new String(fileName.getBytes("GBK"), "iso-8859-1"));
            Log.d(TAG, fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    public static int deleteFiles(List<String> files) {
        int count = 0;
        for (String item : files) {
            if (delete(item)) {
                count++;
            }
        }
        return count;
    }

    private void upload() {
        isUploading = true;
        double process = 0;
        double nowProcess = 0;
        String item;
        while ((item = uploadQueue.poll()) != null) {
            Log.d(TAG, "remain: " + uploadQueue.size() + " ,now: " + item);
            try {
                File file = new File(item);
                delete(file.getName());
                InputStream in = new FileInputStream(file);
                OutputStream out = ftpClient.appendFileStream(new String(file.getName().getBytes("GBK"), "iso-8859-1"));
                byte[] bytes = new byte[1024];
                int c;
                while ((c = in.read(bytes)) != -1) {
                    out.write(bytes, 0, c);
                    precessSizeUpload += c;
                    nowProcess = precessSizeUpload * 1.0 / totalSizeUpload;
                    if (nowProcess > process || process - nowProcess > 0.1) {
                        process = nowProcess;
                        if (listener != null) {
                            listener.onProcess(DIRECTION_UP, item, FileUtils.getFileSize((long) totalSizeUpload), process, totalUpload, successUpload, failureUpload);
                        }
                    }
                }
                out.flush();
                in.close();
                out.close();
                boolean result = ftpClient.completePendingCommand();
                if (result) {
                    successUpload += 1;
                } else {
                    failureUpload += 1;
                }
            } catch (FileNotFoundException e) {
                failureUpload += 1;
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                failureUpload += 1;
                e.printStackTrace();
            } catch (IOException e) {
                failureUpload += 1;
                e.printStackTrace();
            }
            listener.onProcess(DIRECTION_UP, item, FileUtils.getFileSize((long) totalSizeUpload), nowProcess, totalUpload, successUpload, failureUpload);
            Log.d(TAG, "finish: " + item);
        }
        clearUpload();
    }

    private void download() {
        isDownloading = true;
        double process = 0;
        double nowProcess = 0;
        String item;
        while ((item = downloadQueue.poll()) != null) {
            Log.d(TAG, "remain: " + downloadQueue.size() + " ,now: " + item);
            try {
                FileOutputStream out = new FileOutputStream(LOCAL_DOWNLOAD_TEMP_PATH + "/" + item);
                InputStream in = ftpClient.retrieveFileStream(new String(item.getBytes("GBK"), "iso-8859-1"));
                byte[] bytes = new byte[1024];
                int c;
                while ((c = in.read(bytes)) != -1) {
                    out.write(bytes, 0, c);
                    precessSizeDownload += c;
                    nowProcess = precessSizeDownload * 1.0 / totalSizeDownload;
                    if (nowProcess > process || process - nowProcess > 0.1) {
                        process = nowProcess;
                        if (listener != null) {
                            listener.onProcess(DIRECTION_DOWN, item, FileUtils.getFileSize((long) totalSizeDownload), process, totalDownload, successDownload, failureDownload);
                        }
                    }
                }
                in.close();
                out.close();
                boolean upNewStatus = ftpClient.completePendingCommand();
                if (upNewStatus) {
                    successDownload += 1;
                    new File(LOCAL_DOWNLOAD_TEMP_PATH + "/" + item).renameTo(new File(LOCAL_PATH + "/" + item));
                } else {
                    failureDownload += 1;
                }
            } catch (IOException e) {
                e.printStackTrace();
                failureDownload += 1;
            }
            listener.onProcess(DIRECTION_DOWN, item, FileUtils.getFileSize((long) totalSizeDownload), nowProcess, totalDownload, successDownload, failureDownload);
            Log.d(TAG, " finish: " + item);
        }
        clearDownload();
    }

    public int getTotalDownload() {
        return totalDownload;
    }

    public int getSuccessDownload() {
        return successDownload;
    }

    public static int getFailureDownload() {
        return failureDownload;
    }

    public int getTotalUpload() {
        return totalUpload;
    }

    public int getSuccessUpload() {
        return successUpload;
    }

    public static int getFailureUpload() {
        return failureUpload;
    }

    public static boolean isUploading() {
        return isUploading;
    }

    public static void setUploading(boolean isUploading) {
        MyFtpClient.isUploading = isUploading;
    }

    public static boolean isDownloading() {
        return isDownloading;
    }

    public static void setDownloading(boolean isDownloading) {
        MyFtpClient.isDownloading = isDownloading;
    }

    public void disconnect() throws IOException {
        if (ftpClient.isConnected()) {
            ftpClient.disconnect();
        }
    }

}
