package com.meida.utils;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class RandomLength {

    /**
     * 根据传递的参数len的长度，随机生成字符串
     * @param Len
     * @return
     */
    public static String getRandomString(int Len) {

        String[] baseString = { "0", "1", "2", "3", "4", "5", "6", "7", "8",
                "9", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k",
                "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w",
                "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I",
                "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U",
                "V", "W", "X", "Y", "Z" };
        Random random = new Random();
        int length = baseString.length;
        String randomString = "";
        for (int i = 0; i < length; i++) {
            randomString += baseString[random.nextInt(length)];
        }
        random = new Random(System.currentTimeMillis());
        String resultStr = "";
        for (int i = 0; i < Len; i++) {
            resultStr += randomString.charAt(random.nextInt(randomString.length() - 1));
        }
        return resultStr;
    }

    /**
     * 产生min到max随机数
     * @param min 最小值
     * @param max 最大值
     * @return 随机数
     */
    public static int createRandomNumber(int min,int max){
        return (int) ( min + Math.random() * (max));
    }

    /**
     * 获取时间戳,格式:yyyyMMddHHmmss
     * @return 字符串
     */
    @SuppressLint("SimpleDateFormat")
    public static String getCurrentTime() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

}
