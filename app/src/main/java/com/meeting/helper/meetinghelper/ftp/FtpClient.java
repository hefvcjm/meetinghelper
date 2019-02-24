package com.meeting.helper.meetinghelper.ftp;

import android.util.Log;

import com.meeting.helper.meetinghelper.model.FileInfo;
import com.meeting.helper.meetinghelper.utils.FileUtils;

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
import java.util.List;

public class FtpClient {

    private static final String TAG = "FtpClient";
    private static final String REMOTE_PATH = "/home/uftp";
    private static final String LOCAL_DOWNLOAD_TEMP_PATH = "/storage/emulated/0/meetinghelper/temp/download/";

    private static final int DIRECTION_UP = 0;
    private static final int DIRECTION_DOWN = 1;

    private String hostName = "39.106.3.215";
    private String username = "uftp";
    private String password = "xA123456";
    private int port = 21;

    private static FtpClient instance;

    private static FTPClient client = new FTPClient();

    private FtpClient() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                initFtpClient();
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static FtpClient getInstance() {
        if (instance == null) {
            synchronized (FtpClient.class) {
                if (instance == null) {
                    Log.d(TAG, "client is null, new one");
                    instance = new FtpClient();
                }
            }
        }
        if (!client.isConnected()) {
            Log.d(TAG, "client is not connected");
            return null;
        }
        return instance;
    }

    private void initFtpClient() {
        try {
            if (client == null) {
                client = new FTPClient();
            }
            client.connect(hostName, port);
            if (FTPReply.isPositiveCompletion(client.getReplyCode())) {
                if (client.login(username, password)) {
                    client.setControlEncoding("GBK");
                    client.setFileType(FTPClient.BINARY_FILE_TYPE);//二进制文件类型
                    client.enterLocalPassiveMode();
                    client.changeWorkingDirectory(REMOTE_PATH);
                    FTPReply.isPositiveCompletion(client.sendCommand("OPTS UTF8", "ON"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        if (client == null) {
            return false;
        }
        return client.isConnected();
    }

    public ArrayList<FileInfo> getRemoteFileList() {
        ArrayList<FileInfo> files = new ArrayList<>();
        if (!isConnected()) {
            return files;
        }
        try {
            List<FTPFile> remoteFiles = Arrays.asList(client.listFiles());
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        return files;
    }

    public boolean rename(String oldName, String newName) {
        if (!isConnected()) {
            return false;
        }
        boolean flag = false;
        try {
            flag = client.rename(new String(oldName.getBytes("GBK"), "iso-8859-1"),
                    new String(newName.getBytes("GBK"), "iso-8859-1"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    public boolean delete(String fileName) {
        if (!isConnected()) {
            return false;
        }
        boolean flag = false;
        try {
            flag = client.deleteFile(new String(fileName.getBytes("GBK"), "iso-8859-1"));
            Log.d(TAG, fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    public boolean upload(String filePath, OnFtpProcessListener listener) {
        if (!isConnected()) {
            return false;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            return false;
        }
        long totalSize = file.length();
        long precessSize = 0;
        long process = 0;
        try {
            delete(file.getName());
            InputStream in = new FileInputStream(file);
            OutputStream out = client.appendFileStream(new String(file.getName().getBytes("GBK"), "iso-8859-1"));
            if (out == null) {
                return false;
            }
            byte[] bytes = new byte[1024];
            int c;
            while ((c = in.read(bytes)) != -1) {
                out.write(bytes, 0, c);
                precessSize += c;
                if (precessSize > process) {
                    process = precessSize;
                    if (listener != null) {
                        listener.onProcess(DIRECTION_UP, filePath, totalSize, process);
                    }
                }
            }
            out.flush();
            in.close();
            out.close();
            boolean result = client.completePendingCommand();
            if (!result) {
                return false;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (listener != null) {
            listener.onProcess(DIRECTION_UP, filePath, totalSize, precessSize);
        }
        Log.d(TAG, "finish: " + filePath);
        return true;
    }

    public boolean download(String remoteFile, long fileSize, String localPath, OnFtpProcessListener listener) {
        if (!isConnected()) {
            return false;
        }
        File file = new File(localPath);
        if (file.isFile()) {
            return false;
        }
        if (!file.exists()) {
            file.mkdirs();
        }
        if (!((new File(LOCAL_DOWNLOAD_TEMP_PATH)).exists())) {
            (new File(LOCAL_DOWNLOAD_TEMP_PATH)).mkdirs();
        }
        long totalSize = fileSize;
        long precessSize = 0;
        long process = 0;
        try {
            FileOutputStream out = new FileOutputStream(localPath + remoteFile);
            InputStream in = client.retrieveFileStream(new String(remoteFile.getBytes("GBK"), "iso-8859-1"));
            if (in == null) {
                return false;
            }
            byte[] bytes = new byte[1024];
            int c;
            while ((c = in.read(bytes)) != -1) {
                out.write(bytes, 0, c);
                precessSize += c;
                if (precessSize > process) {
                    process = precessSize;
                    if (listener != null) {
                        listener.onProcess(DIRECTION_DOWN, remoteFile, totalSize, process);
                    }
                }
            }
            in.close();
            out.close();
            boolean upNewStatus = client.completePendingCommand();
            if (upNewStatus) {
                new File(LOCAL_DOWNLOAD_TEMP_PATH + remoteFile).renameTo(new File(localPath + remoteFile));
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (listener != null) {
            listener.onProcess(DIRECTION_DOWN, remoteFile, totalSize, precessSize);
        }
        Log.d(TAG, " finish: " + remoteFile);
        return true;
    }
}
