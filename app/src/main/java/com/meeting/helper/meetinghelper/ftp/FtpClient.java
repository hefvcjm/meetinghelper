package com.meeting.helper.meetinghelper.ftp;

import android.util.Log;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FtpClient {

    private static final String TAG = "FtpClient";
    public static final String REMOTE_BASE_PATH = "/home/uftp";
    public static final String LOCAL_DOWNLOAD_TEMP_PATH = "/storage/emulated/0/meetinghelper/temp/download/";

    private static final int DIRECTION_UP = 0;
    private static final int DIRECTION_DOWN = 1;

    private String hostName = "39.106.3.215";
    private String username = "uftp";
    private String password = "xA123456";
    private int port = 21;

    private static FtpClient instance;

    private Thread initThread;
    private Thread testTread;
    boolean testResult = false;

    private FTPClient client = new FTPClient();

    private FtpClient() {
        init();
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
        if (!instance.isConnected()) {
            instance.resetClient();
            Log.d(TAG, "resetClient");
        }
        return instance;
    }

    public void resetClient() {
        if (initThread != null) {
            initThread.interrupt();
            initThread = null;
        }
        init();
    }


    private void init() {
        if (initThread == null || !initThread.isAlive()) {
            initThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    initFtpClient();
                }
            });
            initThread.start();
            try {
                initThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void initFtpClient() {
        try {
            if (client != null) {
                client.logout();
                client.disconnect();
                client = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            client = new FTPClient();
            client.connect(hostName, port);
            if (FTPReply.isPositiveCompletion(client.getReplyCode())) {
                if (client.login(username, password)) {
                    client.setControlEncoding("GBK");
                    client.setFileType(FTPClient.BINARY_FILE_TYPE);//二进制文件类型
                    client.enterLocalPassiveMode();
                    client.changeWorkingDirectory(REMOTE_BASE_PATH);
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
        try {
            if (testTread == null || !testTread.isAlive()) {
                testTread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            testResult = client.sendNoOp();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            testTread.start();
            testTread.join();
            if (testResult) {
                testResult = false;
                return true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<FileInfo> getRemoteFileList() {
        return getRemoteFileList(REMOTE_BASE_PATH);
    }

    public ArrayList<FileInfo> getRemoteFileList(String workingDirectory) {
        ArrayList<FileInfo> files = new ArrayList<>();
        try {
            client.changeWorkingDirectory(new String(workingDirectory.getBytes("GBK"), "iso-8859-1"));
            client.changeWorkingDirectory(new String(workingDirectory.getBytes("GBK"), "iso-8859-1"));
            List<FTPFile> remoteFiles = Arrays.asList(client.listFiles());
            Collections.sort(remoteFiles, new Comparator<FTPFile>() {
                @Override
                public int compare(FTPFile o1, FTPFile o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            for (FTPFile file : remoteFiles) {
                FileInfo info = new FileInfo();
                info.setFileTime(file.getTimestamp().getTime().getTime());
                info.setFileSize(file.getSize());
                info.setFileName(file.getName());
                info.setFilePath(workingDirectory);
                info.setFileMode(file.isFile());
                files.add(info);
                Log.d(TAG, file.getName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return files;
    }

    public void clearEmptyDirectory(String path) {
        try {
            client.changeWorkingDirectory(new String(path.getBytes("GBK"), "iso-8859-1"));
            FTPFile[] ftpFiles = client.listFiles();
            for (FTPFile file : ftpFiles) {
                if (file.isDirectory()) {
                    String str = path + "/" + file.getName();
                    Log.d(TAG, "str:" + str);
                    clearEmptyDirectory(str);
                }
            }
            if (ftpFiles.length == 0 && !path.equals(REMOTE_BASE_PATH)) {
                Log.d(TAG, path.substring(0, path.lastIndexOf("/")));
                Log.d(TAG, path.substring(path.lastIndexOf("/") + 1));
                client.changeWorkingDirectory(new String(path.substring(0, path.lastIndexOf("/")).getBytes("GBK"), "iso-8859-1"));
                client.removeDirectory(new String(path.substring(path.lastIndexOf("/") + 1).getBytes("GBK"), "iso-8859-1"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean rename(String workingDirectory, String oldName, String newName) {
        boolean flag = false;
        try {
            client.changeWorkingDirectory(new String(workingDirectory.getBytes("GBK"), "iso-8859-1"));
            client.changeWorkingDirectory(new String(workingDirectory.getBytes("GBK"), "iso-8859-1"));
            flag = client.rename(new String(oldName.getBytes("GBK"), "iso-8859-1"),
                    new String(newName.getBytes("GBK"), "iso-8859-1"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    public boolean delete(String workingDirectory, String fileName) {
        boolean flag = false;
        try {
            client.changeWorkingDirectory(new String(workingDirectory.getBytes("GBK"), "iso-8859-1"));
            client.changeWorkingDirectory(new String(workingDirectory.getBytes("GBK"), "iso-8859-1"));
            flag = client.deleteFile(new String(fileName.getBytes("GBK"), "iso-8859-1"));
            Log.d(TAG, fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    public boolean upload(String workingDirectory, String filePath, OnFtpProcessListener listener) {
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
            String temp = REMOTE_BASE_PATH;
            workingDirectory = workingDirectory.substring(temp.length());
            client.changeWorkingDirectory(new String(temp.getBytes("GBK"), "iso-8859-1"));
            if (!client.changeWorkingDirectory(new String((REMOTE_BASE_PATH + "/" + workingDirectory).getBytes("GBK"), "iso-8859-1"))) {
                String[] dir = workingDirectory.split("/");
                for (String item : dir) {
                    if (!item.equals("")) {
                        temp = temp + "/" + item;
                        if (!client.changeWorkingDirectory(new String(temp.getBytes("GBK"), "iso-8859-1"))) {
                            client.makeDirectory(new String(temp.getBytes("GBK"), "iso-8859-1"));
                            client.changeWorkingDirectory(new String(temp.getBytes("GBK"), "iso-8859-1"));
                        }
                    } else {
                        continue;
                    }
                }
                client.changeWorkingDirectory(new String(temp.getBytes("GBK"), "iso-8859-1"));
            }
            client.changeWorkingDirectory(new String(temp.getBytes("GBK"), "iso-8859-1"));
            delete(temp, file.getName());
            InputStream in = new FileInputStream(file);
            OutputStream out = client.appendFileStream(new String((temp + "/" + file.getName()).getBytes("GBK"), "iso-8859-1"));
            if (out == null) {
                return false;
            }
            byte[] bytes = new byte[1024];
            int c;
            while ((c = in.read(bytes)) != -1) {
                out.write(bytes, 0, c);
                precessSize += c;
                if (precessSize >= process) {
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

    public boolean download(String workingDirectory, String remoteFile, long fileSize, String localPath, OnFtpProcessListener listener) {
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
            client.changeWorkingDirectory(new String(workingDirectory.getBytes("GBK"), "iso-8859-1"));
            client.changeWorkingDirectory(new String(workingDirectory.getBytes("GBK"), "iso-8859-1"));
            Log.d(TAG, localPath + "/" + remoteFile);
            FileOutputStream out = new FileOutputStream(localPath + "/" + remoteFile);
            InputStream in = client.retrieveFileStream(new String(remoteFile.getBytes("GBK"), "iso-8859-1"));
            if (in == null) {
                return false;
            }
            byte[] bytes = new byte[1024];
            int c;
            while ((c = in.read(bytes)) != -1) {
                out.write(bytes, 0, c);
                precessSize += c;
                if (precessSize >= process) {
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
