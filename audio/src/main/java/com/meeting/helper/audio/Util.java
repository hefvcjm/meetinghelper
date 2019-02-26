package com.meeting.helper.audio;

import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class Util {

    private static final String TAG = "Util";

    private static final Handler HANDLER = new Handler();

    private Util() {
    }

    public static void wait(int millis, Runnable callback) {
        HANDLER.postDelayed(callback, millis);
    }

    public static boolean isBrightColor(int color) {
        if (android.R.color.transparent == color) {
            return true;
        }
        int[] rgb = {Color.red(color), Color.green(color), Color.blue(color)};
        int brightness = (int) Math.sqrt(
                rgb[0] * rgb[0] * 0.241 +
                        rgb[1] * rgb[1] * 0.691 +
                        rgb[2] * rgb[2] * 0.068);
        return brightness >= 200;
    }

    public static int getDarkerColor(int color) {
        float factor = 0.8f;
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return Color.argb(a,
                Math.max((int) (r * factor), 0),
                Math.max((int) (g * factor), 0),
                Math.max((int) (b * factor), 0));
    }

    public static String formatSeconds(int seconds) {
        return getTwoDecimalsValue(seconds / 3600) + ":"
                + getTwoDecimalsValue(seconds / 60) + ":"
                + getTwoDecimalsValue(seconds % 60);
    }

    public static String getCurrentTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String t = format.format(new Date());
        return t;
    }

    private static String getTwoDecimalsValue(int value) {
        if (value >= 0 && value <= 9) {
            return "0" + value;
        } else {
            return value + "";
        }
    }

    public static String mergeAndSavePcmFiles(String filesDir, String saveAsFilePath, ArrayList<String> patchFileNameList) {
        if (!new File(filesDir).exists()) {
            Log.d(TAG, filesDir + " not exist");
            return null;
        }
        if (patchFileNameList.size() == 0) {
            Log.d(TAG, "patchFileNameList size is 0");
            return null;
        }
        if (patchFileNameList.size() == 1) {
            File file = new File(filesDir + "/" + patchFileNameList.get(0));
            if (file.exists()) {
                File save = new File(saveAsFilePath);
                file.renameTo(save);
                return saveAsFilePath;
            }
            Log.d(TAG, patchFileNameList.get(0) + " not exist");
            return null;
        }
        try {
            OutputStream saveAs = new FileOutputStream(saveAsFilePath);
            for (String patchFile : patchFileNameList) {
                File file = new File(filesDir + "/" + patchFile);
                Log.d(TAG, "patch file: " + patchFile + "    size: " + file.length());
                if (!file.exists()) {
                    continue;
                }
                if (file.isDirectory()) {
                    continue;
                }
                InputStream in = new FileInputStream(file);
                byte[] data = new byte[1024];
                while (in.read(data) != -1) {
                    saveAs.write(data);
                }
                in.close();
            }
            saveAs.close();
            return saveAsFilePath;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "I/O stream exception");
        return null;
    }


}