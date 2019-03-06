package com.meeting.helper.audio;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PinyinMatch {
    /*
    会议名称
     */
    /**
     * 班前会
     */
    private static String[] banqianhui = {
            "BAN/QIAN/HUI",
            "BAN/QIANG/HUI",
            "BANG/QIAN/HUI",
            "AN/QUAN/HUI",
            "BAN/CHENG/HUI",
            "BANG/CHENG/HUI",
            "QIANG/HUI",
            "QIAN/HUI",
            "QUAN/HUI",
            "CHENG/HUI"
    };
    /**
     * 班后会
     */
    private static String[] banhouhui = {
            "BAN/HOU/HUI",
            "BAN/HAO/HUI",
            "BANG/HOU/HUI",
            "BANG/HAO/HUI",
            "AN/HOU/HUI",
            "AN/HAO/HUI",
            "RAN/HOU/HUI",
            "RAN/HAO/HUI",
            "BAN/HOU",
            "BAN/HAO",
            "AN/HOU",
            "AN/HAO",
            "BANG/HOU",
            "BANG/HAO",
            "HAO/HUI",
            "HOU/HUI"
    };
    /**
     * 安全学习
     */
    private static String[] anquanxuexi = {
            "AN/QUAN/XUE/XI",
            "AN/ZHUANG/XUE/XI",
            "AN/RAN/XUE/XI"
    };

    /*
    变电站名称
     */
    /**
     * 巴南
     */
    private static String[] banan = {
            "BA/NAN",
            "DANG/RAN",
            "BA/REN",
            "BAN/NAN",
            "BA/LAN",
            "BAN/LAN",
            "BA/NENG",
            "BAN/NENG"
    };
    /**
     * 隆盛
     */
    private static String[] longsheng = {
            "LONG/SHENG",
            "LONG/SHEN",
            "NONG/SHENG",
            "NONG/SHENG",
            "LONG/SHEN",
            "NONG/SHEN",
            "LONG/SEN",
            "NONG/SEN",
            "LONG/SENG",
            "NONG/SENG",
            "LONG/XIANG",
            "NONG/XIANG"
    };
    /**
     * 陈家桥
     */
    private static String[] chenjiaqiao = {
            "CHEN/JIA/QIAO",
            "CHEN/JIA/QIANG",
            "CHEN/JIA/QIAN",
            "CAN/JIA/QIAO",
            "CAN/JIA/QIAN",
            "QUAN/JIA/QIAO",
            "QUAN/JIA/QIANG",
            "QUAN/JIA/QIAN",
            "CHENG/JIA/QIAO",
            "CHENG/JIA/QIAN",
            "CHENG/JIA/QIANG",
            "CHEN/JIA",
            "CAN/JIA",
            "CAN/JIA",
            "QUAN/JIA",
            "CHENG/JIA",
            "JIA/QIAO",
            "JIA/QIANG",
            "JIA/QIAN"
    };
    /**
     * 板桥
     */
    private static String[] banqiao = {
            "BAN/QIAO",
            "BANG/QIAO",
    };
    /**
     * 圣泉
     */
    private static String[] shengquan = {
            "SHENG/QUAN",
            "SHEN/QUAN",
            "SHANG/CHUAN"
    };
    /**
     * 玉屏
     */
    private static String[] yuping = {
            "YU/PING",
            "YU/PIN"
    };
    /**
     * 长寿
     */
    private static String[] changshou = {
            "CHANG/SHOU",
            "CHENG/SHOU"
    };
    /**
     * 石坪
     */
    private static String[] shiping = {
            "SHI/PING",
            "SHI/PIN",
            "SI/PING",
            "SI/PIN"
    };
    /**
     * 思源
     */
    private static String[] siyuan = {
            "SI/YUAN",
            "SHI/YUAN",
            "SHI/RUAN",
            "SI/RUAN"
    };
    /**
     * 如意
     */
    private static String[] ruyi = {
            "RU/YI"
    };
    /**
     * 明月山
     */
    private static String[] mingyueshan = {
            "MING/YUE/SHAN",
            "MIN/YUE/SHAN",
            "MING/YU/SHAN",
            "MING/YUE",
            "MIN/YUE",
            "YUE/SHAN"
    };
    /**
     * 会议汉字-拼音映射
     */
    public static Map<String, String[]> MEETING = new HashMap<>();
    /**
     * 地点汉字-拼音映射
     */
    public static Map<String, String[]> LOCATION = new HashMap<>();
    /**
     * 地点会议汉字-拼音映射
     */
    public static Map<String, String[]> FUll = new HashMap<>();

    static {
        MEETING.put("班前会", banqianhui);
        MEETING.put("班后会", banhouhui);
        MEETING.put("安全学习", anquanxuexi);

        LOCATION.put("巴南", banan);
        LOCATION.put("隆盛", longsheng);
        LOCATION.put("陈家桥", chenjiaqiao);
        LOCATION.put("板桥", banqiao);
        LOCATION.put("圣泉", shengquan);
        LOCATION.put("玉屏", yuping);
        LOCATION.put("长寿", changshou);
        LOCATION.put("石坪", shiping);
        LOCATION.put("思源", siyuan);
        LOCATION.put("如意", ruyi);
        LOCATION.put("明月山", mingyueshan);

        for (String location_key : LOCATION.keySet()) {
            for (String meeting_key : MEETING.keySet()) {
                List<String> temp = new ArrayList<>();
                for (String location : LOCATION.get(location_key)) {
                    for (String meeting : MEETING.get(meeting_key)) {
                        temp.add(location + "/BIAN/DIAN/ZHAN/" + meeting);
                    }
                }
                String[] a = new String[temp.size()];
                temp.toArray(a);
                FUll.put(location_key + "变电站" + meeting_key, a);
            }
        }
    }
}
